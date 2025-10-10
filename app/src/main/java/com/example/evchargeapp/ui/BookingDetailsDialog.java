package com.example.evchargeapp.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.evchargeapp.R;
import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.models.StationDto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class BookingDetailsDialog extends DialogFragment {

    private static final String ARG_STATION_NAME = "stationName";
    private static final String ARG_STATION_ADDR = "stationAddr";
    private static final String ARG_STATUS = "status";
    private static final String ARG_START = "start";
    private static final String ARG_END = "end";
    private static final String ARG_QR = "qr";
    private static final String ARG_REASON = "reason";   // NEW

    public static void show(@NonNull FragmentManager fm,
                            @NonNull BookingDto b,
                            @Nullable StationDto st) {
        Bundle args = new Bundle();
        args.putString(ARG_STATION_NAME, (st != null && st.name != null && !st.name.isEmpty()) ? st.name : b.stationId);
        args.putString(ARG_STATION_ADDR, (st != null) ? st.address : "");
        args.putString(ARG_STATUS, b.status);
        args.putString(ARG_START, b.startTimeUtc);
        args.putString(ARG_END, b.endTimeUtc);
        args.putString(ARG_QR, b.qrCode);
        args.putString(ARG_REASON, b.rejectionReason);    // NEW

        BookingDetailsDialog d = new BookingDetailsDialog();
        d.setArguments(args);
        d.show(fm, "BookingDetailsDialog");
    }

    public static void display(@NonNull FragmentManager fm,
                               @NonNull BookingDto b,
                               @Nullable StationDto st) {
        show(fm, b, st);
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_booking_details, null, false);

        TextView tvStation = v.findViewById(R.id.tvStation);
        TextView tvAddr    = v.findViewById(R.id.tvAddr);
        TextView tvStatus  = v.findViewById(R.id.tvStatus);
        TextView tvWhen    = v.findViewById(R.id.tvWhen);
        TextView tvQr      = v.findViewById(R.id.tvQr);

        // NEW views
        View     dividerRej         = v.findViewById(R.id.dividerRej);
        TextView tvRejectionTitle   = v.findViewById(R.id.tvRejectionTitle);
        TextView tvRejectionReason  = v.findViewById(R.id.tvRejectionReason);

        Bundle a = getArguments();
        String name   = a.getString(ARG_STATION_NAME, "-");
        String addr   = a.getString(ARG_STATION_ADDR, "");
        String status = a.getString(ARG_STATUS, "-");
        String start  = a.getString(ARG_START, null);
        String end    = a.getString(ARG_END, null);
        String qr     = a.getString(ARG_QR, null);
        String reason = a.getString(ARG_REASON, null);  // NEW

        tvStation.setText(name);
        tvAddr.setText((addr == null || addr.trim().isEmpty()) ? getString(R.string.na) : addr);
        tvStatus.setText(status);
        tvWhen.setText(formatRange(start, end));

        if (qr == null || qr.trim().isEmpty()) {
            tvQr.setText(getString(R.string.qr_not_available));
        } else {
            tvQr.setText(qr);
            tvQr.setOnLongClickListener(v1 -> {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("qr", qr));
                return true;
            });
        }

        // ---- NEW: show rejection reason when present or when status says Rejected ----
        boolean isRejected = status != null && status.equalsIgnoreCase("Rejected");
        boolean hasReason  = reason != null && reason.trim().length() > 0;

        if (isRejected || hasReason) {
            dividerRej.setVisibility(View.VISIBLE);
            tvRejectionTitle.setVisibility(View.VISIBLE);
            tvRejectionReason.setVisibility(View.VISIBLE);
            tvRejectionReason.setText(hasReason ? reason : getString(R.string.na));
            // Optional: long-press to copy the reason
            tvRejectionReason.setOnLongClickListener(copyView -> {
                ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("rejectionReason", tvRejectionReason.getText()));
                return true;
            });
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.details)
                .setView(v)
                .setPositiveButton(R.string.ok, (d1, which) -> dismiss());

        if (qr != null && !qr.trim().isEmpty()) {
            b.setNeutralButton(R.string.share, (d12, which) -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, qr);
                startActivity(Intent.createChooser(i, getString(R.string.share)));
            });
        }
        return b.create();
    }

    private static String formatRange(String isoStart, String isoEnd){
        Long s = parseUtc(isoStart); Long e = parseUtc(isoEnd);
        if (s == null || e == null) return "-";
        SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return d.format(new Date(s)) + " → " + d.format(new Date(e));
    }
    private static Long parseUtc(String iso){
        if (iso == null) return null;
        String[] p = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm'Z'"};
        for (String f : p){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                if (d != null) return d.getTime();
            } catch (Exception ignored){}
        }
        return null;
    }
}
