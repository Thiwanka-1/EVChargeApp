package com.example.evchargeapp.owner;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OwnerBookingsFragment extends Fragment {
    @Nullable @Override public View onCreateView(LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        TextView tv = new TextView(getContext());
        tv.setPadding(24,24,24,24);
        tv.setText("Bookings list (upcoming & history)");
        return tv;
    }
}
