package com.example.milk_app;

public class DailyRecord {
    private String date;
    private String batch;
    private String status;
    private double amount;

    public DailyRecord(String date, String batch, String status, double amount) {
        this.date = date;
        this.batch = batch;
        this.status = status;
        this.amount = amount;
    }

    public String getDate() { return date; }
    public String getBatch() { return batch; }
    public String getStatus() { return status; }
    public double getAmount() { return amount; }
}