package com.example.evchargeapp.utils;

import android.util.Base64;
import org.json.JSONObject;

public class JwtUtil {
    public static String getSubject(String jwt){
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String payloadJson = new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP));
            JSONObject obj = new JSONObject(payloadJson);
            return obj.optString("sub", null); // owner NIC or system user id
        } catch (Exception e) { return null; }
    }
}
