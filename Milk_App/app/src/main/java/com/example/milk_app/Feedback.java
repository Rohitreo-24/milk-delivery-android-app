package com.example.milk_app;

public class Feedback {
    private String feedbackText;
    private float rating;
    // You can add other fields like imageUrl, timestamp, etc.

    public Feedback(String feedbackText, float rating) {
        this.feedbackText = feedbackText;
        this.rating = rating;
    }

    // Getters
    public String getFeedbackText() { return feedbackText; }
    public float getRating() { return rating; }
}