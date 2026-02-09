package com.example.milk_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private TextView txtName, txtDetails, txtPrice;
    private NumberPicker numberPicker;
    private Button btnWishlist, btnBuy, btnView3D;

    private String productName, productDetails, modelUrl;
    private int productPrice; // ✅ changed to int
    private FirebaseFirestore db;
    private String currentPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // 🔗 Get data from Intent
        productName = getIntent().getStringExtra("productName");
        productDetails = getIntent().getStringExtra("productDetails");
        productPrice = getIntent().getIntExtra("productPrice", 0); // ✅ fixed here
        modelUrl = getIntent().getStringExtra("modelUrl");
        int imageResId = getIntent().getIntExtra("imageResId", R.drawable.ic_milk_placeholder);

        // 🎯 Initialize views
        imgProduct = findViewById(R.id.imgProduct);
        txtName = findViewById(R.id.txtProductName);
        txtDetails = findViewById(R.id.txtProductDetails);
        txtPrice = findViewById(R.id.txtProductPrice);
        numberPicker = findViewById(R.id.numberPicker);
        btnWishlist = findViewById(R.id.btnAddWishlist);
        btnBuy = findViewById(R.id.btnBuy);
        btnView3D = findViewById(R.id.btnView3D);

        // 📋 Set data
        txtName.setText(productName);
        txtDetails.setText(productDetails);
        txtPrice.setText("₹" + productPrice); // ✅ shows proper integer price

        // 🧮 Quantity selector
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(10);

        // 🖼️ Show product image
        imgProduct.setImageResource(imageResId);

        db = FirebaseFirestore.getInstance();
        android.content.SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentPhone = preferences.getString(LoginActivity.PREF_PHONE_NUMBER, null);

        // ❤️ Wishlist (with selected quantity)
        btnWishlist.setOnClickListener(v -> addToWishlist());

        // 🛒 Buy now via UPI with total = price * quantity
        btnBuy.setOnClickListener(v -> launchUPI());

        // 🧭 View in 3D (AR via Scene Viewer)
        btnView3D.setOnClickListener(v -> openInAR());

    }

    private void addToWishlist() {
        if (currentPhone == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }
        int qty = numberPicker.getValue();
        Map<String, Object> data = new HashMap<>();
        data.put("name", productName);
        data.put("details", productDetails);
        data.put("price", productPrice);
        data.put("modelUrl", modelUrl);
        data.put("quantity", qty);
        data.put("createdAt", System.currentTimeMillis());
        db.collection("users").document(currentPhone)
                .collection("wishlist").document(productName)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> Toast.makeText(this, productName + " added to wishlist ❤️ (x" + qty + ")", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add to wishlist", Toast.LENGTH_SHORT).show());
    }

    private void launchUPI() {
        int quantity = numberPicker.getValue();
        int total = productPrice * quantity;
        String upiId = "ranjaniraj2121@okicici"; // same as MonthlyBillActivity
        String name = "Milk Delivery Service";
        String note = "Purchase: " + productName + " x" + quantity;
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

    private void openInAR() {
        String url = sanitizeModelUrl(modelUrl);
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Model not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri sceneViewerUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                .appendQueryParameter("file", url)
                .appendQueryParameter("mode", "ar_preferred")
                .appendQueryParameter("title", productName != null ? productName : "Product")
                .build();

        Intent arIntent = new Intent(Intent.ACTION_VIEW);
        arIntent.setData(sceneViewerUri);
        // Target Google app for Scene Viewer when available
        arIntent.setPackage("com.google.android.googlequicksearchbox");
        try {
            startActivity(arIntent);
        } catch (Exception e) {
            // Fallback to any capable handler (browser, etc.)
            Intent fallback = new Intent(Intent.ACTION_VIEW, sceneViewerUri);
            try {
                startActivity(fallback);
            } catch (Exception ex) {
                Toast.makeText(this, "No app found to view 3D model", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String sanitizeModelUrl(String src) {
        if (src == null) return null;
        // Convert GitHub blob URL to raw if needed
        // e.g., https://github.com/user/repo/blob/main/file.glb -> https://raw.githubusercontent.com/user/repo/main/file.glb
        if (src.contains("github.com") && src.contains("/blob/")) {
            try {
                return src.replace("https://github.com/", "https://raw.githubusercontent.com/")
                        .replace("/blob/", "/");
            } catch (Exception ignored) {}
        }
        return src;
    }
}
