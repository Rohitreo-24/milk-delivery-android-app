package com.example.milk_app;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText; // Import EditText
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddOrderBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_PHONE = "phoneNumber";

    public static AddOrderBottomSheetFragment newInstance(String phoneNumber) {
        AddOrderBottomSheetFragment f = new AddOrderBottomSheetFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PHONE, phoneNumber);
        f.setArguments(b);
        return f;
    }

    private Spinner spinnerMorning, spinnerEvening;
    private TextView tvDateRange;
    // --- MODIFICATION START ---
    private Button btnPickRange, btnSave, btnQuickMessage;
    private EditText etQuickMessage;
    // --- MODIFICATION END ---
    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private FirebaseFirestore db;
    private String phoneNumber;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Ensure you are using the new layout file name if it's different
        return inflater.inflate(R.layout.fragment_add_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spinnerMorning = view.findViewById(R.id.spinnerMorningNew);
        spinnerEvening = view.findViewById(R.id.spinnerEveningNew);
        tvDateRange = view.findViewById(R.id.tvDateRange);
        btnPickRange = view.findViewById(R.id.btnPickDateRange);
        btnSave = view.findViewById(R.id.btnSaveNewOrder);

        // --- MODIFICATION START ---
        // Initialize the new UI components
        btnQuickMessage = view.findViewById(R.id.btnQuickMessage);
        etQuickMessage = view.findViewById(R.id.etQuickMessage);
        // --- MODIFICATION END ---


        db = FirebaseFirestore.getInstance();
        phoneNumber = getArguments() != null ? getArguments().getString(ARG_PHONE) : null;

        String[] milkOptions = {"No Milk", "0.5L", "1L", "1.5L", "2L"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, milkOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMorning.setAdapter(adapter);
        spinnerEvening.setAdapter(adapter);

        updateDateRangeLabel();

        btnPickRange.setOnClickListener(v -> pickRange());
        btnSave.setOnClickListener(v -> saveRangeOrders());

        // --- MODIFICATION START ---
        // Add click listener to toggle the visibility of the message box
        btnQuickMessage.setOnClickListener(v -> {
            if (etQuickMessage.getVisibility() == View.VISIBLE) {
                etQuickMessage.setVisibility(View.GONE);
            } else {
                etQuickMessage.setVisibility(View.VISIBLE);
            }
        });
        // --- MODIFICATION END ---
    }

    private void pickRange() {
        DatePickerDialog startPicker = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            DatePickerDialog endPicker = new DatePickerDialog(requireContext(), (view2, year2, month2, dayOfMonth2) -> {
                endCal.set(year2, month2, dayOfMonth2, 0, 0, 0);
                endCal.set(Calendar.MILLISECOND, 0);
                if (endCal.before(startCal)) {
                    Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
                } else {
                    updateDateRangeLabel();
                }
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH));
            endPicker.show();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH));
        startPicker.show();
    }

    private void updateDateRangeLabel() {
        tvDateRange.setText(fmt.format(startCal.getTime()) + " - " + fmt.format(endCal.getTime()));
    }

    private void saveRangeOrders() {
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(requireContext(), "User not identified", Toast.LENGTH_SHORT).show();
            return;
        }

        String morning = String.valueOf(spinnerMorning.getSelectedItem());
        String evening = String.valueOf(spinnerEvening.getSelectedItem());
        // --- MODIFICATION START ---
        // Get the message text from the EditText
        String quickMessage = etQuickMessage.getText().toString().trim();
        // --- MODIFICATION END ---
        SimpleDateFormat idFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // New group data
        Map<String, Object> groupDoc = new HashMap<>();
        groupDoc.put("phoneNumber", phoneNumber);
        groupDoc.put("startDate", startCal.getTimeInMillis());
        groupDoc.put("endDate", endCal.getTimeInMillis());
        groupDoc.put("morningPlan", morning);
        groupDoc.put("eveningPlan", evening);
        groupDoc.put("createdAt", System.currentTimeMillis());
        // --- MODIFICATION START ---
        // Add the message to the data map to be saved in Firestore
        groupDoc.put("quickMessage", quickMessage);
        // --- MODIFICATION END ---
        // Add required default fields
        groupDoc.put("deliveryStatus", "not delivered");

        // Create one document per date in the selected range under orderGroups
        WriteBatch batch = db.batch();
        Calendar iter = (Calendar) startCal.clone();
        String groupId = idFmt.format(startCal.getTime());
        long rangeStart = startCal.getTimeInMillis();
        long rangeEnd = endCal.getTimeInMillis();
        while (!iter.after(endCal)) {
            String docId = idFmt.format(iter.getTime());
            Map<String, Object> dayDoc = new HashMap<>(groupDoc);
            long dayMillis = iter.getTimeInMillis();
            // Store only date-level info on the parent.
            dayDoc.put("date", dayMillis);
            dayDoc.put("startDate", dayMillis);
            dayDoc.put("endDate", dayMillis);
            dayDoc.put("groupId", groupId);
            dayDoc.put("rangeStart", rangeStart);
            dayDoc.put("rangeEnd", rangeEnd);

            // Remove shift-specific fields from parent document
            dayDoc.remove("morningPlan");
            dayDoc.remove("eveningPlan");
            dayDoc.remove("quickMessage");
            dayDoc.remove("deliveryStatus");
            dayDoc.remove("payment");

            DocumentReference parentRef = db.collection("users").document(phoneNumber)
                    .collection("orderGroups").document(docId);
            batch.set(parentRef, dayDoc);

            // Subcollection: morning/details if required
            if (!"No Milk".equalsIgnoreCase(morning)) {
                Map<String, Object> morningDoc = new HashMap<>();
                morningDoc.put("milkPlan", morning);
                morningDoc.put("quickMessage", quickMessage);
                morningDoc.put("deliveryStatus", "not delivered");
                morningDoc.put("date", dayMillis);
                DocumentReference mRef = parentRef.collection("morning").document("details");
                batch.set(mRef, morningDoc);
            }

            // Subcollection: evening/details if required
            if (!"No Milk".equalsIgnoreCase(evening)) {
                Map<String, Object> eveningDoc = new HashMap<>();
                eveningDoc.put("milkPlan", evening);
                eveningDoc.put("quickMessage", quickMessage);
                eveningDoc.put("deliveryStatus", "not delivered");
                eveningDoc.put("date", dayMillis);
                DocumentReference eRef = parentRef.collection("evening").document("details");
                batch.set(eRef, eveningDoc);
            }

            iter.add(Calendar.DAY_OF_MONTH, 1);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Order groups saved", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager() != null) {
                        getParentFragmentManager().setFragmentResult("orders_updated", new Bundle());
                    }
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}