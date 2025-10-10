package com.example.evchargeapp.owner;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evchargeapp.R;
import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.utils.TimeUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.VH> {

    public interface Listener {
        void onDetails(BookingDto b);
        void onEdit(BookingDto b);
        void onCancel(BookingDto b);
    }

    private final Context ctx;
    private final Listener listener;
    private final List<BookingDto> data = new ArrayList<>();

    public BookingAdapter(Context ctx, Listener l){
        this.ctx = ctx;
        this.listener = l;
    }

    public void submit(List<BookingDto> items){
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_booking, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        BookingDto b = data.get(i);
        String title = (b.stationId == null ? "" : b.stationId);
        h.tvTitle.setText(title);

        Long s = TimeUtil.parseIsoToMillis(b.startTimeUtc);
        Long e = TimeUtil.parseIsoToMillis(b.endTimeUtc);
        if (s != null && e != null) {
            h.tvWindow.setText(String.format(Locale.getDefault(), "%s → %s",
                    TimeUtil.fmtLocal(s), TimeUtil.fmtLocal(e)));
        } else {
            h.tvWindow.setText("");
        }

        h.tvStatus.setText(b.status == null ? "" : b.status);
        tintStatus(h.tvStatus, b.status);

        // Buttons visibility depending on status
        boolean canChange = "Pending".equalsIgnoreCase(b.status) || "Approved".equalsIgnoreCase(b.status);
        h.btnEdit.setVisibility(canChange ? View.VISIBLE : View.GONE);
        h.btnCancel.setVisibility(canChange ? View.VISIBLE : View.GONE);

        h.btnDetails.setOnClickListener(v -> listener.onDetails(b));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(b));
        h.btnCancel.setOnClickListener(v -> listener.onCancel(b));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvWindow, tvStatus;
        MaterialButton btnDetails, btnEdit, btnCancel;
        VH(@NonNull View v){
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvWindow = v.findViewById(R.id.tvWindow);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnDetails = v.findViewById(R.id.btnDetails);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnCancel = v.findViewById(R.id.btnCancel);
        }
    }

    private void tintStatus(TextView chip, String s){
        int bg = Color.parseColor("#E5E7EB"); // gray-200
        int fg = Color.parseColor("#374151"); // gray-700
        if (s == null) { chip.setTextColor(fg); chip.setBackgroundColor(bg); return; }

        switch (s.toLowerCase(Locale.US)){
            case "pending":   bg = Color.parseColor("#FEF3C7"); fg = Color.parseColor("#92400E"); break; // amber
            case "approved":  bg = Color.parseColor("#DCFCE7"); fg = Color.parseColor("#166534"); break; // green
            case "inprogress":bg = Color.parseColor("#DBEAFE"); fg = Color.parseColor("#1E40AF"); break; // blue
            case "completed": bg = Color.parseColor("#E5E7EB"); fg = Color.parseColor("#374151"); break; // gray
            case "rejected":
            case "cancelled": bg = Color.parseColor("#FEE2E2"); fg = Color.parseColor("#991B1B"); break; // red
        }
        chip.getBackground().setTint(bg);
        chip.setTextColor(fg);
    }
}
