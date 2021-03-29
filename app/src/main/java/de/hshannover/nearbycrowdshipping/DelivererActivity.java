package de.hshannover.nearbycrowdshipping;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.hshannover.nearbycrowdshipping.adapter.CourierListAdapter;
import de.hshannover.nearbycrowdshipping.model.Bid;
import de.hshannover.nearbycrowdshipping.model.ChildEndpointSet;
import de.hshannover.nearbycrowdshipping.model.Courier;
import de.hshannover.nearbycrowdshipping.model.Endpoint;
import de.hshannover.nearbycrowdshipping.model.GenerationTable;
import de.hshannover.nearbycrowdshipping.model.Header;
import de.hshannover.nearbycrowdshipping.model.Message;
import de.hshannover.nearbycrowdshipping.model.Parcel;

import static de.hshannover.nearbycrowdshipping.Constant.SERVICE_ID;
import static de.hshannover.nearbycrowdshipping.Constant.STRATEGY;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DelivererActivity extends AppCompatActivity {

    private TextView mDeviceNameTextView;
    private TextView mCourierMaxTextView;
    private EditText mDestinationEditText;
    private EditText mDeadlineEditText;
    private EditText mMeetingLocationEditText;
    private EditText mMeetingTimeEditText;
    private EditText mPriceEditText;
    private Button mBroadCastParcelButton;

    private Courier cachedCourier;

    private GenerationTable generationTable;

    private Parcel parcel;

    private ArrayList<Courier> mCourierList;
    private RecyclerView mRecyclerView;
    private CourierListAdapter mAdapter;

    /**
     * Handler to Nearby Connections.
     */
    private ConnectionsClient mConnectionsClient;

    /**
     * Callbacks for payloads (bytes of data) sent from another device to this device.
     */
    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Toast.makeText(DelivererActivity.this, new String(payload.asBytes(), UTF_8), Toast.LENGTH_SHORT).show();

                    String messageJson = new String(payload.asBytes());
                    String messageHeaderType = Utils.getMessageHeaderType(payload);

                    switch (messageHeaderType) {
                        case Constant.MESSAGE_HEADER_BID:
                            processBidMessage(messageJson);
                            break;
                        case Constant.MESSAGE_HEADER_CHILDENDPOINT_SET:
                            processChildEndpointSetMessage(endpointId, messageJson);
                            break;
                        default:
                            Log.i(Constant.TAG, "mPayloadCallback onPayloadReceived: unknown message type");
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                }
            };

    /**
     * Callbacks for connections to other devices.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    String logInfo = String.format("mConnectionLifecycleCallback " +
                            "onConnectionInitiated: %s(%s)\n\n", connectionInfo.getEndpointName(), endpointId);
                    Log.i(Constant.TAG, logInfo);
                    Toast.makeText(DelivererActivity.this, logInfo, Toast.LENGTH_SHORT).show();

                    mConnectionsClient.acceptConnection(endpointId, mPayloadCallback);

                    // saved to cachedCourier to use later
                    cachedCourier = new Courier(new Endpoint(endpointId, connectionInfo.getEndpointName()));
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        String logInfo = String.format("mConnectionLifecycleCallback " +
                                "onConnectionResult: succeeded in connecting to %s", endpointId);
                        Log.i(Constant.TAG, logInfo);
                        Toast.makeText(DelivererActivity.this, logInfo, Toast.LENGTH_SHORT).show();


                        // add new entry for generation Table
                        generationTable.getTable().put(cachedCourier.getEndpoint(), new ChildEndpointSet());
                        Log.i(Constant.TAG, "mConnectionLifecycleCallback onConnectionResult: " +
                                "add new entry for table: " + generationTable.getTable() + "\n\n");

                        // add new connected courier to mCourierList and show it
                        mCourierList.add(cachedCourier);
                        mAdapter.notifyDataSetChanged();

                        sendParcelToNewChild(endpointId);

                    } else {
                        Log.i(Constant.TAG, "mConnectionLifecycleCallback onConnectionResult: connection failed" + "\n\n");
                    }

                    cachedCourier = null;
                }

                @Override
                public void onDisconnected(String endpointId) {
                    String info = String.format("mConnectionLifecycleCallback onDisconnected: %s", endpointId);
                    Log.i(Constant.TAG, info);
                    Toast.makeText(DelivererActivity.this, info, Toast.LENGTH_SHORT).show();

                    updateForLostEndpoint(endpointId);

                    Log.i(Constant.TAG, String.format("mConnectionLifecycleCallback onDisconnected: " +
                            "update table: %s\n\n", generationTable.getTable()));

                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deliverer);

        setGUI();

        mConnectionsClient = Nearby.getConnectionsClient(this);

        generationTable = new GenerationTable();
    }

    private void setGUI() {
        mDeviceNameTextView = findViewById(R.id.device_name_text);
        mDeviceNameTextView.setText(mDeviceNameTextView.getText() + " " + Utils.getDeviceName());

        mBroadCastParcelButton = findViewById(R.id.broadcastParcel);

        mDestinationEditText = findViewById(R.id.destination_text);
        mDeadlineEditText = findViewById(R.id.deadline_text);
        mMeetingLocationEditText = findViewById(R.id.meetingLocation_text);
        mMeetingTimeEditText = findViewById(R.id.meetingTime_text);
        mPriceEditText = findViewById(R.id.price_text);

        TextWatcher parcelTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkTheConditionsForParcel();
            }
        };

        mDestinationEditText.addTextChangedListener(parcelTextWatcher);
        mDeadlineEditText.addTextChangedListener(parcelTextWatcher);
        mMeetingLocationEditText.addTextChangedListener(parcelTextWatcher);
        mMeetingTimeEditText.addTextChangedListener(parcelTextWatcher);
        mPriceEditText.addTextChangedListener(parcelTextWatcher);

        mCourierMaxTextView = findViewById(R.id.courier_max_text);

        // create RecyclerView to show the courier list;
        mCourierList = new ArrayList<>();
        mRecyclerView = findViewById(R.id.courier_list_recyclerview);
        mAdapter = new CourierListAdapter(this, mCourierList);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void checkTheConditionsForParcel() {
        if (mDestinationEditText.getText().toString().trim().isEmpty() ||
                mDeadlineEditText.getText().toString().trim().isEmpty() ||
                mMeetingLocationEditText.getText().toString().trim().isEmpty() ||
                mMeetingTimeEditText.getText().toString().trim().isEmpty() ||
                mPriceEditText.getText().toString().trim().isEmpty()) {
            mBroadCastParcelButton.setEnabled(false);
        } else {
            mBroadCastParcelButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnectionsClient.stopAdvertising();
        mConnectionsClient.stopAllEndpoints();
    }

    public void broadcastParcel(View view) {
        parcel = new Parcel(mDestinationEditText.getText().toString(),
                mDeadlineEditText.getText().toString(),
                mMeetingLocationEditText.getText().toString(),
                mMeetingTimeEditText.getText().toString(),
                Integer.parseInt(mPriceEditText.getText().toString()));
        disableBroadCastParcelButton();
        startAdvertising();
    }

    private void disableBroadCastParcelButton() {
        mBroadCastParcelButton.setText("Broadcasted Parcel");
        mBroadCastParcelButton.setEnabled(false);
    }

    private void startAdvertising() {
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

    private void updateForLostEndpoint(String endpointId) {
        Courier courier = Utils.getCourierInListById(mCourierList, endpointId);

        updateCourierListForLostEndpoint(courier);

        updateTableForLostEndpoint(courier);
    }

    private void updateTableForLostEndpoint(Courier courier) {
        generationTable.getTable().remove(courier.getEndpoint());
    }

    private void updateCourierListForLostEndpoint(Courier courier) {
        List<Courier> toRemove = new ArrayList<>();
        toRemove.add(courier);
        for (Endpoint e : generationTable.getTable().get(courier.getEndpoint()).getSet()) {
            toRemove.add(Utils.getCourierInListByEndpoint(mCourierList, e));
        }
        mCourierList.removeAll(toRemove);
        mAdapter.notifyDataSetChanged();
        updateCourierBest();
    }

    private void sendParcelToNewChild(String endpointId) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Header header = new Header(Constant.MESSAGE_HEADER_PARCEL);
        Message<Parcel> message = new Message(header, parcel);
        String messageJson = gson.toJson(message);
        mConnectionsClient.sendPayload(
                endpointId, Payload.fromBytes(messageJson.getBytes(UTF_8)));
    }

    private void processChildEndpointSetMessage(String endpointId, String messageJson) {
        Log.i(Constant.TAG, "mPayloadCallback onPayloadReceived: received ChildEndpointSet from " + endpointId);

        // get childEndpointSet
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<Message<ChildEndpointSet>>() {
        }.getType();
        Message<ChildEndpointSet> message = gson.fromJson(messageJson, type);
        ChildEndpointSet receivedChildEndpointSet = message.getPayload();

        Log.i(Constant.TAG, "mPayloadCallback onPayloadReceived: ChildEndpointSet: " + receivedChildEndpointSet.getSet());

        Endpoint key = Utils.getEndpointFromTableById(generationTable, endpointId);

        updateCourierListForChildEndpointSetMessage(key, receivedChildEndpointSet);

        updateCourierBest();

        updateTableForChildEndpointSetMessage(key, receivedChildEndpointSet);

        Log.i(Constant.TAG, "mPayloadCallback onPayloadReceived: " +
                "update table: " + generationTable.getTable() + "\n\n");
    }

    private void updateTableForChildEndpointSetMessage(Endpoint key, ChildEndpointSet receivedChildEndpointSet) {
        generationTable.getTable().put(key, receivedChildEndpointSet);
    }

    private void updateCourierListForChildEndpointSetMessage(Endpoint key, ChildEndpointSet receivedChildEndpointSet) {
        HashSet<Endpoint> currentChildEndpointSet = generationTable.getTable().get(key).getSet();

        HashSet<Endpoint> intersection = new HashSet<>(currentChildEndpointSet);
        intersection.retainAll(receivedChildEndpointSet.getSet());

        // get endpoints not in new ChildEndpointSet and remove it
        HashSet<Endpoint> onlyInCurrentChildEndpointSet = new HashSet<>(currentChildEndpointSet);
        onlyInCurrentChildEndpointSet.removeAll(intersection);
        for (Endpoint e : onlyInCurrentChildEndpointSet) {
            Courier c = Utils.getCourierInListByEndpoint(mCourierList, e);
            mCourierList.remove(c);
            mAdapter.notifyDataSetChanged();
        }

        // get endpoints in new ChildEndpointSet but not in current ChildEndpointSet and remove it
        HashSet<Endpoint> onlyInReceivedChildEndpointSet = new HashSet<>(receivedChildEndpointSet.getSet());
        onlyInReceivedChildEndpointSet.removeAll(intersection);
        for (Endpoint e : onlyInReceivedChildEndpointSet) {
            Courier c = new Courier(e);
            mCourierList.add(c);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void processBidMessage(String messageJson) {
        // get Message<Bid> Object
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<Message<Bid>>() {
        }.getType();
        Message<Bid> message = gson.fromJson(messageJson, type);

        Header header = message.getHeader();
        Bid bid = message.getPayload();
        String name = header.getFrom();

        Log.i(Constant.TAG, "mPayloadCallback onPayloadReceived: received Bid from " + name);
        setBidForCourierByName(bid, name);
    }

    private void setBidForCourierByName(Bid bid, String name) {
        Courier c = Utils.getCourierInListByName(mCourierList, name);
        if (c != null) {
            c.setBid(bid.getPrice());
            Log.i(Constant.TAG, "index of c" + mCourierList.indexOf(c));
            mAdapter.notifyDataSetChanged();
            updateCourierBest();
        }
    }

    private void updateCourierBest() {
        if (mCourierList.size() == 0) {
            mCourierMaxTextView.setText("null");
        } else {
            Courier max = mCourierList.get(0);
            for (Courier c : mCourierList) {
                if (c.getBid() > max.getBid()) {
                    max = c;
                }
            }
            mCourierMaxTextView.setText(String.format("%s(%s)", max.getEndpoint().getName(), max.getEndpoint().getId()));
        }
    }

    public void sendWinnerMessage(Endpoint endpoint) {
        sendWinnerMessageToAllChildEndpoint(endpoint);

        showResultDialog();
    }

    private void sendWinnerMessageToAllChildEndpoint(Endpoint endpoint) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Header header = new Header(Constant.MESSAGE_HEADER_WINNER, null, endpoint.getName());
        Message message = new Message(header, null);
        String messageJson = gson.toJson(message);

        for (Endpoint e : generationTable.getTable().keySet()) {
            mConnectionsClient.sendPayload(e.getId(), Payload.fromBytes(messageJson.getBytes(UTF_8)));
        }
    }

    private void showResultDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Parcel transfer is completed!")
                .setMessage("The result ist sent to all the couriers.")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mConnectionsClient.stopAllEndpoints();
                        startActivity(new Intent(DelivererActivity.this, MainActivity.class));
                    }
                })
                .show();
    }

}