package com.example.milk_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Note: Unnecessary Firebase and other imports have been removed.

public class CustomerDetailsFragment extends Fragment {

    // Member variables for the views that still exist in the layout
    private TextView nameTextView, phoneTextView, locationTextView;

    public CustomerDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static CustomerDetailsFragment newInstance(String name, String phoneNumber, String location) {
        CustomerDetailsFragment fragment = new CustomerDetailsFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("phoneNumber", phoneNumber);
        args.putString("location", location);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the updated layout file
        return inflater.inflate(R.layout.fragment_customer_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize only the views that exist in the simplified layout
        nameTextView = view.findViewById(R.id.dialog_name);
        phoneTextView = view.findViewById(R.id.dialog_phone);
        locationTextView = view.findViewById(R.id.dialog_location);

        Bundle bundle = getArguments();
        if (bundle != null) {
            String name = bundle.getString("name");
            String phoneNumber = bundle.getString("phoneNumber");
            String location = bundle.getString("location");

            // Set the text for the views. No need to fetch data from Firestore anymore.
            nameTextView.setText("Name: " + (name != null ? name : "N/A"));
            phoneTextView.setText("Phone: " + (phoneNumber != null ? phoneNumber : "N/A"));
            locationTextView.setText("Location: " + (location != null ? location : "N/A"));

        } else {
            Toast.makeText(getContext(), "No customer data available.", Toast.LENGTH_SHORT).show();
        }
    }

    // The loadActiveOrderDetails() method has been completely removed as it is no longer needed.
}