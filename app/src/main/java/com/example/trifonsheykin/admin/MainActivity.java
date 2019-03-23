package com.example.trifonsheykin.admin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trifonsheykin.admin.Key.KeyMainActivity;
import com.example.trifonsheykin.admin.Key.KeySelectActivity;
import com.example.trifonsheykin.admin.Lock.LockEditActivity;
import com.example.trifonsheykin.admin.Lock.LockMainActivity;
import com.example.trifonsheykin.admin.Lock.LockSelectActivity;
import com.example.trifonsheykin.admin.User.UserEditActivity;
import com.example.trifonsheykin.admin.User.UserInfoActivity;
import com.example.trifonsheykin.admin.User.UserMainActivity;
import com.google.zxing.Result;

import java.util.Calendar;
import java.util.concurrent.locks.Lock;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{

    private final int QR = 1;
    private final int NFC = 2;
    private final int BUTTON = 3;
    private boolean qrScanner;
    private static final int REQUEST_LOCK_ROW_ID = 1;
    private static final int REQUEST_KEY_ROW_ID = 2;
    private static final int REQUEST_NEW_USER = 5;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;

    private Button bDefaultLock;
    private Button bDefaultKey;
    private CalendarView calendarView;


    private SQLiteDatabase mDb;
    private Cursor cursor;
    String[] projectionUser = {
            LockDataContract._ID,
            LockDataContract.COLUMN_USER_ACCESS_CODE,
            LockDataContract.COLUMN_USER_EXPIRED
    };
    String[] projectionLock = {
            LockDataContract._ID,
            LockDataContract.COLUMN_LOCK1_TITLE,
            LockDataContract.COLUMN_LOCK2_TITLE,
    };

    String[] projectionKey = {
            LockDataContract._ID,
            LockDataContract.COLUMN_KEY_NAME
    };

    private byte[] door1StopTime = new byte[5];
    private byte[] door2StopTime = new byte[5];
   // private TextView tvMainStatus;


    int mYear;
    int mMonth;
    int mDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bDefaultLock = findViewById(R.id.b_default_lock);
        bDefaultKey = findViewById(R.id.b_default_key);
        calendarView = findViewById(R.id.calendarView);


        Context context = getApplicationContext();
        DbHelper dbHelperLock = DbHelper.getInstance(context);
        mDb = dbHelperLock.getWritableDatabase();

        Calendar c = Calendar.getInstance();
        mDay    = c.get(Calendar.DAY_OF_MONTH);
        mMonth  = c.get(Calendar.MONTH)+1;
        mYear   = c.get(Calendar.YEAR);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                mYear = year;
                mMonth = month+1;
                mDay = dayOfMonth;
            }
        });

        bDefaultLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = MainActivity.this;
                Class destinationActivity = LockSelectActivity.class;
                startActivityForResult(new Intent(context, destinationActivity), REQUEST_LOCK_ROW_ID);
            }
        });
        bDefaultKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = MainActivity.this;
                Class destinationActivity = KeySelectActivity.class;
                startActivityForResult(new Intent(context, destinationActivity), REQUEST_KEY_ROW_ID);
            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = sharedPreferences.edit();
//        qrScanner = sharedPreferences.getBoolean("qrScanner", false);
//        if(qrScanner){
//            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
//            startActivityForResult(intent, 0);
//        }

        FloatingActionButton fab = findViewById(R.id.fab_main);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(MainActivity.this, UserEditActivity.class);
                intent.putExtra("lockId", sharedPreferences.getLong("lockRowId", -1));
                intent.putExtra("keyId", sharedPreferences.getLong("keyRowId", -1));
                intent.putExtra("year", mYear);
                intent.putExtra("month", mMonth);
                intent.putExtra("day", mDay);
                startActivityForResult(intent, REQUEST_NEW_USER);//
            }
        });

        cursor = getUsers();
        if(cursor.getCount() != 0){
            cursor.moveToPosition(0);

            int expiredUsers = 0;
            do{
                byte[] accessCode = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_USER_ACCESS_CODE));
                long id = cursor.getInt(cursor.getColumnIndex(LockDataContract._ID));

                System.arraycopy(accessCode, 62, door1StopTime, 0, 5);
                System.arraycopy(accessCode, 72, door2StopTime, 0, 5);

                if(accessTimeExpired(door1StopTime) && accessTimeExpired(door2StopTime)){
                    ContentValues cv = new ContentValues();
                    cv.put(LockDataContract.COLUMN_USER_EXPIRED, 1);
                    mDb.update(LockDataContract.TABLE_NAME_USER_DATA, cv,
                            LockDataContract._ID + "= ?", new String[] {String.valueOf(id)});
                    expiredUsers++;
                }
            }while(cursor.moveToNext());
            if(expiredUsers != 0) Toast.makeText(getApplicationContext(), "Expired users: " + expiredUsers, Toast.LENGTH_SHORT).show();;
        }
        cursor.close();

    }

    @Override
    protected void onResume() {
        super.onResume();

        bDefaultLock.setText(sharedPreferences.getString("lockButton", "default lock"));
        bDefaultKey.setText(sharedPreferences.getString("keyButton", "default key"));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == 0){
                Toast.makeText(getApplicationContext(),data.getStringExtra("result"),Toast.LENGTH_SHORT).show();
            }else if(requestCode == REQUEST_LOCK_ROW_ID){
                long lockRowId = data.getLongExtra("rowId", -1);
                if(lockRowId != -1) {
                    Cursor lockCursor = mDb.query(
                            LockDataContract.TABLE_NAME_LOCK_DATA,
                            projectionLock,
                            LockDataContract._ID + "= ?",
                            new String[]{String.valueOf(lockRowId)},
                            null,
                            null,
                            LockDataContract.COLUMN_TIMESTAMP
                    );
                    lockCursor.moveToPosition(0);
                    String s1 = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
                    String s2 = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
                    String s = s1 + " & " + s2;
                    bDefaultLock.setText(s);
                    spEditor.putLong("lockRowId", lockRowId);
                    spEditor.putString("lockButton", s);
                    spEditor.commit();
                    lockCursor.close();
                }
            }else if(requestCode == REQUEST_KEY_ROW_ID){
                long keyRowId = data.getLongExtra("rowId", -1);
                if(keyRowId != -1) {
                    Cursor keyCursor = mDb.query(
                            LockDataContract.TABLE_NAME_KEY_DATA,
                            projectionKey,
                            LockDataContract._ID + "= ?",
                            new String[]{String.valueOf(keyRowId)},
                            null,
                            null,
                            LockDataContract.COLUMN_TIMESTAMP
                    );
                    keyCursor.moveToPosition(0);
                    String keyTitle = keyCursor.getString(keyCursor.getColumnIndex(LockDataContract.COLUMN_KEY_NAME));
                    bDefaultKey.setText(keyTitle);
                    spEditor.putLong("keyRowId", keyRowId);
                    spEditor.putString("keyButton", keyTitle);
                    spEditor.commit();
                    keyCursor.close();
                }

            }else if(requestCode == REQUEST_NEW_USER){
                long id = data.getLongExtra("id", -1);//intent.putExtra("rowId", id);
                if(id != -1){
                    Intent intent= new Intent(MainActivity.this, UserInfoActivity.class);
                    intent.putExtra("id", id);
                    startActivity(intent);//request code new lock
                }

            }

        }

    }



    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void qrScan(View v){
        Toast.makeText(getApplicationContext(),"QR code reader mode",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
        startActivityForResult(intent, 0);


    }

    private boolean accessTimeExpired(byte[] doorStopTime){
        Calendar c = Calendar.getInstance();
        int   year, month, day, hour, minute;
        day    = c.get(Calendar.DAY_OF_MONTH);
        month  = c.get(Calendar.MONTH)+1;
        year   = c.get(Calendar.YEAR);
        hour   = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
        byte[] currentTime = timeIntToHex(hour, minute, day, month, year);

        for(int i = 0; i < 5; i++){
            if(Byte.compare(currentTime[i], doorStopTime[i]) < 0) return false;
            else if(Byte.compare(currentTime[i], doorStopTime[i]) > 0) return true;
        }
        return true;
    }
    private byte[] timeIntToHex(int hour, int minute, int day, int month, int year){
        byte[] output = new byte[5];
        int tempX_, temp_X;

        temp_X = year % 10;
        tempX_ = (year / 10 % 10) << 4;
        output[0] = (byte) (tempX_ | temp_X);

        temp_X = month % 10;
        tempX_ = (month / 10 % 10) << 4;
        output[1] = (byte) (tempX_ | temp_X);

        temp_X = day % 10;
        tempX_ = (day / 10 % 10) << 4;
        output[2] = (byte) (tempX_ | temp_X);

        temp_X = hour % 10;
        tempX_ = (hour/ 10 % 10) << 4;
        output[3] = (byte) (tempX_ | temp_X);

        temp_X = minute % 10;
        tempX_ = (minute / 10 % 10) << 4;
        output[4] = (byte) (tempX_ | temp_X);

        return output;
    }

    private Cursor getUsers() {
        return mDb.query(
                LockDataContract.TABLE_NAME_USER_DATA,
                projectionUser,
                LockDataContract.COLUMN_USER_EXPIRED + "= ?",
                new String[] {String.valueOf(0)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_users) {
            Intent intent = new Intent(MainActivity.this, UserMainActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_keys) {
            Intent intent = new Intent(MainActivity.this, KeyMainActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_locks) {
            Intent intent = new Intent(MainActivity.this, LockMainActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_tags) {
            Intent intent = new Intent(MainActivity.this, TagActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_help) {
            Intent intent = new Intent(MainActivity.this, HelpActivity.class);
            startActivity(intent);

        }



        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
//        if (item.getItemId() == R.id.nav_qr_scan) {
//            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
//            startActivityForResult(intent, 0);
//            return true;
//        }
        return false;
    }


}
