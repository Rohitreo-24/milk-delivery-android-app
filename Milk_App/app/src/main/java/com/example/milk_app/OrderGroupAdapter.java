package com.example.milk_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderGroupAdapter extends RecyclerView.Adapter<OrderGroupAdapter.GroupViewHolder> {

    public interface OnGroupActionListener {
        void onDeleteGroup(OrderGroup group);
    }

    private final List<OrderGroup> groups;
    private final OnGroupActionListener listener;
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public OrderGroupAdapter(List<OrderGroup> groups, OnGroupActionListener listener) {
        this.groups = groups;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_group, parent, false);
        return new GroupViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        OrderGroup g = groups.get(position);
        holder.tvRange.setText(fmt.format(g.getStartDate()) + " - " + fmt.format(g.getEndDate()));
        holder.tvDetails.setText("Morning: " + g.getMorningPlan() + "  Evening: " + g.getEveningPlan());
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteGroup(g);
        });
    }

    @Override
    public int getItemCount() { return groups.size(); }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvRange, tvDetails;
        ImageButton btnDelete;
        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRange = itemView.findViewById(R.id.tvGroupRange);
            tvDetails = itemView.findViewById(R.id.tvGroupDetails);
            btnDelete = itemView.findViewById(R.id.btnDeleteGroup);
        }
    }
}




