package com.example.evchargeapp.utils;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeUtil {
    private TimeUtil(){}

    public static Long parseIsoToMillis(String iso){
        if (iso == null) return null;
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mmX"
        };
        for (String p : patterns){
            try{
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                ParsePosition pos = new ParsePosition(0);
                Date d = sdf.parse(iso, pos);
                if (d != null && pos.getIndex() == iso.length()) return d.getTime();
            }catch(Exception ignored){}
        }
        return null;
    }

    public static String fmtLocal(long ms){
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        out.setTimeZone(TimeZone.getDefault());
        return out.format(new Date(ms));
    }
}
