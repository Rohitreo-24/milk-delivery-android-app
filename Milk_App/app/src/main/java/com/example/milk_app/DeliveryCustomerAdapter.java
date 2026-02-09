package com.example.milk_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeliveryCustomerAdapter extends RecyclerView.Adapter<DeliveryCustomerAdapter.DeliveryViewHolder> {

    private final List<DeliveryItem> deliveryList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DeliveryItem item);
    }

    public DeliveryCustomerAdapter(List<DeliveryItem> deliveryList, OnItemClickListener listener) {
        this.deliveryList = deliveryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeliveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_delivery_customer, parent, false);
        return new DeliveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeliveryViewHolder holder, int position) {
        DeliveryItem item = deliveryList.get(position);
        holder.customerName.setText(item.getCustomerName());
        holder.requiredMilk.setText("Required: " + item.getMilkPlan());
        holder.deliveryStatusCheckbox.setChecked(item.isDelivered());
        holder.deliveryStatusCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setDelivered(isChecked);
        });

        // Quick message handling
        String qm = item.getQuickMessage();
        if (qm != null && !qm.trim().isEmpty()) {
            holder.quickMessage.setText("Note: " + qm.trim());
            holder.quickMessage.setVisibility(View.VISIBLE);
            if (holder.quickMessageBubble != null) {
                holder.quickMessageBubble.setVisibility(View.VISIBLE);
            }
        } else {
            holder.quickMessage.setVisibility(View.GONE);
            if (holder.quickMessageBubble != null) {
                holder.quickMessageBubble.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return deliveryList.size();
    }

    // Getter to retrieve the list with updated delivery statuses
    public List<DeliveryItem> getUpdatedDeliveryList() {
        return deliveryList;
    }

    public static class DeliveryViewHolder extends RecyclerView.ViewHolder {
        TextView customerName;
        TextView requiredMilk;
        CheckBox deliveryStatusCheckbox;
        TextView quickMessage;
        TextView quickMessageBubble;

        public DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.tvCustomerName);
            requiredMilk = itemView.findViewById(R.id.tvRequiredMilk);
            deliveryStatusCheckbox = itemView.findViewById(R.id.deliveryStatusCheckbox);
            quickMessage = itemView.findViewById(R.id.tvQuickMessage);
            quickMessageBubble = itemView.findViewById(R.id.tvQuickMessageBubble);
        }
    }
}