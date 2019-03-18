package com.example.trifonsheykin.admin.User;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.trifonsheykin.admin.DbHelper;
import com.example.trifonsheykin.admin.Key.KeySelectActivity;
import com.example.trifonsheykin.admin.Lock.LockSelectActivity;
import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

import java.util.Calendar;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class UserEditActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {


    private EditText etUserName;
    private TextView tvSelectedKey;
    private Button bSelectLock;
    private Button bSelectKey;
    private CheckBox cbDoor1;
    private CheckBox cbDoor2;
    private Button bStartTimeDoor1;
    private Button bStopTimeDoor1;
    private Button bStartTimeDoor2;
    private Button bStopTimeDoor2;
    private Button bSaveData;

    private long lockRowId;
    private long keyRowId;
    private long aesRowId;
    private Cursor lockCursor;
    private Cursor keyCursor;
    private Cursor aesCursor;
    private int timeSelect;
    private String doorOneId;
    private boolean lockSelected;
    private boolean keySelected;
    private String curTime;

    private boolean door1StopTimeSelected;
    private boolean door2StopTimeSelected;

    private String lock1Title;
    private String lock2Title;
    private SQLiteDatabase mDb;
    private static final int REQUEST_LOCK_ROW_ID = 1;
    private static final int REQUEST_KEY_ROW_ID = 2;
    private static final int DOOR1_START_TIME_SELECT = 1;
    private static final int DOOR1_STOP_TIME_SELECT = 2;
    private static final int DOOR2_START_TIME_SELECT = 3;
    private static final int DOOR2_STOP_TIME_SELECT = 4;

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
            LockDataContract.COLUMN_CWMODE,
            LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES
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
            LockDataContract.COLUMN_AES_MEM_PAGE,
            LockDataContract.COLUMN_LOCK_ROW_ID
    };


    int   d1startYear, d1startMonth, d1startDay, d1startHour, d1startMinute;
    int   d1stopYear, d1stopMonth, d1stopDay, d1stopHour, d1stopMinute;
    int   d2startYear, d2startMonth, d2startDay, d2startHour, d2startMinute;
    int   d2stopYear, d2stopMonth, d2stopDay, d2stopHour, d2stopMinute;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);
        etUserName = findViewById(R.id.et_user_name);
        tvSelectedKey = findViewById(R.id.tv_selected_key);
        bSelectLock = findViewById(R.id.b_select_lock);
        bSelectKey = findViewById(R.id.b_select_key);
        bSaveData = findViewById(R.id.b_save);
        cbDoor1 = findViewById(R.id.cb_door1);
        cbDoor2 = findViewById(R.id.cb_door2);
        bStartTimeDoor1 = findViewById(R.id.b_start_door1);
        bStopTimeDoor1 = findViewById(R.id.b_stop_door1);
        bStartTimeDoor2 = findViewById(R.id.b_start_door2);
        bStopTimeDoor2 = findViewById(R.id.b_stop_door2);


        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        Calendar c = Calendar.getInstance();
        d1startDay    = d1stopDay    = d2startDay    = d2stopDay    = c.get(Calendar.DAY_OF_MONTH);
        d1startMonth  = d1stopMonth  = d2startMonth  = d2stopMonth  = c.get(Calendar.MONTH)+1;
        d1startYear   = d1stopYear   = d2startYear   = d2stopYear   = c.get(Calendar.YEAR);
        d1startHour   = d1stopHour   = d2startHour   = d2stopHour   = c.get(Calendar.HOUR_OF_DAY);
        d1startMinute = d1stopMinute = d2startMinute = d2stopMinute = c.get(Calendar.MINUTE);

        Intent intent = getIntent();
        if(intent.hasExtra("day")){
            lockRowId = intent.getLongExtra("lockId", -1);
            keyRowId = intent.getLongExtra("keyId", -1);
            d1stopDay    = d2stopDay   = intent.getIntExtra("day", d1stopDay);
            d1stopMonth  = d2stopMonth = intent.getIntExtra("month", d1stopMonth);
            d1stopYear   = d2stopYear  = intent.getIntExtra("year", d1stopYear);
            door1StopTimeSelected = true;
            door2StopTimeSelected = true;
            etUserName.setText("New user");
            setDateTimeFinalDoor1();
            setDateTimeFinalDoor2();
            checkLockSelected();
            checkKeySelected();
            setElementsEnabled();

        }else{
            door1StopTimeSelected = false;
            door2StopTimeSelected = false;
            keySelected = false;
            lockSelected = false;
            setAllElementsDisabled();
        }

        curTime = String.format("%02d:%02d %02d.%02d.%02d", d1stopHour, d1stopMinute, d1stopDay, d1stopMonth, d1stopYear);

        etUserName.setEnabled(true);


        bSelectLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //HERE WE JUMP TO CHOOSE LOCK ACTIVITY
                Context context = UserEditActivity.this;
                Class destinationActivity = LockSelectActivity.class;
                startActivityForResult(new Intent(context, destinationActivity), REQUEST_LOCK_ROW_ID);
            }
        });

        bSelectKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = UserEditActivity.this;
                Class destinationActivity = KeySelectActivity.class;
                startActivityForResult(new Intent(context, destinationActivity), REQUEST_KEY_ROW_ID);

            }
        });


        cbDoor1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                bStartTimeDoor1.setEnabled(isChecked);
                bStopTimeDoor1.setEnabled(isChecked);
                if(isChecked){
                    bStartTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
                    bStopTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
                }else{
                    bStartTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_disabled_24dp, 0,0,0);
                    bStopTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_disabled_24dp, 0,0,0);
                }
                saveButtonEnableRequest();
            }
        });

        cbDoor2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                bStartTimeDoor2.setEnabled(isChecked);
                bStopTimeDoor2.setEnabled(isChecked);
                if(isChecked){
                    bStartTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
                    bStopTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
                }else{
                    bStartTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_disabled_24dp, 0,0,0);
                    bStopTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_disabled_24dp, 0,0,0);
                }
                saveButtonEnableRequest();
            }
        });

        etUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(etUserName.getText().toString().length() != 0)bSelectLock.setEnabled(true);
                saveButtonEnableRequest();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //b.setText(String.format("Start time:\n%02d:%02d %02d.%02d.%02d", startHour, startMinute, startDay, (startMonth+1), startYear));

        //byte[] test = ipStrToHex("192.168.12.12");
        //byte[] test = timeIntToHex(startHour, startMinute, startDay, startMonth, startYear);



        bStartTimeDoor1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeSelect = DOOR1_START_TIME_SELECT;
                DatePickerDialog datePickerDialog = new DatePickerDialog(UserEditActivity.this, UserEditActivity.this, d1startYear, d1startMonth-1, d1startDay);
                datePickerDialog.show();

            }
        });
        bStopTimeDoor1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeSelect = DOOR1_STOP_TIME_SELECT;
                DatePickerDialog datePickerDialog = new DatePickerDialog(UserEditActivity.this, UserEditActivity.this, d1stopYear, d1stopMonth-1, d1stopDay);
                datePickerDialog.show();
            }
        });

        bStartTimeDoor2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeSelect = DOOR2_START_TIME_SELECT;
                DatePickerDialog datePickerDialog = new DatePickerDialog(UserEditActivity.this, UserEditActivity.this, d2startYear, d2startMonth-1, d2startDay);
                datePickerDialog.show();

            }
        });

        bStopTimeDoor2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeSelect = DOOR2_STOP_TIME_SELECT;
                DatePickerDialog datePickerDialog = new DatePickerDialog(UserEditActivity.this, UserEditActivity.this, d1stopYear, d1stopMonth-1, d1stopDay);
                datePickerDialog.show();
            }
        });



        bSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues cv = new ContentValues();

                byte[] accessCode = generateAccessCode();
                cv.put(LockDataContract.COLUMN_USER_ACCESS_CODE, accessCode);
                cv.put(LockDataContract.COLUMN_USER_NAME, etUserName.getText().toString());
                String userLocks = new String();
                if(cbDoor1.isChecked()) userLocks = userLocks.concat(lock1Title);
                if(cbDoor1.isChecked() && cbDoor2.isChecked())userLocks = userLocks.concat(", ");
                if(cbDoor2.isChecked()) userLocks = userLocks.concat(lock2Title);
                cv.put(LockDataContract.COLUMN_USER_LOCKS, userLocks);
                cv.put(LockDataContract.COLUMN_AC_CREATION_DATE, curTime);
                String keyTitle = keyCursor.getString(keyCursor.getColumnIndex(LockDataContract.COLUMN_KEY_NAME));
                cv.put(LockDataContract.COLUMN_USER_KEY_TITLE, keyTitle);
                cv.put(LockDataContract.COLUMN_LOCK_ROW_ID, lockRowId);
                cv.put(LockDataContract.COLUMN_AES_KEY_ROW_ID, aesRowId);
                cv.put(LockDataContract.COLUMN_USER_EXPIRED, 0);

                long id = mDb.insert(LockDataContract.TABLE_NAME_USER_DATA, null, cv);
                Intent intent = new Intent();

                intent.putExtra("id", id);
                setResult(RESULT_OK, intent);
                finish();

            }
        });




    }

    private void saveButtonEnableRequest(){
        if((etUserName.getText().toString().length() != 0)
                && (cbDoor1.isChecked() || cbDoor2.isChecked())
                && !(cbDoor1.isChecked() && !door1StopTimeSelected)
                && !(cbDoor2.isChecked() && !door2StopTimeSelected)){
            bSaveData.setEnabled(true);
        }else{
            bSaveData.setEnabled(false);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){

            if(requestCode == REQUEST_LOCK_ROW_ID){
                lockRowId = data.getLongExtra("rowId", -1);//intent.putExtra("rowId", id);
                if(lockRowId != -1){
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
                    lock1Title = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
                    lock2Title = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
                    doorOneId = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));
                    //tvSelectLock.setText(lock1Title + " and " + lock2Title);
                    cbDoor1.setText("Door 1 (" + lock1Title + ") access time:");
                    cbDoor2.setText("Door 2 (" + lock2Title + ") access time:");
                    lockSelected = true;
                    if(lockSelected && keySelected){
                        if(door1StopTimeSelected)setDateTimeFinalDoor1();
                        else setDateTimeDoor1();

                        if(door2StopTimeSelected)setDateTimeFinalDoor2();
                        else setDateTimeDoor2();
                    }
                    onKeyLockSelected();
                    bSelectKey.setEnabled(true);
                }

            }else if(requestCode == REQUEST_KEY_ROW_ID){
                keyRowId = data.getLongExtra("rowId", -1);//intent.putExtra("rowId", id);
                if(keyRowId == -1) tvSelectedKey.setText("ERROR ROW ID");
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
                    tvSelectedKey.setText("Key: " + keyTitle);
                    keySelected = true;
                    if(lockSelected && keySelected){
                        if(door1StopTimeSelected)setDateTimeFinalDoor1();
                        else setDateTimeDoor1();

                        if(door2StopTimeSelected)setDateTimeFinalDoor2();
                        else setDateTimeDoor2();
                    }
                    onKeyLockSelected();
                    cbDoor1.setEnabled(true);
                    cbDoor2.setEnabled(true);
                    tvSelectedKey.setEnabled(true);


                }
            }
        }
    }


    private void setAllElementsDisabled(){

        tvSelectedKey.setEnabled(false);
        bSaveData.setEnabled(false);
        bSelectKey.setEnabled(false);
        bSelectLock.setEnabled(false);
        cbDoor1.setEnabled(false);
        cbDoor2.setEnabled(false);
        bStartTimeDoor1.setEnabled(false);
        bStopTimeDoor1.setEnabled(false);
        bStartTimeDoor2.setEnabled(false);
        bStopTimeDoor2.setEnabled(false);
        bStartTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_disabled_24dp, 0,0,0);
        bStartTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_disabled_24dp, 0,0,0);
        bStopTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_disabled_24dp, 0,0,0);
        bStopTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_disabled_24dp, 0,0,0);
        bSelectLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_disabled_24dp, 0,0,0);
        bSelectKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_vpn_key_disabled_24dp,0,0,0);


    }
    private void checkLockSelected(){
        if(lockRowId != -1){
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
            lock1Title = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
            lock2Title = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
            doorOneId = lockCursor.getString(lockCursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));
            //tvSelectLock.setText(lock1Title + " and " + lock2Title);
            cbDoor1.setText("Door 1 (" + lock1Title + ") access time:");
            cbDoor2.setText("Door 2 (" + lock2Title + ") access time:");
            lockSelected = true;
            onKeyLockSelected();
        }else{
            lockSelected = false;
            onKeyLockSelected();
        }
    }

    private void checkKeySelected(){
        if(keyRowId == -1) {
            tvSelectedKey.setText("ERROR ROW ID");
            keySelected = false;
            onKeyLockSelected();
        }else{
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
            tvSelectedKey.setText("Key: " + keyTitle);
            keySelected = true;
            onKeyLockSelected();
            tvSelectedKey.setEnabled(true);
        }

    }

    private void onKeyLockSelected(){
        if(keySelected && lockSelected){
            cbDoor1.setEnabled(true);
            cbDoor2.setEnabled(true);
//            bStartTimeDoor1.setEnabled(true);
//            bStopTimeDoor1.setEnabled(true);
//            bStartTimeDoor2.setEnabled(true);
//            bStopTimeDoor2.setEnabled(true);
//            bStartTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
//            bStartTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
//            bStopTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
//            bStopTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);

        }else{
            cbDoor1.setEnabled(false);
            cbDoor2.setEnabled(false);
//            bStartTimeDoor1.setEnabled(false);
//            bStopTimeDoor1.setEnabled(false);
//            bStartTimeDoor2.setEnabled(false);
//            bStopTimeDoor2.setEnabled(false);
//            bStartTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
//            bStartTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
//            bStopTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
//            bStopTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
        }

        if(lockSelected)bSelectLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_colored_24dp, 0,0,0);
        else bSelectLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_disabled_24dp, 0,0,0);

        if(keySelected)bSelectKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_vpn_key_colored_24dp, 0,0,0);
        else bSelectKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_vpn_key_disabled_24dp, 0,0,0);
    }


    private void setElementsEnabled(){
        tvSelectedKey.setEnabled(true);
        bSelectKey.setEnabled(true);
        bSelectLock.setEnabled(true);
        cbDoor1.setChecked(true);
        cbDoor2.setChecked(true);

    }


    /*  if(state){
            bStartTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
            bStartTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_access_time_colored_24dp, 0,0,0);
            bStopTimeDoor1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
            bStopTimeDoor2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_flat_black_24dp, 0,0,0);
            cbDoor1.setChecked(true);
            cbDoor2.setChecked(true);
            cbDelivery.setChecked(true);
            rbShowQr.setChecked(true);

        }else{*/

    void setDateTimeDoor1(){
        bStartTimeDoor1.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d1startHour, d1startMinute, d1startDay, d1startMonth, d1startYear));
        bStopTimeDoor1.setText("select time");
    }
    void setDateTimeDoor2(){
        bStartTimeDoor2.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d2startHour, d2startMinute, d2startDay, d2startMonth, d2startYear));
        bStopTimeDoor2.setText("select time");
    }

    void setDateTimeFinalDoor1(){
        bStartTimeDoor1.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d1startHour, d1startMinute, d1startDay, d1startMonth, d1startYear));
        bStopTimeDoor1.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d1stopHour, d1stopMinute, d1stopDay, d1stopMonth, d1stopYear));
    }
    void setDateTimeFinalDoor2(){
        bStartTimeDoor2.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d2startHour, d2startMinute, d2startDay, d2startMonth, d2startYear));
        bStopTimeDoor2.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d2stopHour, d2stopMinute, d2stopDay, d2stopMonth, d2stopYear));
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

        byte[] accessCode = new byte[78];

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
                if(i == count - 1) return null;
            }

        }else{
            return null;
        }

        if(!cbDoor1.isChecked()){
            d1stopHour   = d1startHour;
            d1stopMinute = d1startMinute;
            d1stopDay    = d1startDay;
            d1stopMonth  = d1startMonth;
            d1stopYear   = d1startYear;
        }
        if(!cbDoor2.isChecked()){
            d2stopHour   = d2startHour;
            d2stopMinute = d2startMinute;
            d2stopDay    = d2startDay;
            d2stopMonth  = d2startMonth;
            d2stopYear   = d2startYear;
        }

        userAes = aesCursor.getBlob(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY));
        userTag = (byte) aesCursor.getInt(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_MEM_PAGE));
        aesRowId = aesCursor.getLong(aesCursor.getColumnIndex(LockDataContract._ID));

        ContentValues contentValues = new ContentValues();
        contentValues.put(LockDataContract.COLUMN_AES_KEY_USED_FLAG, 1);
        mDb.update(LockDataContract.TABLE_NAME_AES_DATA, contentValues,
                LockDataContract._ID + "= ?", new String[] {String.valueOf(aesRowId)});

        System.arraycopy(userAes, 0, userIdAsAes, 0, 4);
        passData = keyCursor.getBlob(keyCursor.getColumnIndex(LockDataContract.COLUMN_KEY_DATA));

        door1StartTime = timeIntToHex(d1startHour, d1startMinute, d1startDay, d1startMonth, d1startYear);
        door1StopTime =  timeIntToHex(d1stopHour,  d1stopMinute,  d1stopDay,  d1stopMonth,  d1stopYear);

        door2StartTime = timeIntToHex(d2startHour, d2startMinute, d2startDay, d2startMonth, d2startYear);
        door2StopTime = timeIntToHex(d2stopHour, d2stopMinute, d2stopDay, d2stopMonth, d2stopYear);

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

        System.arraycopy(doorAccessTime, 0, accessCode, 57, 20);
        byte[] toXor = new byte[77];
        System.arraycopy(accessCode, 0, toXor, 0, 77);
        accessCode[77] = XORcalc(toXor);
        return accessCode;
    }

    public byte XORcalc(byte[] input){
        byte output = input[0];
        for(int i=1; i<input.length; i++) output = (byte) (output ^ input[i]);
        return output;
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

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        TimePickerDialog timePickerDialog;
        month++;

        if(timeSelect == DOOR1_START_TIME_SELECT){
            d1startDay = dayOfMonth;
            d1startMonth = month;
            d1startYear = year;
            timePickerDialog = new TimePickerDialog(UserEditActivity.this, UserEditActivity.this, d1startHour, d1startMinute,true);
            timePickerDialog.show();

        }else if(timeSelect == DOOR1_STOP_TIME_SELECT){//stopTimeSelect
            d1stopDay = dayOfMonth;
            d1stopMonth = month;
            d1stopYear = year;
            timePickerDialog = new TimePickerDialog(UserEditActivity.this, UserEditActivity.this, d1stopHour, d1stopMinute,true);
            timePickerDialog.show();

        }else if(timeSelect == DOOR2_START_TIME_SELECT){
            d2startDay = dayOfMonth;
            d2startMonth = month;
            d2startYear = year;
            timePickerDialog = new TimePickerDialog(UserEditActivity.this, UserEditActivity.this, d2startHour, d2startMinute,true);
            timePickerDialog.show();

        }else if(timeSelect == DOOR2_STOP_TIME_SELECT){
            d2stopDay = dayOfMonth;
            d2stopMonth = month;
            d2stopYear = year;
            timePickerDialog = new TimePickerDialog(UserEditActivity.this, UserEditActivity.this, d2stopHour, d2stopMinute,true);
            timePickerDialog.show();
        }

    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        if(timeSelect == DOOR1_START_TIME_SELECT){
            d1startHour = hourOfDay;
            d1startMinute = minute;
            bStartTimeDoor1.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d1startHour, d1startMinute, d1startDay, d1startMonth, d1startYear));
        }else if(timeSelect == DOOR1_STOP_TIME_SELECT){//stopTimeSelect
            door1StopTimeSelected = true;
            saveButtonEnableRequest();
            d1stopHour = hourOfDay;
            d1stopMinute = minute;
            bStopTimeDoor1.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d1stopHour, d1stopMinute, d1stopDay, d1stopMonth, d1stopYear));
        }else if(timeSelect == DOOR2_START_TIME_SELECT){
            d2startHour = hourOfDay;
            d2startMinute = minute;
            bStartTimeDoor2.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d2startHour, d2startMinute, d2startDay, d2startMonth, d2startYear));
        }else if(timeSelect == DOOR2_STOP_TIME_SELECT){
            door2StopTimeSelected = true;
            saveButtonEnableRequest();
            d2stopHour = hourOfDay;
            d2stopMinute = minute;
            bStopTimeDoor2.setText(String.format("%02d:%02d\n\n%02d.%02d.%02d", d2stopHour, d2stopMinute, d2stopDay, d2stopMonth, d2stopYear));
        }
    }


}
