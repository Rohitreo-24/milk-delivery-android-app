// File: Customer.java
package com.example.milk_app;

public class Customer {
    private String name;
    private String phoneNumber;
    private String location;
    private String morningPlan;
    private String eveningPlan;

    public Customer() {
        // Required empty constructor for Firestore
    }

    public Customer(String name, String phoneNumber, String location, String morningPlan, String eveningPlan) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.morningPlan = morningPlan;
        this.eveningPlan = eveningPlan;
    }

    // Getters for all fields
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getLocation() { return location; }
    public String getMorningPlan() { return morningPlan; }
    public String getEveningPlan() { return eveningPlan; }

    // Setters for all fields (optional but good practice)
    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setLocation(String location) { this.location = location; }
    public void setMorningPlan(String morningPlan) { this.morningPlan = morningPlan; }
    public void setEveningPlan(String eveningPlan) { this.eveningPlan = eveningPlan; }
}