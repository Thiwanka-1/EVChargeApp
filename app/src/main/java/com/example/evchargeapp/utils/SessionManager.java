package com.example.evchargeapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String P = "ev_session";
    public static void saveToken(Context c, String t){ put(c, "token", t); }
    public static String getToken(Context c){ return get(c, "token"); }

    public static void saveRole(Context c, String r){ put(c, "role", r); }
    public static String getRole(Context c){ return get(c, "role"); }

    private static void put(Context c, String k, String v){
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        sp.edit().putString(k, v).apply();
    }
    private static String get(Context c, String k){
        SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
        return sp.getString(k, null);
    }
    public static void clear(Context c){
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
