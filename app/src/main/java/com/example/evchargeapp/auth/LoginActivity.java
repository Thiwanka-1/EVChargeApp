package com.example.evchargeapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.AuthService;
import com.example.evchargeapp.models.*;
import com.example.evchargeapp.operator.OperatorDashboardActivity;
import com.example.evchargeapp.owner.OwnerDashboardActivity;
import com.example.evchargeapp.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText etUser, etPass;
    private Button btnLogin;
    private TextView tvToReg;
    private AuthService auth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        tvToReg = findViewById(R.id.tvToRegister);

        String baseUrl = getString(R.string.base_url);
        auth = ApiClient.get(this, baseUrl).create(AuthService.class);

        btnLogin.setOnClickListener(v -> doLogin());
        tvToReg.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String u = etUser.getText().toString().trim();
        String p = etPass.getText().toString().trim();
        if (u.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show(); return;
        }

        boolean isOwner = u.contains("@");
        if (isOwner) {
            auth.ownerLogin(new OwnerLoginRequest(u, p)).enqueue(cb());
        } else {
            auth.systemLogin(new SystemLoginRequest(u, p)).enqueue(cb());
        }
    }

    private Callback<AuthResponse> cb() {
        return new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show(); return;
                }
                AuthResponse a = res.body();
                SessionManager.saveToken(LoginActivity.this, a.accessToken);
                SessionManager.saveRole(LoginActivity.this, a.role);

                if ("Owner".equalsIgnoreCase(a.role)) {
                    startActivity(new Intent(LoginActivity.this, OwnerDashboardActivity.class));
                } else {
                    startActivity(new Intent(LoginActivity.this, OperatorDashboardActivity.class));
                }
                finish();
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        };
    }
}
