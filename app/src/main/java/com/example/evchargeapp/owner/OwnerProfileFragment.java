package com.example.evchargeapp.owner;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.evchargeapp.utils.OwnerLocalStore;

public class OwnerProfileFragment extends Fragment {
    @Nullable @Override public View onCreateView(LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        OwnerLocalStore store = new OwnerLocalStore(requireContext());
        OwnerLocalStore.Owner o = store.get();
        String text = (o==null) ? "No profile cached." :
                "NIC: " + o.nic + "\nName: " + o.firstName + " " + o.lastName +
                        "\nEmail: " + o.email + "\nPhone: " + o.phone +
                        "\nActive: " + (o.isActive ? "Yes" : "No");
        TextView tv = new TextView(getContext());
        tv.setPadding(24,24,24,24);
        tv.setText(text + "\n\n(Will add edit/deactivate next)");
        return tv;
    }
}
