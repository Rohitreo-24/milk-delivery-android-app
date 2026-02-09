package com.example.milk_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FarmerProfileActivity extends AppCompatActivity {

    private static final String TAG = "FarmerProfileActivity";
    private TextView tvAvatar, tvRole, tvPhoneNumber;
    private EditText etName, etEmail, etLocation; // Added etEmail and etLocation
    private Button btnSaveChanges, btnLogout, btnChangeLocation; // Add change location button

    private FirebaseFirestore db;
    private String farmerPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_profile); // Ensure this points to the new XML

        db = FirebaseFirestore.getInstance();

        // Initialize Views - Ensure IDs match the XML
        tvAvatar = findViewById(R.id.tvAvatar);
        etName = findViewById(R.id.etName);
        tvPhoneNumber = findViewById(R.id.tvPhone); // Corrected ID to tvPhone from XML
        tvRole = findViewById(R.id.tvRole);
        etEmail = findViewById(R.id.etEmail);     // New: from XML
        etLocation = findViewById(R.id.etLocation); // New: from XML

        btnSaveChanges = findViewById(R.id.btnSaveChanges); // Corrected ID from XML
        btnLogout = findViewById(R.id.btnLogout);
        btnChangeLocation = findViewById(R.id.btnChangeLocation);

        // Get the phone number from SharedPreferences or Intent (prefer SharedPreferences for robustness)
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        farmerPhoneNumber = preferences.getString(LoginActivity.PREF_PHONE_NUMBER, null);

        if (farmerPhoneNumber == null || farmerPhoneNumber.isEmpty()) {
            Toast.makeText(this, "User phone number not found. Please log in again.", Toast.LENGTH_LONG).show();
            // Optionally redirect to login if no phone number
            logout(); // Forces a re-login
            return;
        }
        // Fetch and display farmer data
        fetchFarmerData();

        // Set up button listeners
        btnSaveChanges.setOnClickListener(v -> updateProfile());
        btnLogout.setOnClickListener(v -> logout());
        if (btnChangeLocation != null) {
            btnChangeLocation.setOnClickListener(v -> startChangeLocationFlow());
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (pendingOpenMap && isLocationEnabled()) {
            pendingOpenMap = false;
            Intent i = new Intent(this, MapPickerActivity.class);
            startActivityForResult(i, REQ_PICK_LOCATION);
        }
    }

    private void fetchFarmerData() {
        DocumentReference userRef = db.collection("users").document(farmerPhoneNumber);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String role = documentSnapshot.getString("role");
                String email = documentSnapshot.getString("email");
                String location = documentSnapshot.getString("location");

                // Populate views with data
                etName.setText(name);
                tvPhoneNumber.setText(farmerPhoneNumber); // Phone number is displayed, not edited
                tvRole.setText(role != null ? role : "Farmer"); // Default to "Farmer" if role is null
                etEmail.setText(email);
                etLocation.setText(location);

                // Set avatar with the first letter of the name
                String initials = (name != null && name.trim().length() > 0)
                        ? name.trim().substring(0, 1).toUpperCase()
                        : "F"; // Default to 'F' if name is empty
                tvAvatar.setText(initials);

            } else {
                Toast.makeText(this, "Farmer profile not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching farmer data", e);
            Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateProfile() {
        String newName = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newLocation = etLocation.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Basic email validation
        if (!newEmail.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("users").document(farmerPhoneNumber);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("location", newLocation);

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    // Update SharedPreferences with the new name (as it's used in dashboard)
                    SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putString(LoginActivity.PREF_USER_NAME, newName).apply();
                    // Optionally refresh UI if needed, or simply trust the data is updated
                    tvAvatar.setText(newName.trim().substring(0, 1).toUpperCase()); // Update avatar immediately
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
        // Clear login state from SharedPreferences
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        // Redirect to LoginActivity and clear activity stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ------------------- Location change flow -------------------
    private static final int REQ_PICK_LOCATION = 2101;
    private boolean pendingOpenMap = false;

    private void startChangeLocationFlow() {
        // Check location services turned on
        if (!isLocationEnabled()) {
            Toast.makeText(this, "Please turn on Location services", Toast.LENGTH_SHORT).show();
            pendingOpenMap = true;
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        // Permissions are requested by MapPicker if needed; open picker directly
        Intent i = new Intent(this, MapPickerActivity.class);
        startActivityForResult(i, REQ_PICK_LOCATION);
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0.0);
            double lng = data.getDoubleExtra("longitude", 0.0);
            Map<String, Object> updates = new HashMap<>();
            updates.put("latitude", lat);
            updates.put("longitude", lng);
            db.collection("users").document(farmerPhoneNumber)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update location", Toast.LENGTH_SHORT).show());
        }
    }
}