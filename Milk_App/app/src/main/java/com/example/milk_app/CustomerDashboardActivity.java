package com.example.milk_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    TextView tvWelcome, tvPlan;
    // Add the new CardView variable
    CardView cardBills, cardOrders, cardProfile, cardFeedback, cardMilkProducts;

    private FirebaseFirestore db;
    private String currentPhoneNumber;
    private ListenerRegistration planListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvPlan = findViewById(R.id.tvPlan);
        cardBills = findViewById(R.id.cardBills);
        cardOrders = findViewById(R.id.cardOrders);
        cardProfile = findViewById(R.id.cardProfile);
        cardFeedback = findViewById(R.id.cardFeedback);
        // Find the new CardView by its ID from the layout
        cardMilkProducts = findViewById(R.id.cardMilkProducts);

        String userName = getIntent().getStringExtra("userName");
        currentPhoneNumber = getIntent().getStringExtra("phoneNumber");

        if (userName != null) {
            tvWelcome.setText(" Welcome, " + userName + "!");
        } else {
            tvWelcome.setText(" Welcome, Customer!");
        }

        // No longer fetching plan here. We'll do it in onStart().

        cardBills.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Bills...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CustomerDashboardActivity.this, MonthlyBillActivity.class);
            intent.putExtra("userName", getIntent().getStringExtra("userName"));
            intent.putExtra("phoneNumber", currentPhoneNumber);
            startActivity(intent);
        });

        cardOrders.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, ManageOrdersActivity.class);
            intent.putExtra("phoneNumber", currentPhoneNumber);
            startActivity(intent);
        });

        cardProfile.setOnClickListener(v ->{
            Toast.makeText(this, "Opening Profile...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CustomerDashboardActivity.this, CustomerProfileActivity.class);
            intent.putExtra("phoneNumber", currentPhoneNumber);
            startActivity(intent);
        });

        cardFeedback.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, FeedbackActivity.class);
            startActivity(intent);
        });

        // Add the listener for the new "Milk Products" card
        cardMilkProducts.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, MilkProductActivity.class);
            // You can pass the phone number along if needed, just like with other activities
            intent.putExtra("phoneNumber", currentPhoneNumber);
            startActivity(intent);
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Attach the real-time listener when the activity becomes visible
        if (currentPhoneNumber != null && !currentPhoneNumber.isEmpty()) {
            fetchCurrentDayPlan();
        } else {
            tvPlan.setText("User data not found.");
            Toast.makeText(this, "Failed to get user data.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach the listener to avoid memory leaks and unnecessary fetches
        if (planListener != null) {
            planListener.remove();
        }
    }

    /**
     * Fetches the customer's delivery plan for the current day using a real-time listener.
     */
    private void fetchCurrentDayPlan() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Read batch details from orderGroups/{today}/{morning|evening}/details
        db.collection("users")
                .document(currentPhoneNumber)
                .collection("orderGroups")
                .document(currentDate)
                .collection("morning").document("details")
                .get()
                .addOnSuccessListener(morningDoc -> {
                    String morningPlan = morningDoc.exists() ? morningDoc.getString("milkPlan") : null;

                    db.collection("users")
                            .document(currentPhoneNumber)
                            .collection("orderGroups")
                            .document(currentDate)
                            .collection("evening").document("details")
                            .get()
                            .addOnSuccessListener(eveningDoc -> {
                                String eveningPlan = eveningDoc.exists() ? eveningDoc.getString("milkPlan") : null;

                                if ((morningPlan != null && !"No Milk".equals(morningPlan)) ||
                                        (eveningPlan != null && !"No Milk".equals(eveningPlan))) {
                                    String mText = morningPlan != null ? morningPlan : "No Milk";
                                    String eText = eveningPlan != null ? eveningPlan : "No Milk";
                                    tvPlan.setText("Today's Delivery \n"+
                                            "\t\t\t\t\t\t\t\tMorning : " + mText +
                                            "\n\t\t\t\t\t\t\t\tEvening : " + eText);
                                } else {
                                    tvPlan.setText("No active plan found for today.");
                                }
                            })
                            .addOnFailureListener(err -> {
                                Log.e(TAG, "Evening fetch failed", err);
                                tvPlan.setText("Failed to load plan.");
                            });
                })
                .addOnFailureListener(err -> {
                    Log.e(TAG, "Morning fetch failed", err);
                    tvPlan.setText("Failed to load plan.");
                });
    }
}
