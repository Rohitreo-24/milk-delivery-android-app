package com.example.milk_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class WishlistActivity extends AppCompatActivity implements WishlistAdapter.OnQtyChangeListener {
    private RecyclerView recycler;
    private TextView tvGrandTotal;
    private Button btnBuyWishlist;
    private FirebaseFirestore db;
    private String currentPhone;
    private ArrayList<WishlistItem> items = new ArrayList<>();
    private WishlistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        recycler = findViewById(R.id.recyclerWishlist);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        tvGrandTotal = findViewById(R.id.tvGrandTotal);
        btnBuyWishlist = findViewById(R.id.btnBuyWishlist);

        db = FirebaseFirestore.getInstance();
        android.content.SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentPhone = preferences.getString(LoginActivity.PREF_PHONE_NUMBER, null);

        if (currentPhone == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new WishlistAdapter(items, currentPhone, db, this);
        recycler.setAdapter(adapter);

        btnBuyWishlist.setOnClickListener(v -> launchUPIForTotal());

        loadWishlist();
    }

    private void loadWishlist() {
        db.collection("users").document(currentPhone)
                .collection("wishlist")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String name = d.getString("name");
                        String details = d.getString("details");
                        int price = d.getLong("price") != null ? d.getLong("price").intValue() : 0;
                        int qty = d.getLong("quantity") != null ? d.getLong("quantity").intValue() : 1;
                        items.add(new WishlistItem(name, details, price, qty));
                    }
                    adapter.notifyDataSetChanged();
                    recalcTotal();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load wishlist", Toast.LENGTH_SHORT).show());
    }

    private void recalcTotal() {
        int total = 0;
        for (WishlistItem it : items) total += it.price * it.quantity;
        tvGrandTotal.setText("Total: ₹" + total);
    }

    @Override
    public void onQtyChanged() {
        recalcTotal();
    }

    private void launchUPIForTotal() {
        int total = 0;
        for (WishlistItem it : items) total += it.price * it.quantity;
        if (total <= 0) {
            Toast.makeText(this, "Wishlist is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        String upiId = "ranjaniraj2121@okicici";
        String name = "Milk Delivery Service";
        String note = "Wishlist purchase";
        String amount = String.format(Locale.ROOT, "%.2f", total * 1.0);

        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", note)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .build();

        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);
        Intent chooser = Intent.createChooser(upiPayIntent, "Pay with");
        try {
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "No UPI app found!", Toast.LENGTH_SHORT).show();
        }
    }
}
