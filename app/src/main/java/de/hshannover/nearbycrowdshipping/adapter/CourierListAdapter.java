package de.hshannover.nearbycrowdshipping.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.hshannover.nearbycrowdshipping.Constant;
import de.hshannover.nearbycrowdshipping.model.Courier;
import de.hshannover.nearbycrowdshipping.DelivererActivity;
import de.hshannover.nearbycrowdshipping.model.Endpoint;
import de.hshannover.nearbycrowdshipping.R;

public class CourierListAdapter extends RecyclerView.Adapter<CourierListAdapter.ViewHolder> {
    private ArrayList<Courier> mCourierList;
    private Context mContext;

    public CourierListAdapter(Context mContext, ArrayList<Courier> mCourierList) {
        this.mContext = mContext;
        this.mCourierList = mCourierList;
    }

    @NonNull
    @Override
    public CourierListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = LayoutInflater.from(mContext).inflate(R.layout.courier_list_item, parent, false);
        return new ViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Courier currentCourier = mCourierList.get(position);

        if (currentCourier.getBid() == -1) {
            holder.mLinearLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.grey_200));
        } else {
            Log.i(Constant.TAG, "bid < max courier, currentCourier: " + currentCourier.getBid());
            holder.mLinearLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.purple_200));
        }


        holder.mNameTextView.setText("Name: " + currentCourier.getEndpoint().getName());
        holder.mEndpointIdTextView.setText("EndpointId: " + currentCourier.getEndpoint().getId());
        holder.mBidTextView.setText("Bid: " + String.valueOf(currentCourier.getBid()));
    }


    @Override
    public int getItemCount() {
        return mCourierList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final CourierListAdapter mAdapter;

        public final LinearLayout mLinearLayout;

        public final TextView mNameTextView;
        public final TextView mEndpointIdTextView;
        public final TextView mBidTextView;


        ViewHolder(@NonNull View itemView, CourierListAdapter adapter) {
            super(itemView);
            mLinearLayout = itemView.findViewById(R.id.courier_list_item_linearlayout);
            mNameTextView = itemView.findViewById(R.id.courier_name);
            mEndpointIdTextView = itemView.findViewById(R.id.courier_endpointid);
            mBidTextView = itemView.findViewById(R.id.courier_bid);
            this.mAdapter = adapter;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int mPosition = getLayoutPosition();
            Courier courier = mCourierList.get(mPosition);
            Endpoint endpoint = courier.getEndpoint();
            ((DelivererActivity) mContext).sendWinnerMessage(endpoint);
        }
    }
}
