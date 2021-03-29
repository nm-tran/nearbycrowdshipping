package de.hshannover.nearbycrowdshipping;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import de.hshannover.nearbycrowdshipping.adapter.EndpointListAdapter;
import de.hshannover.nearbycrowdshipping.model.Bid;
import de.hshannover.nearbycrowdshipping.model.ChildEndpointSet;
import de.hshannover.nearbycrowdshipping.model.Endpoint;
import de.hshannover.nearbycrowdshipping.model.GenerationTable;
import de.hshannover.nearbycrowdshipping.model.Header;
import de.hshannover.nearbycrowdshipping.model.Message;
import de.hshannover.nearbycrowdshipping.model.Parcel;

import static de.hshannover.nearbycrowdshipping.Constant.SERVICE_ID;
import static de.hshannover.nearbycrowdshipping.Constant.STRATEGY;
import static java.nio.charset.StandardCharsets.UTF_8;

public class CourierActivity extends AppCompatActivity {

    private TextView mNameTextView;
    private TextView mDestinationTextView;
    private TextView mDeadlineTextView;
    private TextView mMeetingLocationTextView;
    private TextView mMeetingTimeTextView;
    private TextView mPriceTextView;
    private EditText mBidEditText;
    private Button mBidButton;
    private LinearLayout mParentLinearLayout;
    private TextView mParentNameTextView;
    private TextView mParentEndpointIdTextView;

    private Endpoint cachedEndpoint;

    private Endpoint parentEndpoint;

    private GenerationTable generationTable;

    private ArrayList<Endpoint> mChildEndpointList;
    private RecyclerView mRecyclerView;
    private EndpointListAdapter mAdapter;

    private Parcel parcel;

    private Message<Parcel> parcelMessage;

    /**
     * Handler to Nearby Connections.
     */
    private ConnectionsClient mConnectionsClient;

    /**
     * The state of the device
     */
    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courier);

        mChildEndpointList = new ArrayList<>();

        setGUI();

        generationTable = new GenerationTable();

        mConnectionsClient = Nearby.getConnectionsClient(this);

        startDiscovery();
    }

    private void setGUI() {
        mNameTextView = findViewById(R.id.courier_device_name_text);
        mNameTextView.setText(mNameTextView.getText() + " " + Utils.getDeviceName());

        mDestinationTextView = findViewById(R.id.courier_destination_text);
        mDeadlineTextView = findViewById(R.id.courier_deadline_text);
        mMeetingLocationTextView = findViewById(R.id.courier_meetingLocation_text);
        mMeetingTimeTextView = findViewById(R.id.courier_meetingTime_text);
        mPriceTextView = findViewById(R.id.courier_price_text);

        mBidEditText = findViewById(R.id.courier_bid_text);
        mBidButton = findViewById(R.id.courier_bid_button);

        mParentLinearLayout = findViewById(R.id.parent_linearlayout);
        mParentNameTextView = findViewById(R.id.parent_name);
        mParentEndpointIdTextView = findViewById(R.id.parent_endpointId);

        mRecyclerView = findViewById(R.id.children_endpoint_list_recyclerview);
        mAdapter = new EndpointListAdapter(this, mChildEndpointList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsDiscovering) {
            mConnectionsClient.stopDiscovery();
            Log.i(Constant.TAG, "stopDiscovery\n\n");
        }
        if (mIsAdvertising) {
            mConnectionsClient.stopAdvertising();
            Log.i(Constant.TAG, "stopAdvertising\n\n");
        }
        mConnectionsClient.stopAllEndpoints();
    }

    /**
     * Callbacks for payloads (bytes of data) sent from another device to us.
     */
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Toast.makeText(CourierActivity.this, new String(payload.asBytes(), UTF_8), Toast.LENGTH_SHORT).show();

                    String messageJson = new String(payload.asBytes());
                    String messageHeaderType = Utils.getMessageHeaderType(payload);

                    switch (messageHeaderType) {
                        case Constant.MESSAGE_HEADER_PARCEL:
                            processParcelMessage(endpointId, messageJson);
                            break;
                        case Constant.MESSAGE_HEADER_BID:
                            processBidMessage(messageJson, parentEndpoint);
                            break;
                        case Constant.MESSAGE_HEADER_CHILDENDPOINT_SET:
                            processChildEndpointSetMessage(endpointId, messageJson);
                            break;
                        case Constant.MESSAGE_HEADER_WINNER:
                            processWinnerMessage(endpointId, messageJson);
                            break;
                        default:
                            Log.i(Constant.TAG, "mPayloadCallback onPayloadReceived: unknown message type");
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                }
            };

    private void processWinnerMessage(String endpointId, String messageJson) {
        Log.i(Constant.TAG, String.format("mPayloadCallback onPayloadReceived: " +
                "receive winnerMessage from %s", endpointId));

        sendWinnerMessageToAllChildEndpoint(messageJson);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Message message = gson.fromJson(messageJson, Message.class);
        Header header = message.getHeader();

        showResultDialog(header);
    }

    private void showResultDialog(Header header) {

        if (header.getTo().equals(Utils.getDeviceName())) {
            new AlertDialog.Builder(CourierActivity.this)
                    .setTitle("Congratulation!")
                    .setMessage("You are the winner of the auction. Please go to " +
                            "the meeting location on time to pick the parcel.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mConnectionsClient.stopAllEndpoints();
                            startActivity(new Intent(CourierActivity.this, MainActivity.class));
                        }
                    })
                    .show();
        } else {
            new AlertDialog.Builder(CourierActivity.this)
                    .setTitle("Maybe next time!")
                    .setMessage("You has failed to win the auction.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mConnectionsClient.stopAllEndpoints();
                            startActivity(new Intent(CourierActivity.this, MainActivity.class));
                        }
                    })
                    .show();
        }
    }

    private void sendWinnerMessageToAllChildEndpoint(String messageJson) {
        if (generationTable.getTable().size() != 0) {
            for (Endpoint e : generationTable.getTable().keySet()) {
                mConnectionsClient.sendPayload(e.getId(), Payload.fromBytes(messageJson.getBytes(UTF_8)));
            }
        }
    }

    private void processChildEndpointSetMessage(String endpointId, String messageJson) {
        Log.i(Constant.TAG, String.format("mPayloadCallback onPayloadReceived: " +
                "receive ChildEndpointSet from %s", endpointId));

        updateForChildEndpointSetMessage(endpointId, messageJson);
        Log.i(Constant.TAG, String.format("mPayloadCallback onPayloadReceived: " +
                "updated table: %s", generationTable.getTable()));

        sendChildEndpointSetToParentEndpoint();
        Log.i(Constant.TAG, String.format("mPayloadCallback onPayloadReceived: " +
                "send new childEndpointSet to parent after updating : %s\n\n", Utils.getCurrentChildEndpointSet(generationTable).getSet()));
    }

    private void updateForChildEndpointSetMessage(String endpointId, String messageJson) {
        // get childEndpointSet
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<Message<ChildEndpointSet>>() {
        }.getType();
        Message<ChildEndpointSet> message = gson.fromJson(messageJson, type);
        ChildEndpointSet childEndpointSet = message.getPayload();

        Endpoint key = Utils.getEndpointFromTableById(generationTable, endpointId);

        updateTableForChildEndpointSetMessage(childEndpointSet, key);

        updateListForChildEndpointSetMessage(childEndpointSet, key);
    }

    private void updateListForChildEndpointSetMessage(ChildEndpointSet childEndpointSet, Endpoint key) {
        // add new endpoint
        for (Endpoint e : childEndpointSet.getSet()) {
            if (!mChildEndpointList.contains(e)) {
                Log.i(Constant.TAG, "add new Endpoint " + e);
                mChildEndpointList.add(e);
            }
        }

        // remove lost endpoint
        for (Endpoint e : mChildEndpointList) {
            if (!childEndpointSet.getSet().contains(e) && e != key) {
                Log.i(Constant.TAG, "remove lost Endpoint " + e);
                mChildEndpointList.remove(e);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void updateTableForChildEndpointSetMessage(ChildEndpointSet childEndpointSet, Endpoint key) {
        generationTable.getTable().put(key, childEndpointSet);
    }

    private void processBidMessage(String messageJson, Endpoint parent) {
        // forward it to parent
        mConnectionsClient.sendPayload(parent.getId(), Payload.fromBytes(messageJson.getBytes(UTF_8)));
    }

    private void processParcelMessage(String endpointId, String messageJson) {
        Log.i(Constant.TAG, String.format("mPayloadCallback onPayloadReceived: " +
                "receive Parcel from %s \n\n", endpointId));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<Message<Parcel>>() {
        }.getType();
        Message<Parcel> message = gson.fromJson(messageJson, type);
        parcel = message.getPayload();
        parcelMessage = message;

        setParcelGUI();
    }

    private void setParcelGUI() {
        mDestinationTextView.setText(parcel.getDestination());
        mDeadlineTextView.setText(parcel.getDeadline());
        mMeetingLocationTextView.setText(parcel.getMeetingLocation());
        mMeetingTimeTextView.setText(parcel.getMeetingTime());
        mPriceTextView.setText(String.valueOf(parcel.getPrice()));
        mBidEditText.setEnabled(true);
        mBidButton.setEnabled(true);
    }

    /**
     * Callbacks for connections to other devices.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    String info = String.format("mConnectionLifecycleCallback onConnectionInitiated: " +
                            "%s(%s)\n\n", connectionInfo.getEndpointName(), endpointId);
                    Log.i(Constant.TAG, info);
                    Toast.makeText(CourierActivity.this, info, Toast.LENGTH_SHORT).show();

                    mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);

                    // save cachedEndpoint to use in "ConnectionLifecycleCallback onConnectionResult"
                    cachedEndpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {

                        String info = String.format("mConnectionLifecycleCallback " +
                                "onConnectionResult: succeeded in connecting to %s", endpointId);
                        Log.i(Constant.TAG, info);
                        Toast.makeText(CourierActivity.this, info, Toast.LENGTH_SHORT).show();

                        if (parentEndpoint == null) {
                            Log.i(Constant.TAG, "mConnectionLifecycleCallback onConnectionResult: set parent, stopDiscovery");

                            parentEndpoint = cachedEndpoint;

                            setParentGUI();

                            mIsDiscovering = false;
                            mConnectionsClient.stopDiscovery();

                            startAdvertising();
                        } else {
                            updateForNewChildEndpoint();

                            Log.i(Constant.TAG, String.format("mConnectionLifecycleCallback " +
                                    "onConnectionResult: send parcel to new child %s", endpointId));
                            sendParcelToNewChildEndPoint(endpointId);
                        }

                        sendChildEndpointSetToParentEndpoint();
                        Log.i(Constant.TAG, String.format("mConnectionLifecycleCallback onConnectionResult: " +
                                "send childEndpointSet to parent: %s\n\n", Utils.getCurrentChildEndpointSet(generationTable)));

                    } else {
                        Log.i(Constant.TAG, String.format("mConnectionLifecycleCallback onConnectionResult: " +
                                "failed in connecting to %s\n\n", endpointId));
                    }

                    cachedEndpoint = null;
                }

                @Override
                public void onDisconnected(String endpointId) {
                    String info = String.format("mConnectionLifecycleCallback onDisconnected: %s", endpointId);
                    Log.i(Constant.TAG, info);
                    Toast.makeText(CourierActivity.this, info, Toast.LENGTH_SHORT).show();
                    if (parentEndpoint != null) {
                        if (parentEndpoint.getId().equals(endpointId)) {
                            parentEndpoint = null;

                            resetParcelGUI();

                            resetParentEndpointGUI();

                            mConnectionsClient.stopAllEndpoints();

                            mConnectionsClient.stopAdvertising();

                            mIsAdvertising = false;
                            startDiscovery();
                        } else {
                            updateForLostChildEndpoint(endpointId);

                            sendChildEndpointSetToParentEndpoint();
                            Log.i(Constant.TAG, String.format("mConnectionLifecycleCallback onDisconnected:" +
                                            " send new childEndpointSet to parent after losing %s : %s\n\n",
                                    endpointId, Utils.getCurrentChildEndpointSet(generationTable)));
                        }
                    }
                }
            };

    private void updateForLostChildEndpoint(String endpointId) {
        Endpoint endpoint = Utils.getEndpointFromTableById(generationTable, endpointId);
        updateListForLostChildEndpoint(endpoint);
        updateTableForLostChildEndpoint(endpoint);
    }

    private void updateTableForLostChildEndpoint(Endpoint endpoint) {
        generationTable.getTable().remove(endpoint);
    }

    private void updateListForLostChildEndpoint(Endpoint endpoint) {
        mChildEndpointList.remove(endpoint);
        mChildEndpointList.remove(generationTable.getTable().get(endpoint));
        mAdapter.notifyDataSetChanged();
    }

    private void resetParentEndpointGUI() {
        mParentLinearLayout.setVisibility(View.INVISIBLE);
    }

    private void resetParcelGUI() {
        mDestinationTextView.setText("null");
        mDeadlineTextView.setText("null");
        mMeetingLocationTextView.setText("null");
        mMeetingTimeTextView.setText("null");
        mPriceTextView.setText("null");
        mBidEditText.setEnabled(false);
        mBidButton.setEnabled(false);
    }

    private void sendChildEndpointSetToParentEndpoint() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Header header = new Header(Constant.MESSAGE_HEADER_CHILDENDPOINT_SET);
        ChildEndpointSet childEndpointSet = Utils.getCurrentChildEndpointSet(generationTable);
        Message<ChildEndpointSet> childEndpointSetMessage = new Message(header, childEndpointSet);
        String childEndpointSetMessageJson = gson.toJson(childEndpointSetMessage);
        mConnectionsClient.sendPayload(
                parentEndpoint.getId(), Payload.fromBytes(childEndpointSetMessageJson.getBytes(UTF_8)));
    }

    private void updateForNewChildEndpoint() {
        updateListForNewChildEndpoint();
        updateTableForNewChildEndpoint();
    }

    private void updateTableForNewChildEndpoint() {
        generationTable.getTable().put(cachedEndpoint, new ChildEndpointSet());
        Log.i(Constant.TAG, String.format("mConnectionLifecycleCallback onConnectionResult: " +
                "add new entry for generationTable: %s", generationTable.getTable()));
    }

    private void updateListForNewChildEndpoint() {
        mChildEndpointList.add(cachedEndpoint);
        mAdapter.notifyDataSetChanged();
    }

    private void sendParcelToNewChildEndPoint(String endpointId) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String messageJson = gson.toJson(parcelMessage);
        mConnectionsClient.sendPayload(
                endpointId, Payload.fromBytes(messageJson.getBytes(UTF_8)));
    }

    private void setParentGUI() {
        mParentLinearLayout.setVisibility(View.VISIBLE);
        mParentNameTextView.setText("Name: " + parentEndpoint.getName());
        mParentEndpointIdTextView.setText("EndpointId: " + parentEndpoint.getId());
    }


    /**
     * Callbacks for finding other devices
     */
    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    String logInfo1 = String.format("EndpointDiscoveryCallback onEndpointFound: %s(%s)",
                            info.getEndpointName(), endpointId);
                    Log.i(Constant.TAG, logInfo1);
                    Toast.makeText(CourierActivity.this, logInfo1, Toast.LENGTH_SHORT).show();

                    mConnectionsClient
                            .requestConnection(
                                    Utils.getDeviceName(), endpointId, mConnectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {
                                        // We successfully requested a connection. Now both sides
                                        // must accept before the connection is established.
                                        String logInfo2 = String.format("EndpointDiscoveryCallback, " +
                                                "onEndpointFound onSuccess: %s(%s)\n\n", info.getEndpointName(), endpointId);
                                        Log.i(Constant.TAG, logInfo2);
                                        Toast.makeText(CourierActivity.this, logInfo2, Toast.LENGTH_SHORT).show();
                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {
                                        // Nearby Connections failed to request the connection.
                                        String logInfo2 = String.format("EndpointDiscoveryCallback, " +
                                                "onEndpointFound onFailure: %s\n\n", e.getMessage());
                                        Log.i(Constant.TAG, logInfo2);
                                        Toast.makeText(CourierActivity.this, logInfo2, Toast.LENGTH_SHORT).show();
                                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    String info = String.format("EndpointDiscoveryCallback onEndpointLost: %s\n\n", endpointId);
                    Log.i(Constant.TAG, info);
                    Toast.makeText(CourierActivity.this, info, Toast.LENGTH_SHORT).show();
                }
            };

    private void startDiscovery() {
        mIsDiscovering = true;

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        mConnectionsClient
                .startDiscovery(
                        SERVICE_ID, mEndpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're discovering!
                            Log.i(Constant.TAG, "startDiscovery onSuccess\n\n");
                            Toast.makeText(this, "startDiscovery onSuccess", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We're unable to start discovering.
                            Log.i(Constant.TAG, "startDiscovery onFailure\n\n");
                            Toast.makeText(this, "startDiscovery onFailure", Toast.LENGTH_SHORT).show();
                        });
    }

    private void startAdvertising() {
        mIsAdvertising = true;

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();

        mConnectionsClient
                .startAdvertising(
                        Utils.getDeviceName(), SERVICE_ID, mConnectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            // We're advertising!
                            Log.i(Constant.TAG, "startAdvertising: onSuccess\n\n");
                            Toast.makeText(this, "startAdvertising: onSuccess", Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            // We were unable to start advertising.
                            Log.i(Constant.TAG, "startAdvertising: OnFailure\n\n");
                            Toast.makeText(this, "startAdvertising: OnFailure", Toast.LENGTH_SHORT).show();
                        });
    }


    public void bid(View view) {
        sendBidToParentEndpoint();
        disableBidGUI();
    }

    private void sendBidToParentEndpoint() {
        // prepare bid message
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Header header = new Header(Constant.MESSAGE_HEADER_BID, Utils.getDeviceName(), null); // use name because it can get its own endpointId
        Bid bid = new Bid(Integer.parseInt(mBidEditText.getText().toString()));
        Message<Bid> message = new Message(header, bid);
        String messageJson = gson.toJson(message);

        mConnectionsClient.sendPayload(parentEndpoint.getId(), Payload.fromBytes(messageJson.getBytes(UTF_8)));
    }

    private void disableBidGUI() {
        mBidEditText.setEnabled(false);
        mBidButton.setEnabled(false);
        mBidButton.setText("Sent Bid");
    }
}