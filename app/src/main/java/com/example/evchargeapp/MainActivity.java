package com.example.evchargeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evchargeapp.auth.LoginActivity;
import com.example.evchargeapp.operator.OperatorDashboardActivity;
import com.example.evchargeapp.owner.OwnerDashboardActivity;
import com.example.evchargeapp.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);

        // If already logged in, route directly to dashboard
        String token = SessionManager.getToken(this);
        String role  = SessionManager.getRole(this);
        if (token != null && role != null) {
            if ("Owner".equalsIgnoreCase(role)) {
                startActivity(new Intent(this, OwnerDashboardActivity.class));
            } else {
                startActivity(new Intent(this, OperatorDashboardActivity.class));
            }
            finish();
            return;
        }

        // Otherwise show the landing screen
        setContentView(R.layout.activity_main);

        Button getStarted = findViewById(R.id.btnGetStarted);
        Button goRegister = findViewById(R.id.btnGoRegister);

        getStarted.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        goRegister.setOnClickListener(v -> {
            // Optional: go straight to Register (owner)
            startActivity(new Intent(this, com.example.evchargeapp.auth.RegisterActivity.class));
        });
    }
}
