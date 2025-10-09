package com.example.evchargeapp.common;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String P = "ev_session";
    private static final String K_TOKEN = "token";
    private static final String K_ROLE = "role";
    private static final String K_SUBJECT = "subject"; // owner NIC or user Id

    public static void save(Context c, String token, String role, String subject){
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit().putString(K_TOKEN, token)
                .putString(K_ROLE, role)
                .putString(K_SUBJECT, subject)
                .apply();
    }
    public static String token(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_TOKEN, null); }
    public static String role(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_ROLE, null); }
    public static String subject(Context c){ return c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_SUBJECT, null); }
    public static void clear(Context c){ c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply(); }
}
