package com.example.milk_app;

public class Order {
    private String id; // Firestore document id (not stored in doc fields)
    private String phoneNumber;
    private long date; // epoch millis for the day (morning/evening together or per slot)
    private String morningPlan; // e.g., "500ml" or "No Milk"
    private String eveningPlan; // e.g., "1L" or "No Milk"

    public Order() {}

    public Order(String phoneNumber, long date, String morningPlan, String eveningPlan) {
        this.phoneNumber = phoneNumber;
        this.date = date;
        this.morningPlan = morningPlan;
        this.eveningPlan = eveningPlan;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }
    public String getMorningPlan() { return morningPlan; }
    public void setMorningPlan(String morningPlan) { this.morningPlan = morningPlan; }
    public String getEveningPlan() { return eveningPlan; }
    public void setEveningPlan(String eveningPlan) { this.eveningPlan = eveningPlan; }
}



