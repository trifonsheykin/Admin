package com.example.trifonsheykin.admin.Key;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.trifonsheykin.admin.DbHelper;
import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

import java.util.Arrays;

public class KeyInfoActivity extends AppCompatActivity {

    private TextView tvKeyInfo;

    private long id;
    String[] projectionKey = {
            LockDataContract._ID,
            LockDataContract.COLUMN_KEY_NAME,
            LockDataContract.COLUMN_KEY_LOCKS,
            LockDataContract.COLUMN_KEY_DATA
    };

    private SQLiteDatabase mDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_info);
        tvKeyInfo = findViewById(R.id.tv_key_info);
        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        Intent intent = getIntent();
        id = intent.getLongExtra("id", -1);
        Cursor cursor = mDb.query(
                LockDataContract.TABLE_NAME_KEY_DATA,
                projectionKey,
                LockDataContract._ID + "= ?",
                new String[] {String.valueOf(id)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
        cursor.moveToPosition(0);

        String keyTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_NAME));
        String keyLocks = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_LOCKS));
        byte[] wiegand = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_DATA));
        String keyData = bytesToHexString(wiegand);//Arrays.toString(wiegand);



        tvKeyInfo.setText("Key title: "         + keyTitle +
                "\n\nKey locks: "  + keyLocks +
                "\n\nWiegand data:\n" + keyData +
                  "\n" + Arrays.toString(wiegand));


    }

    public static String bytesToHexString(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
            sb.append(String.format("0x%02x ", b&0xff));
        }
        return sb.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

}
