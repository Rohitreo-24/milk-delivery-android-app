package com.example.milk_app;

// Imports from your new code
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List; // Added this import
import java.util.Locale;
import java.util.Map;

// Imports from the route planning logic
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class CustomerDeliveryListActivity extends AppCompatActivity implements DeliveryCustomerAdapter.OnItemClickListener {

    // --- Variables from your new code ---
    private RecyclerView recyclerView;
    private DeliveryCustomerAdapter adapter;
    private ArrayList<DeliveryItem> deliveryList;
    private FirebaseFirestore db;
    private String selectedBatch;
    private Button finishDeliveryButton;

    // --- Variables for Route Planning ---
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private Button routePlanningButton; // Added this variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_delivery_list);

        // --- Your existing onCreate code ---
        recyclerView = findViewById(R.id.deliveryRecyclerView);
        finishDeliveryButton = findViewById(R.id.finishDeliveryButton);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        deliveryList = new ArrayList<>();
        adapter = new DeliveryCustomerAdapter(deliveryList, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        selectedBatch = getIntent().getStringExtra("batch");

        if (selectedBatch != null) {
            fetchCustomersAndOrders();
        } else {
            Toast.makeText(this, "No batch selected.", Toast.LENGTH_SHORT).show();
            finish();
        }

        finishDeliveryButton.setOnClickListener(v -> finishDelivery());

        // --- Added code for Route Planning Button ---
        routePlanningButton = findViewById(R.id.routePlanningButton); // Find the new button
        routePlanningButton.setOnClickListener(v -> {
            // Check for location permissions before showing the route
            checkPermissionsAndShowRoute();
        });
        // ---------------------------------------------
    }

    // --- Your existing Firestore methods (UNMODIFIED) ---

    private void fetchCustomersAndOrders() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("users")
                .whereEqualTo("role", "Customer")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    deliveryList.clear();
                    for (QueryDocumentSnapshot userDoc : queryDocumentSnapshots) {
                        String customerId = userDoc.getId();
                        // Read from orderGroups/{today}/{batch}/details
                        String batchName = selectedBatch != null ? selectedBatch.toLowerCase() : "morning";
                        db.collection("users").document(customerId)
                                .collection("orderGroups").document(currentDate)
                                .collection(batchName).document("details")
                                .get()
                                .addOnSuccessListener(batchDoc -> {
                                    if (batchDoc.exists()) {
                                        String milkPlan = batchDoc.getString("milkPlan");
                                        String deliveryStatus = batchDoc.getString("deliveryStatus");
                                        String quickMessage = batchDoc.getString("quickMessage");

                                        if (milkPlan != null && !milkPlan.equals("No Milk")) {
                                            String customerName = userDoc.getString("name");
                                            String customerPhone = userDoc.getString("phoneNumber");
                                            String customerLocation = userDoc.getString("location");

                                            // --- MODIFIED: Fetch latitude and longitude ---
                                            Double latitude = userDoc.getDouble("latitude");
                                            Double longitude = userDoc.getDouble("longitude");
                                            // ---------------------------------------------

                                            DeliveryItem item = new DeliveryItem(customerName, customerPhone, customerLocation, milkPlan);
                                            if ("Delivered".equalsIgnoreCase(deliveryStatus)) {
                                                item.setDelivered(true);
                                            }
                                            if (quickMessage != null && !quickMessage.trim().isEmpty()) {
                                                item.setQuickMessage(quickMessage.trim());
                                            }

                                            // --- MODIFIED: Set lat/lng on the item ---
                                            if (latitude != null) {
                                                item.setLatitude(latitude);
                                            }
                                            if (longitude != null) {
                                                item.setLongitude(longitude);
                                            }

                                            deliveryList.add(item);
                                            adapter.notifyItemInserted(deliveryList.size() - 1);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FetchCustomers", "Error getting documents: ", e);
                    Toast.makeText(this, "Failed to load customers.", Toast.LENGTH_SHORT).show();
                });
    }

    private void finishDelivery() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Update deliveryStatus on the created order subdocuments instead of writing to customerReports
        String batchName = selectedBatch != null ? selectedBatch.toLowerCase() : "morning";
        WriteBatch batch = db.batch();

        // Prefer the adapter's updated list if available; otherwise fallback to current list
        java.util.List<DeliveryItem> items = (adapter != null && adapter.getUpdatedDeliveryList() != null)
                ? adapter.getUpdatedDeliveryList() : deliveryList;

        for (DeliveryItem item : items) {
            String customerPhone = item.getCustomerPhone();
            DocumentReference ref = db.collection("users").document(customerPhone)
                    .collection("orderGroups").document(currentDate)
                    .collection(batchName).document("details");

            Map<String, Object> updates = new HashMap<>();
            updates.put("deliveryStatus", item.isDelivered() ? "Delivered" : "Not Delivered");
            batch.set(ref, updates, com.google.firebase.firestore.SetOptions.merge());

            // If delivered, add/update duepayment entry for this date+batch
            if (item.isDelivered()) {
                String dueId = currentDate + "_" + batchName;
                DocumentReference dueRef = db.collection("users").document(customerPhone)
                        .collection("duepayment").document(dueId);
                Map<String, Object> due = new HashMap<>();
                due.put("date", currentDate);
                due.put("batch", batchName);
                due.put("milkPlan", item.getMilkPlan());
                due.put("payment", "Not paid");
                due.put("createdAt", System.currentTimeMillis());
                batch.set(dueRef, due, com.google.firebase.firestore.SetOptions.merge());
            }
        }

        batch.commit()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Delivery status updated.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("DeliveryUpdate", "Failed to update delivery status", e);
                    Toast.makeText(this, "Failed to update delivery status.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(DeliveryItem item) {
        showCustomerDetailsDialog(item);
    }

    private void showCustomerDetailsDialog(DeliveryItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_customer_details, null);
        builder.setView(dialogView);

        TextView name = dialogView.findViewById(R.id.dialog_name);
        TextView phone = dialogView.findViewById(R.id.dialog_phone);
        TextView location = dialogView.findViewById(R.id.dialog_location);
        TextView milk = dialogView.findViewById(R.id.dialog_milk);

        name.setText("Name: " + item.getCustomerName());
        phone.setText("Phone: " + item.getCustomerPhone());
        location.setText("Location: " + item.getCustomerLocation());
        milk.setText("Required: " + item.getMilkPlan());

        builder.setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // --- NEW METHODS ADDED FOR ROUTE PLANNING ---

    private void checkPermissionsAndShowRoute() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            showRouteInGoogleMaps();
        } else {
            // Permission is not granted, request it
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                showRouteInGoogleMaps();
            } else {
                // Permission was denied
                Toast.makeText(this, "Location permission is required to show the route", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showRouteInGoogleMaps() {
        // Build waypoints only from currently displayed batch, valid and unique, capped to 23
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        for (DeliveryItem item : deliveryList) {
            double lat = item.getLatitude();
            double lng = item.getLongitude();
            if (lat == 0.0 && lng == 0.0) continue;
            String coord = lat + "," + lng;
            unique.add(coord);
            if (unique.size() >= 23) break; // Google Maps waypoints limit
        }
        List<String> customerCoordinates = new ArrayList<>(unique);

        if (customerCoordinates.isEmpty()) {
            Toast.makeText(this, "No customer coordinates found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use Uri.Builder to safely construct the URL
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("www.google.com")
                .path("maps/dir/")
                .appendQueryParameter("api", "1")
                .appendQueryParameter("travelmode", "driving");

        // --- MODIFIED: Origin and Destination are now "My Location" ---
        // Use "My Location" to start from the user's current position
        builder.appendQueryParameter("origin", "My Location");
        // Use "My Location" again to create a round trip
        builder.appendQueryParameter("destination", "My Location");
        // -------------------------------------------------------------

        // All customer locations are waypoints (encoded by Uri.Builder)
        if (!customerCoordinates.isEmpty()) {
            // Use StringBuilder for compatibility instead of String.join
            StringBuilder waypointsString = new StringBuilder();
            for (int i = 0; i < customerCoordinates.size(); i++) {
                waypointsString.append(customerCoordinates.get(i));
                if (i < customerCoordinates.size() - 1) {
                    waypointsString.append("|");
                }
            }
            builder.appendQueryParameter("waypoints", waypointsString.toString());
        }
        // ---------------------------------------------------

        String url = builder.build().toString();
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        // Set the package to ensure it opens in Google Maps
        mapIntent.setPackage("com.google.android.apps.maps");

        // Verify that Google Maps is installed before starting the intent
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
            // Fallback: try to open in any app that can handle the URL
            Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if(genericMapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(genericMapIntent);
            }
        }
    }
}

