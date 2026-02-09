package com.example.milk_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    interface OnQtyChangeListener {
        void onQtyChanged();
    }

    private ArrayList<WishlistItem> items;
    private String phone;
    private FirebaseFirestore db;
    private OnQtyChangeListener qtyListener;

    public WishlistAdapter(ArrayList<WishlistItem> items, String phone, FirebaseFirestore db, OnQtyChangeListener l) {
        this.items = items;
        this.phone = phone;
        this.db = db;
        this.qtyListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        WishlistItem it = items.get(position);
        h.tvName.setText(it.name);
        h.tvDetails.setText(it.details);
        h.tvPrice.setText("₹" + it.price);
        h.tvQty.setText(String.valueOf(it.quantity));
        h.tvSubtotal.setText("₹" + (it.price * it.quantity));

        h.btnMinus.setOnClickListener(v -> changeQty(h.getAdapterPosition(), -1));
        h.btnPlus.setOnClickListener(v -> changeQty(h.getAdapterPosition(), +1));
        h.btnDelete.setOnClickListener(v -> deleteItem(h.getAdapterPosition()));
    }

    private void changeQty(int pos, int delta) {
        if (pos < 0 || pos >= items.size()) return;
        WishlistItem it = items.get(pos);
        int newQty = Math.max(1, it.quantity + delta);
        if (newQty == it.quantity) return;
        it.quantity = newQty;
        notifyItemChanged(pos);
        // persist
        db.collection("users").document(phone)
                .collection("wishlist").document(it.name)
                .update("quantity", it.quantity)
                .addOnSuccessListener(a -> { if (qtyListener != null) qtyListener.onQtyChanged(); })
                .addOnFailureListener(e -> { /* ignore */ });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDetails, tvPrice, tvQty, tvSubtotal;
        Button btnMinus, btnPlus;
        ImageButton btnDelete;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private void deleteItem(int pos) {
        if (pos < 0 || pos >= items.size()) return;
        WishlistItem it = items.get(pos);
        db.collection("users").document(phone)
                .collection("wishlist").document(it.name)
                .delete()
                .addOnSuccessListener(a -> {
                    items.remove(pos);
                    notifyItemRemoved(pos);
                    if (qtyListener != null) qtyListener.onQtyChanged();
                })
                .addOnFailureListener(e -> {/* ignore */});
    }
}
