package com.example.evchargeapp.operator;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OperatorHomeFragment extends Fragment {
    @Nullable @Override public View onCreateView(LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        TextView tv = new TextView(getContext());
        tv.setPadding(24,24,24,24);
        tv.setText("Operator Home\n(quick scan entry)");
        return tv;
    }
}
