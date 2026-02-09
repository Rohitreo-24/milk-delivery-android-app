package com.example.milk_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class FeedbackDisplayActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackDisplayActivity";
    private RecyclerView feedbackRecyclerView;
    private FeedbackAdapter feedbackAdapter;
    private ArrayList<Feedback> feedbackList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_display);

        feedbackRecyclerView = findViewById(R.id.feedbackRecyclerView);
        feedbackRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        feedbackList = new ArrayList<>();
        feedbackAdapter = new FeedbackAdapter(feedbackList);
        feedbackRecyclerView.setAdapter(feedbackAdapter);

        db = FirebaseFirestore.getInstance();
        fetchFeedback();
    }

    private void fetchFeedback() {
        db.collection("feedback")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        feedbackList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String feedbackText = document.getString("feedbackText");
                            float rating = document.getDouble("rating").floatValue();

                            // You can also retrieve the image URL if you added that to your feedback collection
                            // String imageUrl = document.getString("imageUrl");

                            Feedback feedback = new Feedback(feedbackText, rating); // Add imageUrl if needed
                            feedbackList.add(feedback);
                        }
                        feedbackAdapter.notifyDataSetChanged();
                        if (feedbackList.isEmpty()) {
                            Toast.makeText(this, "No feedback found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(this, "Failed to load feedback.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}