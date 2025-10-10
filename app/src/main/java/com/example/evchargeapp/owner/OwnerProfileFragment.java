package com.example.evchargeapp.owner;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.EvOwnersService;
import com.example.evchargeapp.models.OwnerDto;
import com.example.evchargeapp.utils.OwnerLocalStore;
import com.example.evchargeapp.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerProfileFragment extends Fragment {

    private TextView tvNic, tvActive;
    private EditText etFirst, etLast, etEmail, etPhone;
    private Button btnSave, btnDeactivate;

    private EvOwnersService ownersApi;
    private OwnerLocalStore local;
    private @Nullable OwnerLocalStore.Owner localOwner;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_owner_profile, c, false);

        tvNic = v.findViewById(R.id.tvNic);
        tvActive = v.findViewById(R.id.tvActive);
        etFirst = v.findViewById(R.id.etFirst);
        etLast  = v.findViewById(R.id.etLast);
        etEmail = v.findViewById(R.id.etEmail);
        etPhone = v.findViewById(R.id.etPhone);
        btnSave = v.findViewById(R.id.btnSave);
        btnDeactivate = v.findViewById(R.id.btnDeactivate);

        ownersApi = ApiClient.get(requireContext(), getString(R.string.base_url)).create(EvOwnersService.class);
        local = new OwnerLocalStore(requireContext());

        btnSave.setOnClickListener(v1 -> save());
        btnDeactivate.setOnClickListener(v12 -> confirmDeactivate());

        bindLocal();
        // also refresh from server (best-effort)
        fetchFromServer();

        return v;
    }

    private void bindLocal() {
        localOwner = local.get();
        if (localOwner == null) {
            // fallback UI
            tvNic.setText("-");
            tvActive.setText(getString(R.string.na));
            return;
        }
        tvNic.setText(localOwner.nic);
        tvActive.setText(localOwner.isActive ? getString(R.string.active_yes) : getString(R.string.active_no));
        etFirst.setText(s(localOwner.firstName));
        etLast.setText(s(localOwner.lastName));
        etEmail.setText(s(localOwner.email));
        etPhone.setText(s(localOwner.phone));

        // visual state
        btnDeactivate.setEnabled(localOwner.isActive);
        btnDeactivate.setAlpha(localOwner.isActive ? 1f : 0.5f);
    }

    private void fetchFromServer() {
        if (localOwner == null || TextUtils.isEmpty(localOwner.nic)) return;
        ownersApi.getByNic(localOwner.nic).enqueue(new Callback<OwnerDto>() {
            @Override public void onResponse(Call<OwnerDto> call, Response<OwnerDto> res) {
                if (!isAdded()) return;
                OwnerDto o = res.body();
                if (res.isSuccessful() && o != null) {
                    local.upsert(o.nic, o.firstName, o.lastName, o.email, o.phone, o.isActive);
                    bindLocal();
                }
            }
            @Override public void onFailure(Call<OwnerDto> call, Throwable t) { /* ignore */ }
        });
    }

    private void save() {
        if (localOwner == null) return;

        String first = etFirst.getText().toString().trim();
        String last  = etLast.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (first.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "First name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        OwnerDto body = new OwnerDto();
        body.nic = localOwner.nic;
        body.firstName = first;
        body.lastName = last;
        body.email = email;
        body.phone = phone;
        body.isActive = localOwner.isActive; // keep current active flag

        btnSave.setEnabled(false);
        ownersApi.update(localOwner.nic, body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                btnSave.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    local.upsert(body.nic, body.firstName, body.lastName, body.email, body.phone, body.isActive);
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    bindLocal();
                } else {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeactivate() {
        if (localOwner == null || !localOwner.isActive) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate account?")
                .setMessage("You won’t be able to create new bookings until the back-office reactivates your account.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Deactivate", (d,w) -> deactivateNow())
                .show();
    }

    private void deactivateNow() {
        if (localOwner == null) return;
        btnDeactivate.setEnabled(false);
        ownersApi.setActive(localOwner.nic, false).enqueue(new Callback<java.util.Map<String,String>>() {
            @Override public void onResponse(Call<java.util.Map<String,String>> call, Response<java.util.Map<String,String>> res) {
                btnDeactivate.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    local.upsert(localOwner.nic, localOwner.firstName, localOwner.lastName, localOwner.email, localOwner.phone, false);
                    Toast.makeText(requireContext(), "Account deactivated", Toast.LENGTH_LONG).show();
                    bindLocal();

                    // optional: clear token & return to login
                    // SessionManager.clear(requireContext());
                    // startActivity(new Intent(requireContext(), com.example.evchargeapp.auth.LoginActivity.class));
                    // requireActivity().finish();
                } else {
                    Toast.makeText(requireContext(), "Deactivation failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<java.util.Map<String,String>> call, Throwable t) {
                btnDeactivate.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String s(String x){ return x==null? "" : x; }
}
