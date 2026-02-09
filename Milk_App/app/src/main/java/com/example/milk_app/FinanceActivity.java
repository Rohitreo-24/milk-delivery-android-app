package com.example.milk_app;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinanceActivity extends AppCompatActivity {

    private TextView tvMonthTitle;
    private TextView tvRevenuePaid;
    private TextView tvAmountDue;
    private LinearLayout containerMonthly;
    private TextView tvAvgMilkPerDay;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finance);

        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        tvRevenuePaid = findViewById(R.id.tvRevenuePaid);
        tvAmountDue = findViewById(R.id.tvAmountDue);
        containerMonthly = findViewById(R.id.containerMonthly);
        tvAvgMilkPerDay = findViewById(R.id.tvAvgMilkPerDay);

        SimpleDateFormat monthFmtTitle = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthTitle.setText("Finance (" + monthFmtTitle.format(new Date()) + ")");

        loadFinanceData();
    }

    private void loadFinanceData() {
        SimpleDateFormat monthFmtKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat monthFmtLabel = new SimpleDateFormat("MMM", Locale.getDefault());
        final Date now = new Date();

        // Prepare last 4 month keys and labels
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(now);
        List<String> monthKeys = new ArrayList<>();
        List<String> monthLabels = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            java.util.Calendar c = (java.util.Calendar) cal.clone();
            c.add(java.util.Calendar.MONTH, -i);
            Date d = c.getTime();
            monthKeys.add(monthFmtKey.format(d));
            monthLabels.add(monthFmtLabel.format(d));
        }

        // Aggregates
        final Map<String, Integer> deliveredPerMonth = new HashMap<>();
        final double[] totals = new double[]{0.0, 0.0}; // [0]=revenuePaid, [1]=amountDue
        final double[] currentMonthLiters = new double[]{0.0};
        final String currentMonthKey = monthFmtKey.format(now);

        db.collection("users")
                .whereEqualTo("role", "Customer")
                .get()
                .addOnSuccessListener(usersSnap -> {
                    if (usersSnap.isEmpty()) {
                        renderMonthly(deliveredPerMonth, monthKeys, monthLabels, totals[0], totals[1], currentMonthLiters[0]);
                        return;
                    }

                    final int totalCustomers = usersSnap.size();
                    final int[] processed = new int[]{0};

                    for (QueryDocumentSnapshot userDoc : usersSnap) {
                        userDoc.getReference().collection("duepayment")
                                .get()
                                .addOnSuccessListener(dueSnap -> {
                                    for (DocumentSnapshot d : dueSnap.getDocuments()) {
                                        String date = d.getString("date"); // yyyy-MM-dd
                                        String payment = d.getString("payment");
                                        String milkPlan = d.getString("milkPlan");
                                        if (date == null || date.length() < 7) continue;
                                        String monthKey = date.substring(0, 7);
                                        if (!monthKeys.contains(monthKey)) continue; // only last 4 months
                                        if (milkPlan == null || milkPlan.equalsIgnoreCase("No Milk")) continue;

                                        // Count delivered order (duepayment implies delivered)
                                        deliveredPerMonth.put(monthKey, deliveredPerMonth.getOrDefault(monthKey, 0) + 1);

                                        // Revenue / Due totals
                                        double amount = parseAmountFromMilkPlan(milkPlan);
                                        if ("Paid".equalsIgnoreCase(payment)) {
                                            totals[0] += amount;
                                        } else {
                                            totals[1] += amount;
                                        }

                                        // Current month liters total for avg/day
                                        if (currentMonthKey.equals(monthKey)) {
                                            currentMonthLiters[0] += parseLitersFromMilkPlan(milkPlan);
                                        }
                                    }

                                    if (++processed[0] == totalCustomers) {
                                        renderMonthly(deliveredPerMonth, monthKeys, monthLabels, totals[0], totals[1], currentMonthLiters[0]);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (++processed[0] == totalCustomers) {
                                        renderMonthly(deliveredPerMonth, monthKeys, monthLabels, totals[0], totals[1], currentMonthLiters[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load finance data", Toast.LENGTH_SHORT).show());
    }

    private void renderMonthly(Map<String, Integer> deliveredPerMonth,
                               List<String> monthKeys,
                               List<String> monthLabels,
                               double revenuePaid,
                               double amountDue,
                               double currentMonthLiters) {
        tvRevenuePaid.setText("₹" + Math.round(revenuePaid));
        tvAmountDue.setText("₹" + Math.round(amountDue));

        // Determine max for scaling bars
        int maxCount = 1;
        for (String key : monthKeys) {
            int v = deliveredPerMonth.getOrDefault(key, 0);
            maxCount = Math.max(maxCount, v);
        }

        // Render bars for 4 months
        containerMonthly.removeAllViews();
        for (int i = 0; i < monthKeys.size(); i++) {
            String key = monthKeys.get(i);
            String label = monthLabels.get(i);
            int count = deliveredPerMonth.getOrDefault(key, 0);
            addMonthlyBar(containerMonthly, label, count, maxCount, Color.parseColor("#4CAF50"));
        }

        // Average milk per day for current month (calendar days elapsed)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH);
        double avg = dayOfMonth > 0 ? currentMonthLiters / dayOfMonth : 0.0;
        tvAvgMilkPerDay.setText(String.format(Locale.getDefault(), "Avg milk delivered per day: %.1f L", avg));
    }

    private void addMonthlyBar(LinearLayout container, String label, double value, double max, int color) {
        // Row container
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(6), 0, dp(6));

        // Month label
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setWidth(dp(28));
        row.addView(tv);

        // Bar background
        View barBg = new View(this);
        LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(0, dp(14), 1f);
        bgParams.setMargins(dp(8), 0, dp(8), 0);
        barBg.setLayoutParams(bgParams);
        barBg.setBackgroundColor(Color.parseColor("#EEEEEE"));

        // Bar value (foreground)
        View bar = new View(this);
        int widthPx = (int) (getResources().getDisplayMetrics().widthPixels * 0.6 * (value / max));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(widthPx, dp(14));
        bar.setLayoutParams(barParams);
        bar.setBackgroundColor(color);

        // Overlay bar on background using a small container
        LinearLayout barWrap = new LinearLayout(this);
        barWrap.setLayoutParams(bgParams);
        barWrap.addView(bar);

        // Value label
        TextView val = new TextView(this);
        val.setText(formatValue(value));
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        val.setPadding(dp(6), 0, 0, 0);

        // Click to show value
        row.setOnClickListener(v -> Toast.makeText(this, label + ": " + formatValue(value), Toast.LENGTH_SHORT).show());

        row.addView(barWrap);
        row.addView(val);
        container.addView(row);
    }

    private String formatValue(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.format(Locale.getDefault(), "%.1f", v);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private double parseAmountFromMilkPlan(String plan) {
        // Map common plans, fallback to liters * 40
        if (plan == null) return 0.0;
        String p = plan.trim().toLowerCase(Locale.getDefault());
        switch (p) {
            case "0.5l":
            case "500ml":
            case "0.5":
                return 20;
            case "1l":
            case "1 l":
            case "1":
            case "1000ml":
                return 40;
            case "1.5l":
            case "1.5":
                return 60;
            case "2l":
            case "2":
            case "2000ml":
                return 80;
        }
        double liters = parseLitersFromMilkPlan(plan);
        return liters * 40.0;
    }

    private double parseLitersFromMilkPlan(String plan) {
        if (plan == null) return 0.0;
        String p = plan.trim().toLowerCase(Locale.getDefault());
        try {
            if (p.endsWith("ml")) {
                p = p.replace("ml", "").trim();
                double ml = Double.parseDouble(p);
                return ml / 1000.0;
            }
            p = p.replace("l", "").trim();
            return Double.parseDouble(p);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
