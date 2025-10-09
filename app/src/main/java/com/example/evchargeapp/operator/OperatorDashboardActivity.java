package com.example.evchargeapp.operator;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evchargeapp.R;
import com.example.evchargeapp.auth.LoginActivity;
import com.example.evchargeapp.utils.SessionManager;

public class OperatorDashboardActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_operator_dashboard);
        Button logout = findViewById(R.id.btnLogoutOp);
        logout.setOnClickListener(v -> {
            SessionManager.clear(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
