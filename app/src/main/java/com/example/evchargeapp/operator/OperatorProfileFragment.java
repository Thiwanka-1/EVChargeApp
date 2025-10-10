package com.example.evchargeapp.operator;

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
import com.example.evchargeapp.api.UsersService;
import com.example.evchargeapp.models.UserDto;
import com.example.evchargeapp.utils.JwtUtil;
import com.example.evchargeapp.utils.SessionManager;
import com.example.evchargeapp.utils.OperatorLocalStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorProfileFragment extends Fragment {

    private TextView tvUserId, tvRole, tvActive;
    private EditText etUsername, etNewPassword, etConfirm;
    private Button btnSave, btnDeactivate;

    private UsersService usersApi;
    private @Nullable UserDto me;
    private @Nullable String myId;

    // local cache
    private OperatorLocalStore local;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_operator_profile, c, false);

        tvUserId     = v.findViewById(R.id.tvUserId);
        tvRole       = v.findViewById(R.id.tvRole);
        tvActive     = v.findViewById(R.id.tvActive);
        etUsername   = v.findViewById(R.id.etUsername);
        etNewPassword= v.findViewById(R.id.etNewPassword);
        etConfirm    = v.findViewById(R.id.etConfirm);
        btnSave      = v.findViewById(R.id.btnSave);
        btnDeactivate= v.findViewById(R.id.btnDeactivate);

        usersApi = ApiClient.get(requireContext(), getString(R.string.base_url)).create(UsersService.class);
        local = new OperatorLocalStore(requireContext());

        btnSave.setOnClickListener(v1 -> save());
        btnDeactivate.setOnClickListener(v12 -> confirmDeactivate());

        // 1) read token from SessionManager, decode subject
        String token = SessionManager.getToken(requireContext());
        myId = JwtUtil.getSubject(token);

        // show locally cached info immediately
        bindLocal();

        // then refresh from server
        if (!TextUtils.isEmpty(myId)) fetch();
        else Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show();

        return v;
    }

    private void bindLocal() {
        OperatorLocalStore.Operator u = local.get();
        if (u == null) return;
        tvUserId.setText(s(u.id));
        tvRole.setText(s(u.role));
        tvActive.setText(u.isActive ? getString(R.string.active_yes) : getString(R.string.active_no));
        etUsername.setText(s(u.username));
        btnDeactivate.setEnabled(u.isActive);
        btnDeactivate.setAlpha(u.isActive ? 1f : 0.5f);
    }

    private void fetch() {
        usersApi.getById(myId).enqueue(new Callback<UserDto>() {
            @Override public void onResponse(Call<UserDto> call, Response<UserDto> res) {
                if (!isAdded()) return;
                if (res.isSuccessful() && res.body()!=null) {
                    me = res.body();
                    // cache locally
                    local.upsert(me.id, me.username, me.role, me.isActive);
                    // bind UI
                    tvUserId.setText(s(me.id));
                    tvRole.setText(s(me.role));
                    tvActive.setText(me.isActive ? getString(R.string.active_yes) : getString(R.string.active_no));
                    etUsername.setText(s(me.username));
                    btnDeactivate.setEnabled(me.isActive);
                    btnDeactivate.setAlpha(me.isActive ? 1f : 0.5f);
                } else {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<UserDto> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void save() {
        if (me == null) return;

        String username = etUsername.getText().toString().trim();
        String pwd1 = etNewPassword.getText().toString();
        String pwd2 = etConfirm.getText().toString();

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Username is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pwd1.isEmpty() || !pwd2.isEmpty()) {
            if (!pwd1.equals(pwd2)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        UsersService.UserUpdate body = new UsersService.UserUpdate();
        body.id = me.id;
        body.username = username;
        body.passwordHash = pwd1; // server hashes if non-empty
        body.role = me.role;
        body.isActive = me.isActive;

        btnSave.setEnabled(false);
        usersApi.update(me.id, body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                btnSave.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    // update cache
                    local.upsert(me.id, username, me.role, me.isActive);
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    etNewPassword.setText("");
                    etConfirm.setText("");
                    fetch();
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
        // prefer fresh state
        boolean active = (me != null ? me.isActive : (local.get()!=null && local.get().isActive));
        if (!active) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate account?")
                .setMessage("You won’t be able to operate stations until back-office reactivates your account.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Deactivate", (d,w) -> setActive(false))
                .show();
    }

    private void setActive(boolean active) {
        if (me == null) return;
        btnDeactivate.setEnabled(false);
        usersApi.setActive(me.id, active).enqueue(new Callback<java.util.Map<String,String>>() {
            @Override public void onResponse(Call<java.util.Map<String,String>> call, Response<java.util.Map<String,String>> res) {
                btnDeactivate.setEnabled(true);
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    // update local cache
                    local.upsert(me.id, me.username, me.role, active);
                    Toast.makeText(requireContext(), active ? "Reactivated" : "Deactivated", Toast.LENGTH_LONG).show();
                    fetch();
                    // Optionally force logout after deactivation:
                    // if (!active) { SessionManager.clear(requireContext()); ... }
                } else {
                    Toast.makeText(requireContext(), "Action failed", Toast.LENGTH_SHORT).show();
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
