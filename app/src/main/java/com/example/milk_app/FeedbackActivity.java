package com.example.milk_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FeedbackActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText feedbackEditText;
    private RatingBar ratingBar;
    private Button addImageButton;
    private Button submitButton;
    private ImageView attachedImage;
    private Uri attachedImageUri;

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize views
        feedbackEditText = findViewById(R.id.feedbackEditText);
        ratingBar = findViewById(R.id.ratingBar);
        addImageButton = findViewById(R.id.addImageButton);
        submitButton = findViewById(R.id.submitButton);
        attachedImage = findViewById(R.id.attachedImage);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Set up click listeners
        addImageButton.setOnClickListener(v -> showImagePickerDialog());
        submitButton.setOnClickListener(v -> submitFeedback());
    }

    private void showImagePickerDialog() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent takeImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Intent chooserIntent = Intent.createChooser(pickImageIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takeImageIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (data.getData() != null) {
                // Image from gallery
                attachedImageUri = data.getData();
            } else {
                // Image from camera (returned as a Bitmap in extras)
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                if (imageBitmap != null) {
                    // Save the camera bitmap to a file to get a URI
                    attachedImageUri = getImageUriFromBitmap(imageBitmap);
                }
            }

            if (attachedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), attachedImageUri);
                    attachedImage.setImageBitmap(bitmap);
                    attachedImage.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Helper method to convert a Bitmap to a URI
    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "FeedbackImage", null);
        return Uri.parse(path);
    }


    private void submitFeedback() {
        String feedbackText = feedbackEditText.getText().toString();
        float rating = ratingBar.getRating();

        if (feedbackText.isEmpty() && rating == 0) {
            Toast.makeText(this, "Please provide feedback or a rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        submitButton.setEnabled(false); // Disable button to prevent multiple clicks

        if (attachedImageUri != null) {
            uploadImageAndSubmitFeedback(feedbackText, rating);
        } else {
            saveFeedback(feedbackText, rating, null);
        }
    }

    private void uploadImageAndSubmitFeedback(String feedbackText, float rating) {
        StorageReference storageRef = storage.getReference().child("feedback_images/" + UUID.randomUUID().toString());

        storageRef.putFile(attachedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveFeedback(feedbackText, rating, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                });
    }

    private void saveFeedback(String feedbackText, float rating, String imageUrl) {
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("feedbackText", feedbackText);
        feedback.put("rating", rating);
        feedback.put("timestamp", System.currentTimeMillis());
        if (imageUrl != null) {
            feedback.put("imageUrl", imageUrl);
        }

        db.collection("feedback")
                .add(feedback)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(FeedbackActivity.this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
                    resetForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FeedbackActivity.this, "Failed to submit feedback.", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                });
    }

    private void resetForm() {
        feedbackEditText.setText("");
        ratingBar.setRating(0);
        attachedImage.setVisibility(View.GONE);
        attachedImage.setImageDrawable(null);
        attachedImageUri = null;
        submitButton.setEnabled(true);
    }
}