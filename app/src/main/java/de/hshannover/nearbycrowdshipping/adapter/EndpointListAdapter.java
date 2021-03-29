package de.hshannover.nearbycrowdshipping.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.hshannover.nearbycrowdshipping.model.Endpoint;
import de.hshannover.nearbycrowdshipping.R;

public class EndpointListAdapter extends RecyclerView.Adapter<EndpointListAdapter.ViewHolder> {
    private ArrayList<Endpoint> mEndpointList;
    private Context mContext;

    public EndpointListAdapter(Context mContext, ArrayList<Endpoint> mEndpointList) {
        this.mContext = mContext;
        this.mEndpointList = mEndpointList;
    }

    @NonNull
    @Override
    public EndpointListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = LayoutInflater.from(mContext).inflate(R.layout.endpoint_list_item, parent, false);
        return new ViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Endpoint endpoint = mEndpointList.get(position);
        holder.mNameTextView.setText("Name: " + endpoint.getName());
        holder.mEndpointIdTextView.setText("EndpointId: " + endpoint.getId());
    }

    @Override
    public int getItemCount() {
        return mEndpointList != null ? mEndpointList.size() : 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final EndpointListAdapter mAdapter;

        public final TextView mNameTextView;
        public final TextView mEndpointIdTextView;

        public ViewHolder(@NonNull View itemView, EndpointListAdapter adapter) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.endpoint_name);
            mEndpointIdTextView = itemView.findViewById(R.id.endpoint_id);
            this.mAdapter = adapter;
        }
    }
}
