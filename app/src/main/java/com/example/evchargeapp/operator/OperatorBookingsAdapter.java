package com.example.evchargeapp.operator;

import android.view.*;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evchargeapp.R;
import com.example.evchargeapp.models.BookingDto;

import java.text.SimpleDateFormat;
import java.util.*;

class OperatorBookingsAdapter extends RecyclerView.Adapter<OperatorBookingsAdapter.VH> {

    interface RowAction {
        void onDetails(BookingDto b);
        void onApprove(BookingDto b);
        void onReject(BookingDto b);
        void onStart(BookingDto b);
        void onComplete(BookingDto b);
    }

    private final List<BookingDto> items = new ArrayList<>();
    private final RowAction action;

    OperatorBookingsAdapter(RowAction a){ this.action = a; }

    void setItems(List<BookingDto> data){
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_operator_booking, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        BookingDto b = items.get(pos);
        String title = (b.__stationName!=null? b.__stationName : b.stationId) + " (" + b.stationId + ")";
        h.tvTitle.setText(title);

        String when = fmt(b.startTimeUtc) + " → " + fmt(b.endTimeUtc);
        h.tvSub.setText(b.ownerNic + " • " + when);

        String st = (b.status==null?"":b.status);
        h.tvStatus.setText(st);
        h.tvStatus.setBackgroundResource(statusBg(st));
        h.tvStatus.setTextColor(h.tvStatus.getResources().getColor(statusFg(st)));

        // Primary action depends on status
        h.btnPrimary.setVisibility(View.VISIBLE);
        if ("Pending".equalsIgnoreCase(st)) {
            h.btnPrimary.setText("Approve");
            h.btnPrimary.setOnClickListener(v -> action.onApprove(b));
        } else if ("Approved".equalsIgnoreCase(st)) {
            h.btnPrimary.setText("Start");
            h.btnPrimary.setOnClickListener(v -> action.onStart(b));
        } else if ("InProgress".equalsIgnoreCase(st)) {
            h.btnPrimary.setText("Complete");
            h.btnPrimary.setOnClickListener(v -> action.onComplete(b));
        } else {
            h.btnPrimary.setVisibility(View.GONE);
        }

        h.btnMore.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), h.btnMore);
            pm.inflate(R.menu.menu_op_booking_row);
            pm.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.mnu_details) action.onDetails(b);
                else if (id == R.id.mnu_reject) action.onReject(b);
                return true;
            });
            pm.show();
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvStatus;
        TextView btnPrimary;
        ImageButton btnMore;
        VH(@NonNull View v){
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSub   = v.findViewById(R.id.tvSub);
            tvStatus= v.findViewById(R.id.tvStatus);
            btnPrimary = v.findViewById(R.id.btnPrimary);
            btnMore = v.findViewById(R.id.btnMore);
        }
    }

    private static String fmt(String iso){
        if (iso == null) return "-";
        String[] p = new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm'Z'"};
        for (String f : p){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                if (d!=null){
                    SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    return out.format(d);
                }
            } catch (Exception ignored){}
        }
        return "-";
    }

    private static int statusBg(String st){
        if ("Approved".equalsIgnoreCase(st)) return R.drawable.bg_chip_green;
        if ("Pending".equalsIgnoreCase(st)) return R.drawable.bg_chip_yellow;
        if ("InProgress".equalsIgnoreCase(st)) return R.drawable.bg_chip_blue;
        if ("Rejected".equalsIgnoreCase(st)) return R.drawable.bg_chip_red;
        if ("Cancelled".equalsIgnoreCase(st)) return R.drawable.bg_chip_gray;
        if ("Completed".equalsIgnoreCase(st)) return R.drawable.bg_chip_gray;
        return R.drawable.bg_chip_gray;
    }
    private static int statusFg(String st){
        if ("Approved".equalsIgnoreCase(st)) return android.R.color.holo_green_dark;
        if ("Pending".equalsIgnoreCase(st)) return android.R.color.holo_orange_dark;
        if ("InProgress".equalsIgnoreCase(st)) return android.R.color.holo_blue_dark;
        if ("Rejected".equalsIgnoreCase(st)) return android.R.color.holo_red_dark;
        return android.R.color.darker_gray;
    }
}
