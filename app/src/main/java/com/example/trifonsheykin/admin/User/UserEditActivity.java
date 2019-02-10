package com.example.trifonsheykin.admin.User;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.trifonsheykin.admin.DbHelper;
import com.example.trifonsheykin.admin.Key.KeySelectActivity;
import com.example.trifonsheykin.admin.Lock.LockSelectActivity;
import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.Calendar;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UserEditActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private Button startTime;
    private Button stopTime;
    private Button selectLock;
    private Button selectKey;
    private Button shareAsQr;
    private Button shareAsText;
    private Button saveData;
    private long lockRowId;
    private long keyRowId;
    private ImageView qrImage;
    private Cursor lockCursor;
    private Cursor keyCursor;
    private Cursor aesCursor;
    private boolean startTimeSelect;
    private String doorOneId;

    private SQLiteDatabase mDb;
    private static final int REQUEST_LOCK_ROW_ID = 1;
    private static final int REQUEST_KEY_ROW_ID = 2;
    String[] projectionLock = {
            LockDataContract._ID,
            LockDataContract.COLUMN_LOCK1_TITLE,
            LockDataContract.COLUMN_LOCK2_TITLE,
            LockDataContract.COLUMN_SECRET_KEY,
            LockDataContract.COLUMN_ADM_ID,
            LockDataContract.COLUMN_ADM_KEY,
            LockDataContract.COLUMN_DOOR1_ID,
            LockDataContract.COLUMN_DOOR2_ID,
            LockDataContract.COLUMN_CWJAP_SSID,
            LockDataContract.COLUMN_CWJAP_PWD,
            LockDataContract.COLUMN_CIPSTA_IP,
            LockDataContract.COLUMN_CWSAP_SSID,
            LockDataContract.COLUMN_CWSAP_PWD,
            LockDataContract.COLUMN_CWMODE
    };


    String[] projectionKey = {
            LockDataContract._ID,
            LockDataContract.COLUMN_KEY_NAME,
            LockDataContract.COLUMN_KEY_LOCKS,
            LockDataContract.COLUMN_KEY_DATA
    };

    String[] projectionAes = {
            LockDataContract._ID,
            LockDataContract.COLUMN_AES_KEY,
            LockDataContract.COLUMN_AES_KEY_USED_FLAG,
            LockDataContract.COLUMN_AES_DOOR1_ID,
            LockDataContract.COLUMN_AES_DOOR2_ID,
            LockDataContract.COLUMN_AES_MEM_PAGE
    };

    private TextView tvStartTime;
    private TextView tvStopTime;
    private TextView tvSelectLock;
    private TextView tvSelectKey;
    private TextView tvAccessCode;

    private EditText etUserName;

    int   startYear, startMonth, startDay, startHour, startMinute;
    int   stopYear, stopMonth, stopDay, stopHour, stopMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);
        startTimeSelect  = true;

        startTime = findViewById(R.id.b_select_start_datetime);
        stopTime = findViewById(R.id.b_select_stop_datetime);
        selectLock = findViewById(R.id.b_select_lock);
        selectKey = findViewById(R.id.b_select_key);
        shareAsQr = findViewById(R.id.b_share_qr);
        shareAsText = findViewById(R.id.b_share_text);
        saveData = findViewById(R.id.b_save);

        qrImage = findViewById(R.id.iv_qr_code);


        tvStartTime = findViewById(R.id.tv_selected_start_datetime);
        tvStopTime = findViewById(R.id.tv_selected_stop_datetime);
        tvSelectLock = findViewById(R.id.tv_selected_lock);
        tvSelectKey = findViewById(R.id.tv_selected_key);
        tvAccessCode = findViewById(R.id.tv_text_code);

        etUserName = findViewById(R.id.et_user_name);

        // Create a DB helper (this will create the DB if run for the first time)
        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        Calendar c = Calendar.getInstance();
        stopDay = startDay = c.get(Calendar.DAY_OF_MONTH);
        stopMonth = startMonth = c.get(Calendar.MONTH);
        stopYear = startYear = c.get(Calendar.YEAR);
        stopHour = startHour = c.get(Calendar.HOUR_OF_DAY);
        stopMinute = startMinute = c.get(Calendar.MINUTE);

        tvStartTime.setText(String.format("Start time:\n%02d:%02d %02d.%02d.%02d", startHour, startMinute, startDay, (startMonth+1), startYear));

        //byte[] test = ipStrToHex("192.168.12.12");
        //byte[] test = timeIntToHex(startHour, startMinute, startDay, startMonth, startYear);
        tvStopTime.setText("Not set");


        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimeSelect = true;
                DatePickerDialog datePickerDialog = new DatePickerDialog(UserEditActivity.this, UserEditActivity.this, startYear, startMonth, startDay);
                datePickerDialog.show();


            }
        });
        stopTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimeSelect = false;
                DatePickerDialog datePickerDialog = new DatePickerDialog(UserEditActivity.this, UserEditActivity.this, stopYear, stopMonth, stopDay);
                datePickerDialog.show();
            }
        });


        selectLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //HERE WE JUMP TO CHOOSE LOCK ACTIVITY
                Context context = UserEditActivity.this;
                Class destinationActivity = LockSelectActivity.class;
                startActivityForResult(new Intent(context, destinationActivity), REQUEST_LOCK_ROW_ID);
            }
        });

        selectKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = UserEditActivity.this;
                Class destinationActivity = KeySelectActivity.class;
                startActivityForResult(new Intent(context, destinationActivity), REQUEST_KEY_ROW_ID);

            }
        });


        shareAsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               byte[] tmp = generateAccessCode();
               String encoded = Base64.encodeToString(tmp, Base64.DEFAULT);

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, encoded);
                sendIntent.setType("text/plain");

                if (sendIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(sendIntent);
                }

            }
        });

        shareAsQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] tmp = generateAccessCode();
                String encoded = Base64.encodeToString(tmp, Base64.DEFAULT);
                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                try{
                    BitMatrix bitMatrix = multiFormatWriter.encode(encoded, BarcodeFormat.QR_CODE, 200, 200);
                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                    qrImage.setImageBitmap(bitmap);

                }catch (WriterException e){

                    e.printStackTrace();
                }
            }
        });

        saveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                byte[] userAes = aesCursor.getBlob(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY));
                byte userTag = (byte) aesCursor.getInt(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_MEM_PAGE));
                String door1Id = aesCursor.getString(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_DOOR1_ID));
                String door2Id = aesCursor.getString(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_DOOR2_ID));
                byte keyUsedFlag = (byte) aesCursor.getInt(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY_USED_FLAG));
                tvAccessCode.setText(userAes.toString() + userTag + door1Id + door2Id + keyUsedFlag);
                aesCursor.moveToNext();


            }
        });






    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){

            if(requestCode == REQUEST_LOCK_ROW_ID){
                lockRowId = data.getLongExtra("rowId", -1);//intent.putExtra("rowId", id);
                if(lockRowId == -1) tvSelectLock.setText("ERROR ROW ID");
                else{
                    lockCursor = mDb.query(
                            LockDataContract.TABLE_NAME_LOCK_DATA,
                            projectionLock,
                            LockDataContract._ID + "= ?",
                            new String[] {String.valueOf(lockRowId)},
                            null,
                            null,
                            LockDataContract.COLUMN_TIMESTAMP
                    );
                    lockCursor.moveToPosition(0);
                    String lock1Title = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
                    String lock2Title = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
                    doorOneId = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));

                    tvSelectLock.setText(lock1Title + " and " + lock2Title);
                }

            }else if(requestCode == REQUEST_KEY_ROW_ID){
                keyRowId = data.getLongExtra("rowId", -1);//intent.putExtra("rowId", id);
                if(keyRowId == -1) tvSelectKey.setText("ERROR ROW ID");
                else{
                    keyCursor = mDb.query(
                            LockDataContract.TABLE_NAME_KEY_DATA,
                            projectionKey,
                            LockDataContract._ID + "= ?",
                            new String[] {String.valueOf(keyRowId)},
                            null,
                            null,
                            LockDataContract.COLUMN_TIMESTAMP
                    );
                    keyCursor.moveToPosition(0);
                    String keyTitle = keyCursor.getString(keyCursor.getColumnIndex(LockDataContract.COLUMN_KEY_NAME));
                    tvSelectKey.setText(keyTitle);

                }
            }
        }
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

    private byte[] generateAccessCode(){
        byte[] secretWord;
        byte[] plainWord = new byte[32];
        byte[] secretKey;

        byte[] userIdAsAes = new byte[4];
        byte[] passData;
        byte[] door1StartTime;
        byte[] door1StopTime;
        byte[] door2StartTime;
        byte[] door2StopTime;
        byte[] doorAccessTime = new byte[20];

        byte[] userAes;
        byte[] ipAddr;
        byte[] doorId;
        byte userTag;

        byte[] accessCode = new byte[57];

        aesCursor = mDb.query(
                LockDataContract.TABLE_NAME_AES_DATA,
                projectionAes,
                LockDataContract.COLUMN_AES_DOOR1_ID + "= ?",
                new String[] {String.valueOf(doorOneId)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
        if(aesCursor !=null && aesCursor.getCount()>0){
            aesCursor.moveToPosition(0);
            int count = aesCursor.getCount();
            for(int i = 0; i < count; i++){
                if(aesCursor.getInt(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY_USED_FLAG)) == 0){
                    break;
                }else{
                    aesCursor.moveToNext();
                }
            }

        }else{
            return null;
        }

        userAes = aesCursor.getBlob(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY));
        userTag = (byte) aesCursor.getInt(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_MEM_PAGE));
        System.arraycopy(userAes, 0, userIdAsAes, 0, 4);
        passData = keyCursor.getBlob(keyCursor.getColumnIndex(LockDataContract.COLUMN_KEY_DATA));
        door1StartTime = timeIntToHex(startHour, startMinute, startDay, startMonth, startYear);
        door2StartTime = timeIntToHex(startHour, startMinute, startDay, startMonth, startYear);
        door1StopTime = timeIntToHex(stopHour, stopMinute, stopDay, stopMonth, stopYear);
        door2StopTime = timeIntToHex(stopHour, stopMinute, stopDay, stopMonth, stopYear);
        System.arraycopy(door1StartTime, 0, doorAccessTime, 0, 5);
        System.arraycopy(door1StopTime, 0, doorAccessTime, 5, 5);
        System.arraycopy(door2StartTime, 0, doorAccessTime, 10, 5);
        System.arraycopy(door2StopTime, 0, doorAccessTime, 15, 5);

        System.arraycopy(userIdAsAes, 0, plainWord, 0, 4);
        System.arraycopy(passData, 0, plainWord, 4, 8);
        System.arraycopy(doorAccessTime, 0, plainWord, 12, 20);

        secretKey = lockCursor.getBlob(lockCursor.getColumnIndex(LockDataContract.COLUMN_SECRET_KEY));
        String ip = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_CIPSTA_IP));
        ipAddr = ipStrToHex(ip);
        String s = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));
        doorId = Base64.decode(s, Base64.DEFAULT);


        byte[] tempIV = new byte[16];
        System.arraycopy(userAes, 0, tempIV, 0, 16);
        secretWord = encrypt(plainWord, tempIV, secretKey);

        System.arraycopy(userAes, 0, accessCode, 0, 16);
        System.arraycopy(secretWord, 0, accessCode, 16, 32);
        System.arraycopy(ipAddr, 0, accessCode, 48, 4);
        System.arraycopy(doorId, 0, accessCode, 52, 4);
        accessCode[56] = userTag;

        return accessCode;
    }



    private byte[] ipStrToHex(String ip){
        byte[] output = new byte[4];

        String[] temp = ip.split("\\.");
        for(int i = 0; i < output.length; i++){
            int j = Integer.valueOf(temp[i]);
            output[i] = (byte)j;
        }
        return output;
    }


    private static byte[] encrypt(byte[] data, byte[] initVector, byte[] key) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec k = new SecretKeySpec(key, "AES_256");
            cipher.init(Cipher.ENCRYPT_MODE, k, iv);
            byte[] cipherByte = cipher.doFinal(data);
            return cipherByte;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * this function print access code on the screen
     * */
    void printAccessCode(byte[] accessCode){

    }

    private byte[] timeIntToHex(int hour, int minute, int day, int month, int year){
        byte[] output = new byte[5];
        int tempX_, temp_X;

        temp_X = year % 10;
        tempX_ = (year / 10 % 10) << 4;
        output[0] = (byte) (tempX_ | temp_X);

        month++;
        temp_X = month % 10;
        tempX_ = (month / 10 % 10) << 4;
        output[1] = (byte) (tempX_ | temp_X);

        temp_X = day % 10;
        tempX_ = day / 10 % 10;
        output[2] = (byte) (tempX_ | temp_X);

        temp_X = hour % 10;
        tempX_ = (hour/ 10 % 10) << 4;
        output[3] = (byte) (tempX_ | temp_X);

        temp_X = minute % 10;
        tempX_ = (minute / 10 % 10) << 4;
        output[4] = (byte) (tempX_ | temp_X);

        return output;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        TimePickerDialog timePickerDialog;
        if(startTimeSelect){
            startDay = dayOfMonth;
            startMonth = month;
            startYear = year;
            timePickerDialog = new TimePickerDialog(UserEditActivity.this, UserEditActivity.this, startHour, startMinute,true);

        }else{//stopTimeSelect
            stopDay = dayOfMonth;
            stopMonth = month;
            stopYear = year;
            timePickerDialog = new TimePickerDialog(UserEditActivity.this, UserEditActivity.this, stopHour, stopMinute,true);

        }
        timePickerDialog.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if(startTimeSelect){
            startHour = hourOfDay;
            startMinute = minute;
            tvStartTime.setText(String.format("Start time:\n%02d:%02d %02d.%02d.%02d", startHour, startMinute, startDay, (startMonth+1), startYear));
        }else{//stopTimeSelect
            stopHour = hourOfDay;
            stopMinute = minute;
            tvStopTime.setText(String.format("Stop time:\n%02d:%02d %02d.%02d.%02d", stopHour, stopMinute, stopDay, (stopMonth+1), stopYear));
        }

    }
}
