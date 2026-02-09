package com.example.milk_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText etPhoneNumber, etPassword;
    private Button btnLogin;
    private TextView tvSignup;
    private Spinner spinnerRole;

    private FirebaseFirestore db;

    public static final String PREFS_NAME = "MilkAppPrefs";
    public static final String PREF_LOGIN_STATUS = "isLoggedIn";
    public static final String PREF_PHONE_NUMBER = "phoneNumber";
    public static final String PREF_USER_ROLE = "userRole";
    public static final String PREF_USER_NAME = "userName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserLoggedIn()) {
            redirectToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();

        etPhoneNumber = findViewById(R.id.etLoginPhoneNumber);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignup = findViewById(R.id.tvSignup);
        spinnerRole = findViewById(R.id.spinnerRole);

        btnLogin.setOnClickListener(v -> loginUser());
        tvSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private boolean isUserLoggedIn() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getBoolean(PREF_LOGIN_STATUS, false);
    }

    private void saveLoginState(String phoneNumber, String userRole, String userName) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(PREF_LOGIN_STATUS, true)
                .putString(PREF_PHONE_NUMBER, phoneNumber)
                .putString(PREF_USER_ROLE, userRole)
                .putString(PREF_USER_NAME, userName)
                .apply();
    }

    private void clearLoginState() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    private void redirectToDashboard() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userRole = preferences.getString(PREF_USER_ROLE, "");
        String phoneNumber = preferences.getString(PREF_PHONE_NUMBER, "");
        String userName = preferences.getString(PREF_USER_NAME, "");

        Intent intent;
        if ("Customer".equalsIgnoreCase(userRole)) {
            intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
        } else if ("Milkman".equalsIgnoreCase(userRole)) {
            intent = new Intent(LoginActivity.this, FarmerDashboardActivity.class);
        } else {
            Toast.makeText(this, "Unknown user role. Please log in.", Toast.LENGTH_SHORT).show();
            clearLoginState();
            return;
        }

        intent.putExtra("phoneNumber", phoneNumber);
        intent.putExtra("userName", userName);
        startActivity(intent);
        finish();
    }

    private void startDashboardDirectly(String userRole, String phoneNumber, String userName) {
        Intent intent;
        if ("Customer".equalsIgnoreCase(userRole)) {
            intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
        } else if ("Milkman".equalsIgnoreCase(userRole)) {
            intent = new Intent(LoginActivity.this, FarmerDashboardActivity.class);
        } else {
            Toast.makeText(this, "Invalid user role.", Toast.LENGTH_SHORT).show();
            return;
        }
        intent.putExtra("phoneNumber", phoneNumber);
        intent.putExtra("userName", userName);

        saveLoginState(phoneNumber, userRole, userName);
        startActivity(intent);
        finish();
    }

    private void loginUser() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String selectedRole = spinnerRole.getSelectedItem().toString();

        if (phoneNumber.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter phone number and password", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(phoneNumber).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Login error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    if (document == null || !document.exists()) {
                        Toast.makeText(LoginActivity.this, "User not registered.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String storedPassword = document.getString("password");
                    String userRole = document.getString("role");
                    String userName = document.getString("name");

                    Log.d(TAG, "Firestore role=" + userRole + " | Selected role=" + selectedRole);

                    if (storedPassword == null || !storedPassword.equals(password)) {
                        Toast.makeText(LoginActivity.this, "Invalid credentials.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (userRole == null || userName == null) {
                        Toast.makeText(LoginActivity.this, "User data incomplete.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // STRICT role check
                    if (!userRole.equalsIgnoreCase(selectedRole)) {
                        Toast.makeText(LoginActivity.this,
                                "Role mismatch: this account is registered as '" + userRole + "'.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    startDashboardDirectly(userRole, phoneNumber, userName);
                });
    }
}
