package com.example.milk_app;

import android.content.Intent;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class CustomerProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView tvAvatar;
    private TextInputEditText etName, etEmail, etLocation;
    private TextView tvPhone, tvRole; // These fields are read-only
    private Button btnSaveChanges, btnLogout, btnChangeLocation;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentPhoneNumber;
    private ListenerRegistration profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get the phone number passed from the dashboard
        currentPhoneNumber = getIntent().getStringExtra("phoneNumber");

        // Initialize UI components
        tvAvatar = findViewById(R.id.tvAvatar);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etLocation = findViewById(R.id.etLocation);
        tvPhone = findViewById(R.id.tvPhone);
        tvRole = findViewById(R.id.tvRole);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnLogout = findViewById(R.id.btnLogout);
        btnChangeLocation = findViewById(R.id.btnChangeLocation);

        // Set click listeners
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
        btnLogout.setOnClickListener(v -> logout());
        if (btnChangeLocation != null) {
            btnChangeLocation.setOnClickListener(v -> openMapPicker());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentPhoneNumber != null) {
            fetchUserProfile();
        } else {
            Toast.makeText(this, "User phone number not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (profileListener != null) {
            profileListener.remove();
        }
    }

    private void fetchUserProfile() {
        // Attach a real-time listener to the user's profile document
        DocumentReference docRef = db.collection("users").document(currentPhoneNumber);
        profileListener = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String role = documentSnapshot.getString("role");
                String email = documentSnapshot.getString("email");
                String location = documentSnapshot.getString("location");

                // Update UI fields
                etName.setText(name);
                etEmail.setText(email);
                etLocation.setText(location);
                tvRole.setText(role);
                tvPhone.setText(currentPhoneNumber);

                String initials = (name != null && name.trim().length() > 0)
                        ? name.trim().substring(0, 1).toUpperCase()
                        : "C";
                tvAvatar.setText(initials);
            } else {
                Log.d(TAG, "No such document");
            }
        });
    }

    private static final int REQ_PICK_LOCATION = 2001;

    private void openMapPicker() {
        Intent i = new Intent(this, MapPickerActivity.class);
        startActivityForResult(i, REQ_PICK_LOCATION);
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
            db.collection("users").document(currentPhoneNumber)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update location", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "All fields must be filled.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("location", location);

        db.collection("users").document(currentPhoneNumber)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(CustomerProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(CustomerProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void logout() {
        mAuth.signOut();
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear(); // or just editor.putBoolean(LoginActivity.PREF_LOGIN_STATUS, false);
        editor.apply();
        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(CustomerProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
