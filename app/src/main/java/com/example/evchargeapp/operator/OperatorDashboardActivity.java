package com.example.evchargeapp.operator;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.evchargeapp.R;
import com.example.evchargeapp.auth.LoginActivity;
import com.example.evchargeapp.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.content.Intent;
import android.view.MenuItem;

public class OperatorDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_operator_dashboard);

        // Top app bar (with Logout)
        MaterialToolbar toolbar = findViewById(R.id.toolbar_operator);
        toolbar.setTitle(getString(R.string.operator_dashboard_title));
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuClick);
        toolbar.inflateMenu(R.menu.menu_top_actions);

        // Bottom navigation
        BottomNavigationView nav = findViewById(R.id.op_bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_op_scan) f = new OperatorScanFragment();
            else if (id == R.id.nav_op_profile) f = new OperatorProfileFragment();
            else f = new OperatorHomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.operator_container, f)
                    .commit();
            return true;
        });
        nav.setSelectedItemId(R.id.nav_op_home); // default tab
    }

    private boolean onToolbarMenuClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            SessionManager.clear(this);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
