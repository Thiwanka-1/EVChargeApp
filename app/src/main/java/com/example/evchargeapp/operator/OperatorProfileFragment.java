package com.example.evchargeapp.operator;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.evchargeapp.auth.LoginActivity;
import com.example.evchargeapp.utils.SessionManager;

public class OperatorProfileFragment extends Fragment {
    @Nullable @Override public View onCreateView(LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,24);
        Button logout = new Button(getContext());
        logout.setText("Logout");
        logout.setOnClickListener(v -> {
            SessionManager.clear(requireContext());
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });
        root.addView(logout);
        return root;
    }
}
