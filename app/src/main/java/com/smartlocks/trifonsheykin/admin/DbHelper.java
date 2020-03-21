package com.smartlocks.trifonsheykin.admin;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static DbHelper sInstance;

    // The database name
    private static final String DATABASE_NAME = "userdata.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 1;



    public static synchronized DbHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    // конструктор суперкласса
    private DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);


    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create a table to hold user data

        final String SQL_CREATE_USERDATA_TABLE = "CREATE TABLE " + LockDataContract.TABLE_NAME_USER_DATA //
                + " ("
                + LockDataContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LockDataContract.COLUMN_USER_NAME + " TEXT, "
                + LockDataContract.COLUMN_USER_LOCKS  + " TEXT, "//titles
                + LockDataContract.COLUMN_USER_ACCESS_CODE + " BLOB, "
                + LockDataContract.COLUMN_AC_CREATION_DATE + " TEXT, "
                + LockDataContract.COLUMN_USER_KEY_TITLE + " TEXT, "
                + LockDataContract.COLUMN_LOCK_ROW_ID + " INTEGER, "
                + LockDataContract.COLUMN_AES_KEY_ROW_ID + " INTEGER, "
                + LockDataContract.COLUMN_USER_EXPIRED + " INTEGER, "
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        final String SQL_CREATE_KEYDATA_TABLE = "CREATE TABLE " + LockDataContract.TABLE_NAME_KEY_DATA //
                + " ("
                + LockDataContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LockDataContract.COLUMN_KEY_NAME + " TEXT, "
                + LockDataContract.COLUMN_KEY_LOCKS  + " TEXT, "//titles
                + LockDataContract.COLUMN_KEY_DATA + " BLOB, " // type, wiegand sequence etc
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        final String SQL_CREATE_AESDATA_TABLE = "CREATE TABLE " + LockDataContract.TABLE_NAME_AES_DATA//
                + " ("
                + LockDataContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LockDataContract.COLUMN_AES_KEY + " BLOB, "
                + LockDataContract.COLUMN_AES_KEY_USED_FLAG  + " INTEGER, "//titles
                + LockDataContract.COLUMN_AES_DOOR1_ID + " TEXT, "
                + LockDataContract.COLUMN_AES_DOOR2_ID  + " TEXT, "
                + LockDataContract.COLUMN_AES_MEM_PAGE + " INTEGER, "
                + LockDataContract.COLUMN_LOCK_ROW_ID + " INTEGER, "
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        // Create a table to hold lock data
        final String SQL_CREATE_LOCKDATA_TABLE = "CREATE TABLE " + LockDataContract.TABLE_NAME_LOCK_DATA //
                + " ("
                + LockDataContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + LockDataContract.COLUMN_LOCK1_TITLE + " TEXT NOT NULL, "
                + LockDataContract.COLUMN_LOCK2_TITLE + " TEXT NOT NULL, "
                + LockDataContract.COLUMN_SECRET_KEY + " BLOB, "
                + LockDataContract.COLUMN_SUPER_KEY + " BLOB, "
                + LockDataContract.COLUMN_ADM_KEY + " BLOB, "
                + LockDataContract.COLUMN_DOOR1_ID + " TEXT, "
                + LockDataContract.COLUMN_DOOR2_ID  + " TEXT, "
                + LockDataContract.COLUMN_CWJAP_SSID + " TEXT, "//
                + LockDataContract.COLUMN_CWJAP_PWD + " TEXT, "//
                + LockDataContract.COLUMN_CIPSTA_IP + " TEXT, "//
                + LockDataContract.COLUMN_CWSAP_SSID + " TEXT, "//
                + LockDataContract.COLUMN_CWSAP_PWD + " TEXT, "//
                + LockDataContract.COLUMN_CWMODE  + " INTEGER, "//
                + LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES + " BLOB, "
                + LockDataContract.COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + "); ";

        db.execSQL(SQL_CREATE_USERDATA_TABLE);
        db.execSQL(SQL_CREATE_KEYDATA_TABLE);
        db.execSQL(SQL_CREATE_LOCKDATA_TABLE);
        db.execSQL(SQL_CREATE_AESDATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now simply drop the table and create a new one. This means if you change the
        // DATABASE_VERSION the table will be dropped.
        // In a production app, this method might be modified to ALTER the table
        // instead of dropping it, so that existing data is not deleted.
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_USER_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_KEY_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_LOCK_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + LockDataContract.TABLE_NAME_AES_DATA);
        onCreate(db);

    }

}