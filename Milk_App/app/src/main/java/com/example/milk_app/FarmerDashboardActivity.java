package com.example.milk_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FarmerDashboardActivity extends AppCompatActivity {

    CardView cardCustomers, cardFinance, cardProfile, cardFeedback;
    Button btnStartDelivering;

    // New TextViews to display demand
    private TextView tvTotalDemand, tvMorningDemand, tvEveningDemand;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        cardCustomers = findViewById(R.id.cardCustomers);
        cardFinance = findViewById(R.id.cardFinance);
        cardProfile = findViewById(R.id.cardProfile);
        cardFeedback = findViewById(R.id.cardFeedback);
        btnStartDelivering = findViewById(R.id.btnStartDelivering);

        // Initialize new TextViews
        tvTotalDemand = findViewById(R.id.tvTotalDemand);
        tvMorningDemand = findViewById(R.id.tvMorningDemand);
        tvEveningDemand = findViewById(R.id.tvEveningDemand);

        // Fetch and display the daily demand
        fetchDailyMilkDemand();

        // Manage Customers
        cardCustomers.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Customers...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(FarmerDashboardActivity.this, ManageCustomersActivity.class));
        });

        // Financial Analysis
        cardFinance.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Financial Analysis...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(FarmerDashboardActivity.this, FinanceActivity.class));
        });

        // Profile
        cardProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Profile...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(FarmerDashboardActivity.this, FarmerProfileActivity.class);
            intent.putExtra("userName", getIntent().getStringExtra("userName"));
            startActivity(intent);
        });

        // Feedback / Support
        cardFeedback.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Feedback...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(FarmerDashboardActivity.this, FeedbackDisplayActivity.class);
            startActivity(intent);
        });

        // Start Delivering Button
        btnStartDelivering.setOnClickListener(v -> {
            Toast.makeText(this, "Starting Delivery...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(FarmerDashboardActivity.this, DeliveryOptionsActivity.class);
            startActivity(intent);
        });
    }

    private void fetchDailyMilkDemand() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Use a final array to hold mutable values
        final double[] totalDemands = new double[2]; // Index 0 for morning, Index 1 for evening

        db.collection("users")
                .whereEqualTo("role", "Customer")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String customerId = document.getId();

                            // Morning batch doc
                            db.collection("users").document(customerId)
                                    .collection("orderGroups").document(currentDate)
                                    .collection("morning").document("details")
                                    .get()
                                    .addOnSuccessListener(morningDoc -> {
                                        if (morningDoc.exists()) {
                                            String morningPlan = morningDoc.getString("milkPlan");
                                            if (morningPlan != null) {
                                                try {
                                                    String milkValueStr = morningPlan.replaceAll("[^0-9.]", "");
                                                    if (!milkValueStr.isEmpty()) {
                                                        double value = Double.parseDouble(milkValueStr);
                                                        totalDemands[0] += value;
                                                        tvMorningDemand.setText("Morning Demand: " + totalDemands[0] + "L");
                                                        tvTotalDemand.setText("Total Today's Demand: " + (totalDemands[0] + totalDemands[1]) + "L");
                                                    }
                                                } catch (NumberFormatException e) {
                                                    Log.e("MilkApp", "Invalid morning plan value: " + morningPlan);
                                                }
                                            }
                                        }
                                    });

                            // Evening (Afternoon) batch doc
                            db.collection("users").document(customerId)
                                    .collection("orderGroups").document(currentDate)
                                    .collection("evening").document("details")
                                    .get()
                                    .addOnSuccessListener(eveningDoc -> {
                                        if (eveningDoc.exists()) {
                                            String eveningPlan = eveningDoc.getString("milkPlan");
                                            if (eveningPlan != null) {
                                                try {
                                                    String milkValueStr = eveningPlan.replaceAll("[^0-9.]", "");
                                                    if (!milkValueStr.isEmpty()) {
                                                        double value = Double.parseDouble(milkValueStr);
                                                        totalDemands[1] += value;
                                                        tvEveningDemand.setText("Afternoon Demand: " + totalDemands[1] + "L");
                                                        tvTotalDemand.setText("Total Today's Demand: " + (totalDemands[0] + totalDemands[1]) + "L");
                                                    }
                                                } catch (NumberFormatException e) {
                                                    Log.e("MilkApp", "Invalid evening plan value: " + eveningPlan);
                                                }
                                            }
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(FarmerDashboardActivity.this, "Failed to load customer list.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}