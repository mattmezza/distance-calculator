package me.matteomerola.distancecalculator;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by matt on 2/19/17.
 */

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    private static final String TAG = TripAdapter.class.getSimpleName();
    private List<Trip> items;
    private OnClickListener listener;
    private int itemLayoutId;

    public TripAdapter(List<Trip> initial, int itemLayoutId) {
        this.items = initial;
        this.itemLayoutId = itemLayoutId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(this.itemLayoutId, parent, false);
        ViewHolder vh = new ViewHolder(v);
        vh.attachListener(this.listener);
        return vh;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    public void addTripItem(int pos, Trip item) {
        this.items.add(pos, item);
    }

    public Trip getTripItemAt(int pos) {
        return this.items.get(pos);
    }

    public void removeTripItem(Trip item) {
        this.items.remove(item);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Trip trip = items.get(position);
        holder.fromTv.setText(trip.fromAddress);
        holder.toTv.setText(trip.toAddress);
        holder.meanTv.setText(trip.mean);
        holder.durationTv.setText(trip.duration);
        holder.distanceTv.setText(trip.distance);

    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView fromTv;
        TextView toTv;
        TextView meanTv;
        TextView durationTv;
        TextView distanceTv;
        private OnClickListener listener;

        public ViewHolder(View itemView) {
            super(itemView);

            fromTv = (TextView) itemView.findViewById(R.id.fromTv);
            toTv = (TextView) itemView.findViewById(R.id.toTv);
            meanTv = (TextView) itemView.findViewById(R.id.meanTv);
            durationTv = (TextView) itemView.findViewById(R.id.durationTv);
            distanceTv = (TextView) itemView.findViewById(R.id.distanceTv);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void attachListener(OnClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            if(this.listener!=null) {
                this.listener.onClick(view, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if(this.listener!=null) {
                this.listener.onLongClick(view, getAdapterPosition());
            }
            return true;
        }
    }

    public interface OnClickListener {
        public void onClick(View view, int pos);
        public void onLongClick(View view, int pos);
    }
}
