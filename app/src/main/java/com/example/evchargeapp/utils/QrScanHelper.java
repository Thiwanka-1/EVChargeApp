// utils/QrScanHelper.java
package com.example.evchargeapp.utils;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

public class QrScanHelper {
    public interface Callback { void onCode(String value); }

    public static void scan(Fragment frag, Callback cb){
        ActivityResultLauncher<Intent> launcher = frag.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getData() == null) return;
                    String val = res.getData().getStringExtra("SCAN_RESULT");
                    if (val != null) cb.onCode(val);
                }
        );
        Intent i = new Intent(frag.requireContext(), com.journeyapps.barcodescanner.CaptureActivity.class);
        i.setAction("com.google.zxing.client.android.SCAN");
        i.putExtra("SCAN_MODE", "QR_CODE_MODE");
        launcher.launch(i);
    }
}
