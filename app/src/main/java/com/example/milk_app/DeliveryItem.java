package com.example.milk_app;

public class DeliveryItem {
    private String customerName;
    private String customerPhone;
    private String customerLocation;
    private String milkPlan;
    private boolean isDelivered;
    private String quickMessage;

    // --- ADD THESE ---
    private double latitude;
    private double longitude;
    // -----------------

    public DeliveryItem(String customerName, String customerPhone, String customerLocation, String milkPlan) {
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerLocation = customerLocation;
        this.milkPlan = milkPlan;
        this.isDelivered = false; // Default status
        this.quickMessage = null;

        // --- ADD THESE ---
        this.latitude = 0.0;  // Default to 0
        this.longitude = 0.0; // Default to 0
        // -----------------
    }

    // --- Existing Getters and Setters ---
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerLocation() { return customerLocation; }
    public String getMilkPlan() { return milkPlan; }
    public boolean isDelivered() { return isDelivered; }
    public void setDelivered(boolean delivered) { isDelivered = delivered; }

    public String getQuickMessage() { return quickMessage; }
    public void setQuickMessage(String quickMessage) { this.quickMessage = quickMessage; }

    // --- ADD THESE NEW METHODS ---
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    // -----------------------------
}
