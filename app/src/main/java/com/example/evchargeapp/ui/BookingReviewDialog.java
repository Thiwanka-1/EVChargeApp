package com.example.evchargeapp.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.evchargeapp.R;

public class BookingReviewDialog extends DialogFragment {

    public interface OnDecision {
        void onConfirm();
    }

    private static final String ARG_MODE   = "mode";   // "create" | "update"
    private static final String ARG_ST    = "station";
    private static final String ARG_DATE  = "date";
    private static final String ARG_START = "start";
    private static final String ARG_END   = "end";
    private static final String ARG_DUR   = "dur";

    private @Nullable OnDecision decision;

    public static BookingReviewDialog newInstance(
            String mode, String stationLabel, String date, String start, String end, String duration) {
        Bundle b = new Bundle();
        b.putString(ARG_MODE, mode);
        b.putString(ARG_ST, stationLabel);
        b.putString(ARG_DATE, date);
        b.putString(ARG_START, start);
        b.putString(ARG_END, end);
        b.putString(ARG_DUR, duration);
        BookingReviewDialog d = new BookingReviewDialog();
        d.setArguments(b);
        return d;
    }

    public BookingReviewDialog setDecision(OnDecision d){
        this.decision = d; return this;
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_booking_review, null, false);

        Bundle a = getArguments()==null? new Bundle() : getArguments();
        String mode  = a.getString(ARG_MODE, "create");
        String st    = a.getString(ARG_ST, "-");
        String date  = a.getString(ARG_DATE, "-");
        String start = a.getString(ARG_START, "-");
        String end   = a.getString(ARG_END, "-");
        String dur   = a.getString(ARG_DUR, "-");

        ((TextView) v.findViewById(R.id.tvStation)).setText(st);
        ((TextView) v.findViewById(R.id.tvDate)).setText(date);
        ((TextView) v.findViewById(R.id.tvStart)).setText(start);
        ((TextView) v.findViewById(R.id.tvEnd)).setText(end);
        ((TextView) v.findViewById(R.id.tvDuration)).setText(dur);

        String title = "create".equals(mode) ? getString(R.string.review_create_title)
                : getString(R.string.review_update_title);

        return new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(v)
                .setNegativeButton(R.string.back, (d1,w)-> dismiss())
                .setPositiveButton(R.string.confirm, (d1,w)-> {
                    if (decision!=null) decision.onConfirm();
                    dismiss();
                })
                .create();
    }
}
