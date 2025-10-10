package com.example.evchargeapp.owner;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.evchargeapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class BookingQrDialog extends DialogFragment {

    private static final String ARG_CODE = "code";

    public static BookingQrDialog newInstance(String code){
        BookingQrDialog d = new BookingQrDialog();
        Bundle b = new Bundle();
        b.putString(ARG_CODE, code);
        d.setArguments(b);
        return d;
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String code = getArguments() != null ? getArguments().getString(ARG_CODE) : "";
        ImageView iv = new ImageView(requireContext());
        iv.setAdjustViewBounds(true);
        iv.setPadding(24,24,24,24);

        try {
            BarcodeEncoder enc = new BarcodeEncoder();
            Bitmap bmp = enc.encodeBitmap(code, BarcodeFormat.QR_CODE, 720, 720);
            iv.setImageBitmap(bmp);
        } catch (WriterException e) {
            iv.setImageResource(android.R.drawable.ic_dialog_alert);
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.qr_code)
                .setView(iv)
                .setPositiveButton(R.string.ok, (d, w) -> d.dismiss())
                .create();
    }
}
