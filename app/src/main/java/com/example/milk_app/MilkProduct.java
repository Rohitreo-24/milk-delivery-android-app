package com.example.milk_app;

public class MilkProduct {
    private String name;
    private String details;
    private int price;
    private String modelUrl;
    private int imageResId; // 🆕 Added field

    public MilkProduct(String name, String details, int price, String modelUrl, int imageResId) {
        this.name = name;
        this.details = details;
        this.price = price;
        this.modelUrl = modelUrl;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public String getDetails() { return details; }
    public int getPrice() { return price; }
    public String getModelUrl() { return modelUrl; }
    public int getImageResId() { return imageResId; } // 🆕 Getter
}
