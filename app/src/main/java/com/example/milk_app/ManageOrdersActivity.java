package com.example.milk_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ManageOrdersActivity extends AppCompatActivity {

    FirebaseFirestore db;
    String phoneNumber; // unique identifier for user
    RecyclerView rvUpcomingOrders;
    Button fabAddOrder;
    OrderGroupAdapter adapter;
    List<OrderGroup> upcomingOrders = new ArrayList<>();
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        db = FirebaseFirestore.getInstance();

        rvUpcomingOrders = findViewById(R.id.rvUpcomingOrders);
        fabAddOrder = findViewById(R.id.fabAddOrder);

        // Get phone number from intent
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        // Setup upcoming orders list
        rvUpcomingOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderGroupAdapter(upcomingOrders, this::deleteGroup);
        rvUpcomingOrders.setAdapter(adapter);

        // Load upcoming orders
        loadUpcomingOrders();

        // Add order via button
        fabAddOrder.setOnClickListener(v -> {
            AddOrderBottomSheetFragment.newInstance(phoneNumber)
                    .show(getSupportFragmentManager(), "AddOrderBottomSheet");
        });
    }

    private void loadUpcomingOrders() {
        if (phoneNumber == null || phoneNumber.isEmpty()) return;

        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();

        // simplified query to avoid index errors
        Query query = db.collection("users").document(phoneNumber)
                .collection("orderGroups")
                .orderBy("startDate", Query.Direction.ASCENDING);

        ordersListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshots != null) {
                // Aggregate per-date docs into single range entries by groupId
                java.util.Map<String, OrderGroup> groupsById = new java.util.HashMap<>();
                for (DocumentSnapshot d : snapshots.getDocuments()) {
                    String groupId = d.getString("groupId");
                    Long rangeStart = d.getLong("rangeStart");
                    Long rangeEnd = d.getLong("rangeEnd");
                    if (groupId == null || rangeStart == null || rangeEnd == null) continue;

                    OrderGroup g = groupsById.get(groupId);
                    if (g == null) {
                        g = new OrderGroup();
                        g.setId(groupId);
                        g.setStartDate(rangeStart);
                        g.setEndDate(rangeEnd);
                        g.setPhoneNumber(phoneNumber);
                        // Placeholders so adapter UI remains stable
                        g.setMorningPlan("-");
                        g.setEveningPlan("-");
                        groupsById.put(groupId, g);
                    }
                }

                // Replace list with aggregated values filtered by end date
                upcomingOrders.clear();
                for (OrderGroup g : groupsById.values()) {
                    if (g.getEndDate() >= startOfToday) {
                        upcomingOrders.add(g);
                    }
                }
                // Sort by start date ascending
                java.util.Collections.sort(upcomingOrders, (a, b) -> Long.compare(a.getStartDate(), b.getStartDate()));
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void deleteGroup(OrderGroup group) {
        if (group == null || group.getId() == null) return;

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Order")
                .setMessage("Are you sure you want to delete this order?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Optimistically remove from UI
                    int index = upcomingOrders.indexOf(group);
                    if (index >= 0) {
                        upcomingOrders.remove(index);
                        adapter.notifyItemRemoved(index);
                    }

                    // Delete all per-date docs that belong to this groupId
                    db.collection("users").document(phoneNumber)
                            .collection("orderGroups")
                            .whereEqualTo("groupId", group.getId())
                            .get()
                            .addOnSuccessListener(snap -> {
                                com.google.firebase.firestore.WriteBatch batch = db.batch();
                                for (DocumentSnapshot d : snap.getDocuments()) {
                                    // delete parent per-date doc
                                    batch.delete(d.getReference());
                                    // also attempt to delete subcollection docs morning/details and evening/details
                                    com.google.firebase.firestore.DocumentReference mRef = d.getReference().collection("morning").document("details");
                                    com.google.firebase.firestore.DocumentReference eRef = d.getReference().collection("evening").document("details");
                                    batch.delete(mRef);
                                    batch.delete(eRef);
                                }
                                batch.commit()
                                        .addOnSuccessListener(v2 -> Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(err -> Toast.makeText(this, "Delete failed: " + err.getMessage(), Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(err ->
                                    Toast.makeText(this, "Delete failed: " + err.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
        }
    }
}
