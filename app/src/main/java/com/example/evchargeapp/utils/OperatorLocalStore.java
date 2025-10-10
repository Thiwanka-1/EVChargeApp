package com.example.evchargeapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class OperatorLocalStore {
    private final DBHelper helper;
    public OperatorLocalStore(Context ctx){ helper = new DBHelper(ctx); }

    public static class Operator {
        public String id;
        public String username;
        public String role;
        public boolean isActive;
    }

    public void upsert(String id, String username, String role, boolean isActive){
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("id", id);
        v.put("username", username);
        v.put("role", role);
        v.put("is_active", isActive ? 1 : 0);
        v.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict("operator_profile", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Operator get(){
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, username, role, is_active FROM operator_profile LIMIT 1", null);
        try {
            if (c.moveToFirst()) {
                Operator o = new Operator();
                o.id = c.getString(0);
                o.username = c.getString(1);
                o.role = c.getString(2);
                o.isActive = c.getInt(3) == 1;
                return o;
            }
            return null;
        } finally { c.close(); }
    }

    public void clear(){
        helper.getWritableDatabase().delete("operator_profile", null, null);
    }
}
