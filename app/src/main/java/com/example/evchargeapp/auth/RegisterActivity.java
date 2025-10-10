package com.example.evchargeapp.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.AuthService;
import com.example.evchargeapp.api.EvOwnersService;
import com.example.evchargeapp.models.AuthResponse;
import com.example.evchargeapp.models.OwnerDto;
import com.example.evchargeapp.models.OwnerRegisterRequest;
import com.example.evchargeapp.owner.OwnerDashboardActivity;
import com.example.evchargeapp.utils.OwnerLocalStore;
import com.example.evchargeapp.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etNic, etFirst, etLast, etEmail, etPhone, etPwd;
    private TextInputLayout tilNic, tilFirst, tilLast, tilEmail, tilPhone, tilPwd;
    private Button btnCreate;
    private TextView tvGoLogin;
    private AuthService auth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register);

        // Inputs
        etNic   = findViewById(R.id.etNic);
        etFirst = findViewById(R.id.etFirst);
        etLast  = findViewById(R.id.etLast);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPwd   = findViewById(R.id.etPwd);

        // Layouts (for inline error)
        tilNic   = findViewById(R.id.tilNic);
        tilFirst = findViewById(R.id.tilFirst);
        tilLast  = findViewById(R.id.tilLast);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPwd   = findViewById(R.id.tilPwd);

        btnCreate = findViewById(R.id.btnCreate);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        auth = ApiClient.get(this, getString(R.string.base_url)).create(AuthService.class);

        // Clear errors on change
        addClearErrorWatcher(etNic, tilNic);
        addClearErrorWatcher(etFirst, tilFirst);
        addClearErrorWatcher(etLast, tilLast);
        addClearErrorWatcher(etEmail, tilEmail);
        addClearErrorWatcher(etPhone, tilPhone);
        addClearErrorWatcher(etPwd, tilPwd);

        btnCreate.setOnClickListener(v -> {
            if (validateForm()) {
                doCreate();
            }
        });
        tvGoLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

    private boolean validateForm() {
        boolean ok = true;

        String nic   = safe(etNic);
        String first = safe(etFirst);
        String last  = safe(etLast);
        String email = safe(etEmail);
        String phone = safe(etPhone);
        String pwd   = safe(etPwd);

        // NIC required
        if (nic.isEmpty()) {
            tilNic.setError("NIC is required");
            ok = false;
        } else {
            tilNic.setError(null);
        }

        // First name: required, no digits (input already blocks digits, still validate)
        if (first.isEmpty()) {
            tilFirst.setError("First name is required");
            ok = false;
        } else if (!first.matches("^[A-Za-z][A-Za-z '\\-]*$")) {
            tilFirst.setError("Only letters, spaces, ' and - allowed");
            ok = false;
        } else {
            tilFirst.setError(null);
        }

        // Last name: optional? (you didn’t mark required). If you want required, uncomment:
        if (last.isEmpty()) {
            tilLast.setError("Last name is required");
            ok = false;
        } else if (!last.matches("^[A-Za-z][A-Za-z '\\-]*$")) {
            tilLast.setError("Only letters, spaces, ' and - allowed");
            ok = false;
        } else {
            tilLast.setError(null);
        }

        // Email: required + pattern
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            ok = false;
        } else {
            tilEmail.setError(null);
        }

        // Phone:
        // - typing restricted to '+' and digits in XML
        // - maxLength=13 already applied
        // Accept either:
        //   * 10 digits (e.g., 0786543442)
        //   * '+' followed by 11-12 digits (to cover +94XXXXXXXXX patterns)
        boolean phoneOk =
                phone.matches("^\\d{10}$") ||
                        phone.matches("^\\+\\d{11,12}$");

        if (phone.isEmpty()) {
            tilPhone.setError("Phone is required");
            ok = false;
        } else if (!phoneOk) {
            tilPhone.setError("Phone must be 10 digits or + followed by 11–12 digits");
            ok = false;
        } else {
            tilPhone.setError(null);
        }

        // Password: > 8 characters (i.e., min 9)
        if (pwd.isEmpty()) {
            tilPwd.setError("Password is required");
            ok = false;
        } else if (pwd.length() < 9) {
            tilPwd.setError("Password must be at least 9 characters");
            ok = false;
        } else {
            tilPwd.setError(null);
        }

        return ok;
    }

    private String safe(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void addClearErrorWatcher(TextInputEditText et, TextInputLayout til) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (til.getError() != null) til.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void doCreate() {
        OwnerRegisterRequest r = new OwnerRegisterRequest();
        r.nic = safe(etNic);
        r.firstName = safe(etFirst);
        r.lastName = safe(etLast);
        r.email = safe(etEmail);
        r.phone = safe(etPhone);
        r.password = safe(etPwd);

        auth.ownerRegister(r).enqueue(new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthResponse a = res.body();

                // 1) save session
                SessionManager.saveToken(RegisterActivity.this, a.accessToken);
                SessionManager.saveRole(RegisterActivity.this, a.role);

                // 2) fetch + cache owner profile (newly created owner)
                fetchAndCacheOwnerProfile(a.accessToken);

                // 3) go to Owner dashboard
                startActivity(new Intent(RegisterActivity.this, OwnerDashboardActivity.class));
                finish();
            }

            @Override public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAndCacheOwnerProfile(String token){
        String nic = com.example.evchargeapp.utils.JwtUtil.getSubject(token);
        if (nic == null) return;
        EvOwnersService owners = ApiClient.get(this, getString(R.string.base_url)).create(EvOwnersService.class);
        owners.getByNic(nic).enqueue(new retrofit2.Callback<OwnerDto>() {
            @Override public void onResponse(retrofit2.Call<OwnerDto> call, retrofit2.Response<OwnerDto> res) {
                OwnerDto o = res.body();
                if (o != null) {
                    OwnerLocalStore store = new OwnerLocalStore(RegisterActivity.this);
                    store.upsert(o.nic, o.firstName, o.lastName, o.email, o.phone, o.isActive);
                }
            }
            @Override public void onFailure(retrofit2.Call<OwnerDto> call, Throwable t) { /* ignore */ }
        });
    }
}
