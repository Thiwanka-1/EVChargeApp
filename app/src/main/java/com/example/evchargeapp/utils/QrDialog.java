package com.example.evchargeapp.utils;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import android.graphics.Bitmap.Config;

public class QrDialog extends DialogFragment {
    private static final String ARG_TEXT = "t";

    /** Factory method */
    public static QrDialog newInstance(@NonNull String text) {
        QrDialog d = new QrDialog();
        Bundle b = new Bundle();
        b.putString(ARG_TEXT, text);
        d.setArguments(b);
        return d;
    }

    /** Helper to present the dialog (renamed to avoid clashing with DialogFragment#show) */
    public static void display(@NonNull FragmentManager fm, @NonNull String text) {
        QrDialog d = newInstance(text);
        d.show(fm, "qr");
    }

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String txt = "";
        Bundle args = getArguments();
        if (args != null) txt = args.getString(ARG_TEXT, "");

        ImageView iv = new ImageView(requireContext());
        iv.setImageBitmap(makeQr(txt, 800, 800));
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        iv.setPadding(pad, pad, pad, pad);

        Dialog dlg = new Dialog(requireContext());
        dlg.setContentView(iv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return dlg;
    }

    private static @Nullable Bitmap makeQr(String text, int w, int h) {
        try {
            BitMatrix m = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, w, h, null);
            Bitmap bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bmp.setPixel(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }
}
