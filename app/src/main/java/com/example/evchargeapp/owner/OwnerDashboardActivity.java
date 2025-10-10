package com.example.evchargeapp.owner;

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

public class OwnerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_owner_dashboard);

        // Top app bar (with Logout)
        MaterialToolbar toolbar = findViewById(R.id.toolbar_owner);
        toolbar.setTitle(getString(R.string.owner_dashboard_title));
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuClick);
        toolbar.inflateMenu(R.menu.menu_top_actions);

        // Bottom navigation
        BottomNavigationView nav = findViewById(R.id.owner_bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_owner_bookings) f = new OwnerBookingsFragment();
            else if (id == R.id.nav_owner_availability) f = new OwnerAvailabilityFragment();
            else if (id == R.id.nav_owner_profile) f = new OwnerProfileFragment();
            else f = new OwnerHomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.owner_container, f)
                    .commit();
            return true;
        });
        nav.setSelectedItemId(R.id.nav_owner_home); // default tab
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
