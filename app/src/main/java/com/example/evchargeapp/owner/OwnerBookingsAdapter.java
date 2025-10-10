package com.example.evchargeapp.owner;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evchargeapp.R;
import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.models.StationDto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import androidx.annotation.Nullable;

class OwnerBookingsAdapter extends RecyclerView.Adapter<OwnerBookingsAdapter.VH> {

    interface RowAction {
        void onDetails(BookingDto b);
        void onEdit(BookingDto b);
        void onCancel(BookingDto b);
        void onQr(BookingDto b);
        void onNavigate(BookingDto b, @Nullable StationDto st);   // NEW
    }

    private final List<BookingDto> data = new ArrayList<>();
    private final RowAction action;

    // stationId -> StationDto (set from fragment once stations are loaded)
    private Map<String, StationDto> stationMap = new HashMap<>();

    OwnerBookingsAdapter(RowAction action) {
        this.action = action;
    }

    /** Set/refresh the booking list. */
    void setItems(List<BookingDto> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    /** Provide/refresh the station lookup used for titles and locations. */
    void setStationsMap(Map<String, StationDto> stations) {
        this.stationMap = (stations == null) ? new HashMap<>() : stations;
        notifyDataSetChanged(); // update visible rows with names/addresses
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_booking, parent, false);
        return new VH(v);
    }



    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        BookingDto b = data.get(position);

        String stationId = (b.stationId == null ? "-" : b.stationId);
        StationDto st = stationMap.get(stationId);

        String stationName = (st != null && st.name != null && !st.name.trim().isEmpty())
                ? st.name : stationId;
        String status = (b.status == null ? "-" : b.status);
        h.tvTitle.setText(stationName + " • " + status);

        h.tvWhen.setText(formatRange(b.startTimeUtc, b.endTimeUtc));

        String locText = (st != null && st.address != null && !st.address.trim().isEmpty())
                ? st.address
                : (st != null ? String.format(Locale.getDefault(), "%.5f, %.5f", st.latitude, st.longitude) : "-");
        h.tvLoc.setText(locText);

        h.btnMore.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), h.btnMore);
            pm.inflate(R.menu.menu_booking_row);
            pm.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.mnu_details) action.onDetails(b);
                else if (id == R.id.mnu_edit) action.onEdit(b);
                else if (id == R.id.mnu_cancel) action.onCancel(b);
                else if (id == R.id.mnu_qr) action.onQr(b);
                else if (id == R.id.mnu_nav) action.onNavigate(b, st);   // NEW
                return true;
            });
            pm.show();
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvWhen, tvLoc;
        ImageButton btnMore;

        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvWhen  = v.findViewById(R.id.tvWhen);
            tvLoc   = v.findViewById(R.id.tvLoc);
            btnMore = v.findViewById(R.id.btnMore);
        }
    }

    // ---- time helpers (UTC ISO -> local pretty) ----
    private static String formatRange(String isoStart, String isoEnd) {
        Long s = parseUtc(isoStart);
        Long e = parseUtc(isoEnd);
        if (s == null || e == null) return "-";
        SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return d.format(new Date(s)) + " → " + d.format(new Date(e));
    }

    private static Long parseUtc(String iso) {
        if (iso == null) return null;
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm'Z'"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                if (d != null) return d.getTime();
            } catch (Exception ignored) { }
        }
        return null;
    }
}
