package com.example.milk_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ManageCustomersActivity extends AppCompatActivity implements CustomerAdapter.OnCustomerClickListener {

    private static final String TAG = "ManageCustomersActivity";
    private RecyclerView customerRecyclerView;
    private ArrayList<Customer> customerList;
    private CustomerAdapter adapter;
    private FirebaseFirestore db;
    private FrameLayout detailsFragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_customers);

        customerRecyclerView = findViewById(R.id.customerRecyclerView);
        detailsFragmentContainer = findViewById(R.id.detailsFragmentContainer);
        customerRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        customerList = new ArrayList<>();
        adapter = new CustomerAdapter(customerList, this);
        customerRecyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        fetchCustomers();

        // This is the key addition
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                // If the back stack is empty (no fragments), hide the container
                detailsFragmentContainer.setVisibility(View.GONE);
            }
        });
    }

    private void fetchCustomers() {
        db.collection("users")
                .whereEqualTo("role", "Customer")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        customerList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String phoneNumber = document.getString("phoneNumber");
                            String location = document.getString("location");
                            String morningPlan = document.getString("morningPlan");
                            String eveningPlan = document.getString("eveningPlan");

                            Customer customer = new Customer(name, phoneNumber, location, morningPlan, eveningPlan);
                            customerList.add(customer);
                        }
                        adapter.notifyDataSetChanged();
                        if (customerList.isEmpty()) {
                            Toast.makeText(this, "No customers found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(this, "Failed to load customers.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCustomerClick(Customer customer) {
        // Show the fragment container
        detailsFragmentContainer.setVisibility(View.VISIBLE);

        CustomerDetailsFragment fragment = new CustomerDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("name", customer.getName());
        bundle.putString("phoneNumber", customer.getPhoneNumber());
        bundle.putString("location", customer.getLocation());
        bundle.putString("morningPlan", customer.getMorningPlan());
        bundle.putString("eveningPlan", customer.getEveningPlan());
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.detailsFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}