package com.example.milk_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MilkProductAdapter extends RecyclerView.Adapter<MilkProductAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onAddToWishlist(MilkProduct product);
        void onPurchase(MilkProduct product);
        void onViewDetails(MilkProduct product);
    }

    private ArrayList<MilkProduct> products;
    private OnItemClickListener listener;

    public MilkProductAdapter(ArrayList<MilkProduct> products, OnItemClickListener listener) {
        this.products = products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MilkProduct product = products.get(position);

        holder.txtName.setText(product.getName());
        holder.txtDetails.setText(product.getDetails());
        holder.txtPrice.setText("₹" + product.getPrice());
        holder.imgProduct.setImageResource(product.getImageResId());


        holder.btnWishlist.setOnClickListener(v -> listener.onAddToWishlist(product));
        holder.btnBuy.setOnClickListener(v -> listener.onPurchase(product));

        // ✅ Entire item opens details
        holder.itemLayout.setOnClickListener(v -> listener.onViewDetails(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDetails, txtPrice;
        Button btnWishlist, btnBuy;
        LinearLayout itemLayout;
        ImageView imgProduct;

        ViewHolder(View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.layoutProductItem);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtDetails = itemView.findViewById(R.id.txtProductDetails);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
            btnWishlist = itemView.findViewById(R.id.btnAddWishlist);
            btnBuy = itemView.findViewById(R.id.btnBuy);
            imgProduct = itemView.findViewById(R.id.imgProduct);
        }
    }
}
