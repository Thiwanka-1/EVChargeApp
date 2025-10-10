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
import com.example.evchargeapp.utils.JwtUtil;
import com.example.evchargeapp.utils.OwnerLocalStore;
import com.example.evchargeapp.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerProfileFragment extends Fragment {

    private TextView tvNic, tvActive;
    private EditText etFirst, etLast, etEmail, etPhone;
    private EditText etCurPw, etNewPw, etNewPw2;
    private Button btnSave, btnDeactivate, btnChangePw, btnDelete;

    private EvOwnersService ownersApi;
    private OwnerLocalStore local;
    private @Nullable OwnerLocalStore.Owner localOwner;
    private @Nullable String currentNic;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_owner_profile, c, false);

        tvNic = v.findViewById(R.id.tvNic);
        tvActive = v.findViewById(R.id.tvActive);
        etFirst = v.findViewById(R.id.etFirst);
        etLast  = v.findViewById(R.id.etLast);
        etEmail = v.findViewById(R.id.etEmail);
        etPhone = v.findViewById(R.id.etPhone);
        etCurPw = v.findViewById(R.id.etCurPw);
        etNewPw = v.findViewById(R.id.etNewPw);
        etNewPw2= v.findViewById(R.id.etNewPw2);
        btnSave = v.findViewById(R.id.btnSave);
        btnDeactivate = v.findViewById(R.id.btnDeactivate);
        btnChangePw   = v.findViewById(R.id.btnChangePw);
        btnDelete     = v.findViewById(R.id.btnDelete);

        String base = getString(R.string.base_url);
        ownersApi = ApiClient.get(requireContext(), base).create(EvOwnersService.class);
        local = new OwnerLocalStore(requireContext());

        // derive NIC from stored JWT
        String raw = SessionManager.getToken(requireContext());
        String token = (raw != null && raw.startsWith("Bearer ")) ? raw.substring(7) : raw;
        currentNic = JwtUtil.getSubject(token);

        btnSave.setOnClickListener(v1 -> save());
        btnDeactivate.setOnClickListener(v12 -> confirmDeactivate());
        btnChangePw.setOnClickListener(v13 -> attemptChangePassword());
        btnDelete.setOnClickListener(v14 -> confirmDelete());

        bindLocal();
        fetchFromServer();

        return v;
    }

    private void bindLocal() {
        if (TextUtils.isEmpty(currentNic)) {
            tvNic.setText("-");
            tvActive.setText(getString(R.string.na));
            btnSave.setEnabled(false);
            btnDeactivate.setEnabled(false);
            btnChangePw.setEnabled(false);
            btnDelete.setEnabled(false);
            return;
        }

        localOwner = local.getByNic(currentNic);
        if (localOwner == null) {
            tvNic.setText(currentNic);
            tvActive.setText(getString(R.string.na));
            etFirst.setText(""); etLast.setText(""); etEmail.setText(""); etPhone.setText("");
            btnDeactivate.setEnabled(false); btnDeactivate.setAlpha(0.5f);
            return;
        }

        tvNic.setText(localOwner.nic);
        tvActive.setText(localOwner.isActive ? getString(R.string.active_yes) : getString(R.string.active_no));
        etFirst.setText(s(localOwner.firstName));
        etLast.setText(s(localOwner.lastName));
        etEmail.setText(s(localOwner.email));
        etPhone.setText(s(localOwner.phone));

        btnDeactivate.setEnabled(localOwner.isActive);
        btnDeactivate.setAlpha(localOwner.isActive ? 1f : 0.5f);
    }

    private void fetchFromServer() {
        if (TextUtils.isEmpty(currentNic)) return;
        ownersApi.getByNic(currentNic).enqueue(new Callback<OwnerDto>() {
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
        if (TextUtils.isEmpty(currentNic)) return;

        String first = etFirst.getText().toString().trim();
        String last  = etLast.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (first.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_required_name_email, Toast.LENGTH_SHORT).show();
            return;
        }

        OwnerDto body = new OwnerDto();
        body.nic = currentNic;
        body.firstName = first;
        body.lastName = last;
        body.email = email;
        body.phone = phone;
        body.isActive = (localOwner != null) && localOwner.isActive;

        btnSave.setEnabled(false);
        ownersApi.update(currentNic, body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                btnSave.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    local.upsert(body.nic, body.firstName, body.lastName, body.email, body.phone, body.isActive);
                    Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                    bindLocal();
                } else {
                    Toast.makeText(requireContext(), R.string.update_failed, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptChangePassword() {
        if (TextUtils.isEmpty(currentNic)) return;
        String cur = etCurPw.getText().toString();
        String n1  = etNewPw.getText().toString();
        String n2  = etNewPw2.getText().toString();

        if (cur.trim().isEmpty() || n1.trim().isEmpty() || n2.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.err_pw_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!n1.equals(n2)) {
            Toast.makeText(requireContext(), R.string.err_pw_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }
        if (n1.length() < 6) {
            Toast.makeText(requireContext(), R.string.err_pw_length, Toast.LENGTH_SHORT).show();
            return;
        }

        EvOwnersService.ChangePasswordRequest body = new EvOwnersService.ChangePasswordRequest();
        body.currentPassword = cur;
        body.newPassword = n1;

        btnChangePw.setEnabled(false);
        ownersApi.changePassword(currentNic, body).enqueue(new Callback<EvOwnersService.MessageResponse>() {
            @Override public void onResponse(Call<EvOwnersService.MessageResponse> call, Response<EvOwnersService.MessageResponse> res) {
                btnChangePw.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    etCurPw.setText(""); etNewPw.setText(""); etNewPw2.setText("");
                    Toast.makeText(requireContext(), R.string.pw_changed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.pw_change_failed, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<EvOwnersService.MessageResponse> call, Throwable t) {
                btnChangePw.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeactivate() {
        if (localOwner == null || !localOwner.isActive) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.deactivate_q)
                .setMessage(R.string.deactivate_note)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.deactivate, (d,w) -> deactivateNow())
                .show();
    }

    private void deactivateNow() {
        if (TextUtils.isEmpty(currentNic)) return;
        btnDeactivate.setEnabled(false);
        ownersApi.setActive(currentNic, false).enqueue(new Callback<java.util.Map<String,String>>() {
            @Override public void onResponse(Call<java.util.Map<String,String>> call, Response<java.util.Map<String,String>> res) {
                btnDeactivate.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    OwnerLocalStore.Owner o = (localOwner != null) ? localOwner : new OwnerLocalStore.Owner();
                    String nic = (o.nic != null) ? o.nic : currentNic;
                    local.upsert(nic, s(o.firstName), s(o.lastName), s(o.email), s(o.phone), false);
                    Toast.makeText(requireContext(), R.string.deactivated, Toast.LENGTH_LONG).show();
                    bindLocal();
                } else {
                    Toast.makeText(requireContext(), R.string.deactivation_failed, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<java.util.Map<String,String>> call, Throwable t) {
                btnDeactivate.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        if (TextUtils.isEmpty(currentNic)) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_note)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_account, (d,w) -> deleteNow())
                .show();
    }

    private void deleteNow() {
        if (TextUtils.isEmpty(currentNic)) return;
        btnDelete.setEnabled(false);
        ownersApi.delete(currentNic).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                btnDelete.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    // wipe local
                    local.clear();
                    SessionManager.clear(requireContext());
                    Toast.makeText(requireContext(), R.string.account_deleted, Toast.LENGTH_LONG).show();

                    // TODO: navigate to login screen in your app
                    // startActivity(new Intent(requireContext(), LoginActivity.class));
                    // requireActivity().finish();

                } else {
                    Toast.makeText(requireContext(), R.string.delete_failed, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                btnDelete.setEnabled(true);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String s(String x){ return x==null? "" : x; }
}
