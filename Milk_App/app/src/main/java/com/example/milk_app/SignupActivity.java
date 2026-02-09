package com.example.milk_app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_CODE = 2001;
    private static final int GPS_ENABLE_REQUEST = 3001;

    private EditText etName, etEmail, etPassword, etPhoneNumber;
    private Button btnSignup, btnSelectLocation;
    private Spinner spinnerRole;
    private TextView tvLocation;

    private FirebaseFirestore db;
    private Double selectedLatitude, selectedLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSignup = findViewById(R.id.btnSignup);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        tvLocation = findViewById(R.id.tvLocation);
        spinnerRole = findViewById(R.id.spinnerRole);

        // Populate the Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        // Select location button → check permission & GPS first
        btnSelectLocation.setOnClickListener(v -> checkLocationPermission());

        // Signup button
        btnSignup.setOnClickListener(v -> saveUserDataToFirestore());
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            checkGPSEnabled();
        }
    }

    private void checkGPSEnabled() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> openMapPicker());
        task.addOnFailureListener(e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ((ResolvableApiException) e).startResolutionForResult(SignupActivity.this, GPS_ENABLE_REQUEST);
                } catch (IntentSender.SendIntentException ex) {
                    ex.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Please enable location manually", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0.0);
            selectedLongitude = data.getDoubleExtra("longitude", 0.0);

            String locationText = String.format("Location selected: Lat %.4f, Long %.4f",
                    selectedLatitude, selectedLongitude);
            tvLocation.setText(locationText);
            tvLocation.setVisibility(View.VISIBLE);
        }

        // ✅ When user turns on GPS, wait a bit to let it stabilize, then open map
        if (requestCode == GPS_ENABLE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "GPS Enabled. Please wait...", Toast.LENGTH_SHORT).show();
                tvLocation.postDelayed(this::openMapPicker, 1500); // wait 1.5 sec before launching map
            } else {
                Toast.makeText(this, "GPS not enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void openMapPicker() {
        Intent intent = new Intent(SignupActivity.this, MapPickerActivity.class);
        startActivityForResult(intent, LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGPSEnabled();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveUserDataToFirestore() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()
                || selectedLatitude == null || selectedLongitude == null) {
            Toast.makeText(this, "Please fill all fields and select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("password", password);
        user.put("phoneNumber", phoneNumber);
        user.put("role", role);
        user.put("latitude", selectedLatitude);
        user.put("longitude", selectedLongitude);

        db.collection("users")
                .document(phoneNumber)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Signup Successful!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(SignupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
