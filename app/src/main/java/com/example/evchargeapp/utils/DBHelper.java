package com.example.evchargeapp.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "evcharge.db";
    public static final int DB_VER = 2; // bumped to add operator_profile

    public DBHelper(Context context) { super(context, DB_NAME, null, DB_VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE owner_profile (" +
                "nic TEXT PRIMARY KEY," +
                "first_name TEXT," +
                "last_name TEXT," +
                "email TEXT," +
                "phone TEXT," +
                "is_active INTEGER DEFAULT 1," +
                "updated_at INTEGER)");

        db.execSQL("CREATE TABLE operator_profile (" +
                "id TEXT PRIMARY KEY," +
                "username TEXT," +
                "role TEXT," +
                "is_active INTEGER DEFAULT 1," +
                "updated_at INTEGER)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            // Create operator_profile table when upgrading from v1
            db.execSQL("CREATE TABLE IF NOT EXISTS operator_profile (" +
                    "id TEXT PRIMARY KEY," +
                    "username TEXT," +
                    "role TEXT," +
                    "is_active INTEGER DEFAULT 1," +
                    "updated_at INTEGER)");
        }
    }
}
