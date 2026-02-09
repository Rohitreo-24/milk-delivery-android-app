package com.example.milk_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MonthlyBillActivity extends AppCompatActivity {

    private RecyclerView billRecyclerView;
    private BillAdapter billAdapter;
    private ArrayList<DailyRecord> dailyRecords;
    private TextView totalBillTextView;
    private Button payNowButton;

    private FirebaseFirestore db;
    private String customerName;
    private String phoneNumber;
    private double totalAmount = 0.0;

    private static final int UPI_PAYMENT_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_bill);

        billRecyclerView = findViewById(R.id.billRecyclerView);
        totalBillTextView = findViewById(R.id.totalBillTextView);
        payNowButton = findViewById(R.id.payNowButton);

        billRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyRecords = new ArrayList<>();
        billAdapter = new BillAdapter(dailyRecords);
        billRecyclerView.setAdapter(billAdapter);

        db = FirebaseFirestore.getInstance();

        customerName = getIntent().getStringExtra("userName");
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        if (customerName == null || customerName.isEmpty()) {
            Toast.makeText(this, "Missing user details. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        fetchMonthlyBillData();

        payNowButton.setOnClickListener(v -> initiateUPIPayment());
    }

    private void fetchMonthlyBillData() {
        dailyRecords.clear();
        totalAmount = 0.0;

        java.text.SimpleDateFormat monthFmt = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault());
        java.text.SimpleDateFormat dayFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String currentMonth = monthFmt.format(new java.util.Date());

        db.collection("users").document(phoneNumber)
                .collection("duepayment")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String date = d.getString("date"); // yyyy-MM-dd
                        String payment = d.getString("payment");
                        String milkPlan = d.getString("milkPlan");
                        String batch = d.getString("batch");
                        if (date == null || !date.startsWith(currentMonth)) continue;
                        if (!"Not paid".equalsIgnoreCase(payment)) continue;
                        if (milkPlan == null || milkPlan.equalsIgnoreCase("No Milk")) continue;

                        double amount = parseAmountFromMilkPlan(milkPlan);
                        String label = (batch != null ? (batch.substring(0,1).toUpperCase()+batch.substring(1)) : "") + " " + milkPlan;
                        dailyRecords.add(new DailyRecord(date, label, "Delivered", amount));
                        totalAmount += amount;
                    }
                    finalizeBillList();
                })
                .addOnFailureListener(err -> Toast.makeText(MonthlyBillActivity.this, "Error fetching bill data.", Toast.LENGTH_SHORT).show());
    }

    private void finalizeBillList() {
        dailyRecords.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        billAdapter.notifyDataSetChanged();
        totalBillTextView.setText(String.format("Total: ₹%.2f", totalAmount));
    }

    private double parseAmountFromMilkPlan(String plan) {
        if (plan == null) return 0.0;
        String p = plan.toLowerCase(Locale.ROOT);
        if (p.contains("no milk")) return 0.0;
        if (p.contains("0.5") || p.contains("500")) return 20.0;
        if (p.contains("1.5")) return 60.0;
        if (p.contains("2")) return 80.0;
        if (p.contains("1")) return 40.0;
        // Fallback: extract numeric liters and apply 40 per liter
        try {
            String num = p.replaceAll("[^0-9.]", "");
            if (!num.isEmpty()) {
                double liters = Double.parseDouble(num);
                return liters * 40.0;
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    // ------------------------- 🔽 UPI PAYMENT HANDLER -------------------------

    private void initiateUPIPayment() {
        if (totalAmount <= 0) {
            Toast.makeText(this, "No pending amount to pay!", Toast.LENGTH_SHORT).show();
            return;
        }

        String upiId = "ranjaniraj2121@okicici"; // Supplier UPI ID
        String name = "Milk Delivery Service";
        String note = "Milk Bill Payment for " + customerName;
        String amount = String.format(Locale.ROOT, "%.2f", totalAmount);

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
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "No UPI app found!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            if (data != null) {
                String response = data.getStringExtra("response");
                Log.d("UPI_RESPONSE", "Response: " + response);

                if (response != null && response.toLowerCase().contains("success")) {
                    Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();
                    updatePaymentStatusInFirestore();
                } else {
                    Toast.makeText(this, "Payment Failed or Cancelled", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ------------------------- 🔽 FIRESTORE UPDATE -------------------------

    private void updatePaymentStatusInFirestore() {
        java.text.SimpleDateFormat monthFmt = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault());
        String currentMonth = monthFmt.format(new java.util.Date());

        db.collection("users").document(phoneNumber)
                .collection("duepayment")
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String date = d.getString("date");
                        String payment = d.getString("payment");
                        if (date == null || !date.startsWith(currentMonth)) continue;
                        if (!"Not paid".equalsIgnoreCase(payment)) continue;
                        batch.set(d.getReference(), java.util.Collections.singletonMap("payment", "Paid"), SetOptions.merge());
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Payment marked as Done!", Toast.LENGTH_LONG).show();
                                fetchMonthlyBillData();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update payment status.", Toast.LENGTH_SHORT).show();
                                Log.e("Firestore", "Error updating payment status", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update payment status.", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error updating payment status", e);
                });
    }
}
