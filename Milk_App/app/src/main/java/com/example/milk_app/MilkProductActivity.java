package com.example.milk_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MilkProductActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MilkProductAdapter adapter;
    private ArrayList<MilkProduct> productList;
    private FirebaseFirestore db;
    private String currentPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milk_product);

        recyclerView = findViewById(R.id.recyclerMilkProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Button btnMyWishlist = findViewById(R.id.btnMyWishlist);

        db = FirebaseFirestore.getInstance();
        android.content.SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentPhone = preferences.getString(LoginActivity.PREF_PHONE_NUMBER, null);

        // 🥛 Sample products
        productList = new ArrayList<>();
        productList.add(new MilkProduct("Panner", "100g pack", 35,
                "https://github.com/Rohitreo-24/milkapp-assets/blob/main/panner.glb", R.drawable.panner));
        productList.add(new MilkProduct("Butter", "200g salted", 95,
                "https://github.com/Rohitreo-24/milkapp-assets/blob/main/butter.glb", R.drawable.butter));
        productList.add(new MilkProduct("Curd", "500g cup", 45,
                "https://github.com/Rohitreo-24/milkapp-assets/blob/main/curd.glb", R.drawable.curd));
        productList.add(new MilkProduct("Cheese", "200g block", 120,
                "https://github.com/Rohitreo-24/milkapp-assets/blob/main/cheese_wedge.glb", R.drawable.cheese));
        productList.add(new MilkProduct("Milk Cake", "250g box", 110,
                "https://github.com/Rohitreo-24/milkapp-assets/blob/main/bread.glb", R.drawable.plain_cake));


        adapter = new MilkProductAdapter(productList, new MilkProductAdapter.OnItemClickListener() {
            @Override
            public void onAddToWishlist(MilkProduct product) {
                addToWishlist(product);
            }

            @Override
            public void onPurchase(MilkProduct product) {
                launchUPI(product.getName(), product.getPrice());
            }

            @Override
            public void onViewDetails(MilkProduct product) {
                Intent intent = new Intent(MilkProductActivity.this, ProductDetailActivity.class);
                intent.putExtra("productName", product.getName());
                intent.putExtra("productDetails", product.getDetails());
                intent.putExtra("productPrice", product.getPrice());
                intent.putExtra("modelUrl", product.getModelUrl());
                intent.putExtra("imageResId", product.getImageResId());
                startActivity(intent);
            }
        });


        recyclerView.setAdapter(adapter);

        btnMyWishlist.setOnClickListener(v -> {
            Intent i = new Intent(MilkProductActivity.this, WishlistActivity.class);
            startActivity(i);
        });
    }

    private void addToWishlist(MilkProduct p) {
        if (currentPhone == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("name", p.getName());
        data.put("details", p.getDetails());
        data.put("price", p.getPrice());
        data.put("modelUrl", p.getModelUrl());
        data.put("imageResId", p.getImageResId());
        data.put("createdAt", System.currentTimeMillis());
        data.put("quantity", 1);
        db.collection("users").document(currentPhone)
                .collection("wishlist").document(p.getName())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> Toast.makeText(this, p.getName() + " added to wishlist ❤️", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add to wishlist", Toast.LENGTH_SHORT).show());
    }

    private void launchUPI(String productName, int amountInt) {
        String upiId = "ranjaniraj2121@okicici"; // same as MonthlyBillActivity
        String name = "Milk Delivery Service";
        String note = "Purchase: " + productName;
        String amount = String.format(Locale.ROOT, "%.2f", amountInt * 1.0);

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
