package com.example.evchargeapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class OwnerLocalStore {
    private final DBHelper helper;
    public OwnerLocalStore(Context ctx){ helper = new DBHelper(ctx); }

    public void upsert(String nic, String first, String last, String email, String phone, boolean isActive){
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("nic", nic);
        v.put("first_name", first);
        v.put("last_name", last);
        v.put("email", email);
        v.put("phone", phone);
        v.put("is_active", isActive ? 1 : 0);
        v.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict("owner_profile", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Owner get() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT nic, first_name, last_name, email, phone, is_active FROM owner_profile LIMIT 1", null);
        try {
            if (c.moveToFirst()) {
                Owner o = new Owner();
                o.nic = c.getString(0);
                o.firstName = c.getString(1);
                o.lastName = c.getString(2);
                o.email = c.getString(3);
                o.phone = c.getString(4);
                o.isActive = c.getInt(5) == 1;
                return o;
            }
            return null;
        } finally { c.close(); }
    }

    public void clear(){
        helper.getWritableDatabase().delete("owner_profile", null, null);
    }

    // tiny POJO for local use
    public static class Owner {
        public String nic, firstName, lastName, email, phone;
        public boolean isActive;
    }
}
