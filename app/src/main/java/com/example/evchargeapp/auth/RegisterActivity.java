package com.example.evchargeapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.AuthService;
import com.example.evchargeapp.models.AuthResponse;
import com.example.evchargeapp.models.OwnerRegisterRequest;
import com.example.evchargeapp.owner.OwnerDashboardActivity;
import com.example.evchargeapp.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText etNic, etFirst, etLast, etEmail, etPhone, etPwd;
    private Button btnCreate;
    private TextView tvGoLogin;
    private AuthService auth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);

        etNic = findViewById(R.id.etNic);
        etFirst = findViewById(R.id.etFirst);
        etLast = findViewById(R.id.etLast);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPwd = findViewById(R.id.etPwd);
        btnCreate = findViewById(R.id.btnCreate);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        auth = ApiClient.get(this, getString(R.string.base_url)).create(AuthService.class);

        btnCreate.setOnClickListener(v -> doCreate());
        tvGoLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private void doCreate() {
        OwnerRegisterRequest r = new OwnerRegisterRequest();
        r.nic = etNic.getText().toString().trim();
        r.firstName = etFirst.getText().toString().trim();
        r.lastName = etLast.getText().toString().trim();
        r.email = etEmail.getText().toString().trim();
        r.phone = etPhone.getText().toString().trim();
        r.password = etPwd.getText().toString();

        if (r.nic.isEmpty() || r.email.isEmpty() || r.password.isEmpty()) {
            Toast.makeText(this, "NIC, email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.ownerRegister(r).enqueue(new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                AuthResponse a = res.body();
                SessionManager.saveToken(RegisterActivity.this, a.accessToken);
                SessionManager.saveRole(RegisterActivity.this, a.role);
                startActivity(new Intent(RegisterActivity.this, OwnerDashboardActivity.class));
                finish();
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
