package com.example.milk_app;

public class OrderGroup {
    private String id;
    private String phoneNumber;
    private long startDate;
    private long endDate;
    private String morningPlan;
    private String eveningPlan;
    private long createdAt;
    private String quickMessage;
    private String deliveryStatus;
    private String payment;
    private java.util.List<Long> dates;

    public OrderGroup() {
        // Firestore requires empty constructor
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getMorningPlan() { return morningPlan; }
    public void setMorningPlan(String morningPlan) { this.morningPlan = morningPlan; }

    public String getEveningPlan() { return eveningPlan; }
    public void setEveningPlan(String eveningPlan) { this.eveningPlan = eveningPlan; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getQuickMessage() { return quickMessage; }
    public void setQuickMessage(String quickMessage) { this.quickMessage = quickMessage; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) { this.payment = payment; }

    public java.util.List<Long> getDates() { return dates; }
    public void setDates(java.util.List<Long> dates) { this.dates = dates; }
}
