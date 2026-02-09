package com.example.milk_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private final ArrayList<DailyRecord> dailyRecords;

    public BillAdapter(ArrayList<DailyRecord> dailyRecords) {
        this.dailyRecords = dailyRecords;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill_record, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        DailyRecord record = dailyRecords.get(position);
        holder.dateTextView.setText(record.getDate());
        holder.batchTextView.setText(record.getBatch());
        holder.statusTextView.setText(record.getStatus());
        holder.amountTextView.setText(String.format("₹%.2f", record.getAmount()));
    }

    @Override
    public int getItemCount() {
        return dailyRecords.size();
    }

    public static class BillViewHolder extends RecyclerView.ViewHolder {
        public TextView dateTextView;
        public TextView batchTextView;
        public TextView statusTextView;
        public TextView amountTextView;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.recordDate);
            batchTextView = itemView.findViewById(R.id.recordBatch);
            statusTextView = itemView.findViewById(R.id.recordStatus);
            amountTextView = itemView.findViewById(R.id.recordAmount);
        }
    }
}