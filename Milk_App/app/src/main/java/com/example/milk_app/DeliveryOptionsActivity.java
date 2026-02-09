package com.example.milk_app;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;

public class DeliveryOptionsActivity extends AppCompatActivity {

    private Button morningBatchButton;
    private Button eveningBatchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_options);

        morningBatchButton = findViewById(R.id.morningBatchButton);
        eveningBatchButton = findViewById(R.id.eveningBatchButton);

        // In DeliveryOptionsActivity.java, update the on-click listeners:
        morningBatchButton.setOnClickListener(v -> {
            Intent intent = new Intent(DeliveryOptionsActivity.this, CustomerDeliveryListActivity.class);
            intent.putExtra("batch", "morning");
            startActivity(intent);
        });

        eveningBatchButton.setOnClickListener(v -> {
            Intent intent = new Intent(DeliveryOptionsActivity.this, CustomerDeliveryListActivity.class);
            intent.putExtra("batch", "evening");
            startActivity(intent);
        });
    }
}