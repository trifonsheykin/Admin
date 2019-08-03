package com.example.trifonsheykin.admin.Lock;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.EspNetUtil;
import com.example.trifonsheykin.admin.DbHelper;
import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class LockInfoActivity extends AppCompatActivity {

    /*Data to display

edit text
    Lock 1 title
    Lock 2 title


text view:
    Door 1 ID
    Door 2 ID

    (LOCK MODE: cwmode)

    LOCK WIFI SSID:
    LOCK WIFI PWD:

    AP WIFI SSID:
    AP WIFI PWD:
    LOCK IP ADDRESS:

    NUMBER OF AES KEYS EXPIRED
    NUMBER OF AES KEYS IN USE
    NUMBER OF AES KEYS LEFT

buttons:
    Recover AES

radiobutton:
    LOCK AS ACCESS POINT
    LOCK CONNECTED TO WIFI




     */


    private static final int FINISH = 0;
    private static final int SYNC_FIRST = 1;
    private static final int SYNC_XOR = 2;
    private static final int SYNC_FLOW = 3;
    private static final int LAST_USER_BOUND = 212;
    private static final String ESP_AP_STATIC_IP = "192.168.4.1";
    private static final int DOORID_PAGE = 1;
    private static final int CWMODE_PAGE = 2;
    private static final int CWSAP_SSID_PAGE = 3;
    private static final int CWSAP_PWD_PAGE = 4;
    private static final int CWJAP_SSID_PAGE = 5;
    private static final int CWJAP_PWD_PAGE = 6;
    private static final int SECRET_KEY_PAGE = 7;
    private static final int CIPIP_PAGE = 8;
    private static final int ADM_KEY_PAGE = 9;
    private static final int ADM_ID_PAGE = 10;
    private static final int STATION = 1;
    private static final int SOFTAP = 2;
    private EditText etLock1Title;
    private EditText etLock2Title;
    private EditText etLockSsid;
    private EditText etLockPwd;
    private RadioGroup rgWiFiSettings;
    private RadioButton rbLockAccessPoint;
    private RadioButton rbLockStation;

    private Button bSync;
    private Button bWiFiSet;
    private Button bSave;

    private TextView tvInfo;
    private Switch sEdit;
    private ProgressBar progressBar;

    private boolean accessPointEnabled;
    private boolean stationEnabled;
    private boolean needSync;
    private long rowId;
    private String mApBssidTV;

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

    String[] projectionAes = {
            LockDataContract._ID,
            LockDataContract.COLUMN_AES_KEY,
            LockDataContract.COLUMN_AES_KEY_USED_FLAG,
            LockDataContract.COLUMN_AES_DOOR1_ID,
            LockDataContract.COLUMN_AES_DOOR2_ID,
            LockDataContract.COLUMN_AES_MEM_PAGE,
            LockDataContract.COLUMN_LOCK_ROW_ID
    };



    private String lock1Title;
    private String lock2Title;
    private byte[] secretKey;
    private byte[] admId;
    private byte[] admKey;
    private int[] userAes;
    private int[] userData;
    private String door1Id;
    private String door2Id;
    private String softApSsid;
    private String softApPwd;
    private String ipAddress;
    private String deviceIpAddress;
    private String stationSsid;
    private String stationPwd;
    private int cwmode;
    private byte[] expiredAes;

    private boolean accessPointMode;
    private boolean aesSyncMode;
    ArrayList<byte[]> inputData = new ArrayList<byte[]>();

    private SQLiteDatabase mDb;
    private Cursor cursor;
    private Cursor aesCursor;

    private IEsptouchTask mEsptouchTask;
    private NetworkTask networktask;
    private static final Object mLock = new Object();

    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private EsptouchAsyncTask mTask;


    private void onWifiChanged(WifiInfo info) {
        etLockPwd.setText("");
        //spEditor.putString("password", etApPassword.getText().toString());
        //spEditor.commit();
        if (info == null) {
            etLockSsid.setText("");
            etLockSsid.setTag(null);
            mApBssidTV = "";
            //  bSynchronize.setEnabled(false);
            bWiFiSet.setTag(null);

            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
                new AlertDialog.Builder(LockInfoActivity.this)
                        .setMessage("Wifi disconnected or changed")
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        } else {
            String ssid = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }

            etLockSsid.setText(ssid);
            etLockSsid.setTag(ByteUtil.getBytesByString(ssid));
            byte[] ssidOriginalData = EspUtils.getOriginalSsidBytes(info);
            etLockSsid.setTag(ssidOriginalData);

            String bssid = info.getBSSID();
            mApBssidTV = bssid;

            //        bSynchronize.setEnabled(true);
            bWiFiSet.setTag(Boolean.FALSE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int frequence = info.getFrequency();
                if (frequence > 4900 && frequence < 5900) {
                    // Connected 5G wifi. Device does not support 5G
                    bWiFiSet.setTag(Boolean.TRUE);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_info);

        etLock1Title = findViewById(R.id.et_lock1);
        etLock2Title = findViewById(R.id.et_lock2);
        etLockSsid = findViewById(R.id.et_lock_ssid);
        etLockPwd = findViewById(R.id.et_lock_pwd);
        rgWiFiSettings = findViewById(R.id.rg_wifi_select);
        rbLockAccessPoint = findViewById(R.id.rb_lock_as_ap);
        rbLockStation = findViewById(R.id.rb_lock_connects_wifi);
        bSync = findViewById(R.id.b_sync_aes);
        bWiFiSet = findViewById(R.id.b_set);
        bSave = findViewById(R.id.b_save_lock);
        tvInfo = findViewById(R.id.tv_text);
        sEdit = findViewById(R.id.sw_edit);
        progressBar = findViewById(R.id.pb_lock_info);

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        Intent intent = getIntent();
        rowId = intent.getLongExtra("rowId", -1);
        cursor = mDb.query(
                LockDataContract.TABLE_NAME_LOCK_DATA,
                projectionLock,
                LockDataContract._ID + "= ?",
                new String[] {String.valueOf(rowId)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
        cursor.moveToPosition(0);


        aesCursor = mDb.query(
                LockDataContract.TABLE_NAME_AES_DATA,
                projectionAes,
                LockDataContract.COLUMN_LOCK_ROW_ID + "= ?",
                new String[] {String.valueOf(rowId)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );

        lock1Title  = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
        lock2Title  = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
        secretKey   = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_SECRET_KEY));
        admId       = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_ID));
        admKey      = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_KEY));
        door1Id     = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));
        door2Id     = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR2_ID));
        softApSsid  = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWSAP_SSID));
        softApPwd   = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWSAP_PWD));
        ipAddress   = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CIPSTA_IP));
        stationSsid = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWJAP_SSID));
        stationPwd  = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWJAP_PWD));
        cwmode      = cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_CWMODE));
        expiredAes  = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES));
        accessPointEnabled = (softApPwd.length() == 0) ? false : true;
        stationEnabled = (stationPwd.length() == 0) ? false : true;

        etLock1Title.setText(lock1Title);
        etLock2Title.setText(lock2Title);
        setTextInfo();

        if(expiredAes == null){
            needSync = false;
        }else{
            needSync = true;
        }



        if(cwmode == SOFTAP){
            rbLockAccessPoint.setChecked(true);
            etLockSsid.setText(softApSsid);
            etLockPwd.setText(softApPwd);
        }else if(cwmode == STATION){
            rbLockStation.setChecked(true);
            etLockSsid.setText(stationSsid);
            etLockPwd.setText(stationPwd);
        }else{
            tvInfo.setText("Error: cwmode invalid value");
        }

        elementsDisableOnStart();




        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonSaveEnableRequest();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        etLock1Title.addTextChangedListener(textWatcher);
        etLock2Title.addTextChangedListener(textWatcher);


        sEdit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wifiSettingsEditEnable(isChecked);
                if(isChecked){
                    bSave.setEnabled(false);
                    setTextInfo();
                    WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = mainWifi.getConnectionInfo();
                    onWifiChanged(wifiInfo);
                }else{
                    buttonSaveEnableRequest();
                    if(rbLockAccessPoint.isChecked()){
                        etLockSsid.setText(softApSsid);
                        etLockPwd.setText(softApPwd);
                    }else if(rbLockStation.isChecked()){
                        etLockSsid.setText(stationSsid);
                        etLockPwd.setText(stationPwd);
                    }

                }


            }
        });

        rgWiFiSettings.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(!sEdit.isChecked()){
                    buttonSaveEnableRequest();
                    if(rbLockAccessPoint.isChecked()){
                        etLockSsid.setText(softApSsid);
                        etLockPwd.setText(softApPwd);
                    }else if(rbLockStation.isChecked()){
                        etLockSsid.setText(stationSsid);
                        etLockPwd.setText(stationPwd);
                    }
                }

            }
        });

        bSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accessPointMode = false;
                aesSyncMode = true;
                if(cwmode == SOFTAP){
                    deviceIpAddress = ESP_AP_STATIC_IP;
                }else if(cwmode == STATION){
                    deviceIpAddress = ipAddress;
                }

                userAes = new int[expiredAes.length];
                userData = new int[expiredAes.length];
                for(int i = 0; i < expiredAes.length; i++){
                    userAes[i] = expiredAes[i] & 0xff;
                    userData[i] = userAes[i] + 1;
                }

                networktask = new NetworkTask(); //New instance of NetworkTask
                Bundle myBundle = espStartSynchronization();
                networktask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myBundle);


            }
        });

        bWiFiSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rbLockAccessPoint.isChecked()){
                    deviceIpAddress = ESP_AP_STATIC_IP;
                    accessPointMode = true;
                    aesSyncMode = false;
                    networktask = new NetworkTask(); //New instance of NetworkTask
                    Bundle myBundle = espStartSynchronization();
                    networktask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myBundle);


                }else if(rbLockStation.isChecked()){
                    byte[] ssid = etLockSsid.getTag() == null ? ByteUtil.getBytesByString(etLockSsid.getText().toString())
                            : (byte[]) etLockSsid.getTag();
                    byte[] password = ByteUtil.getBytesByString(etLockPwd.getText().toString());
                    byte [] bssid = EspNetUtil.parseBssid2bytes(mApBssidTV);
                    byte[] deviceCount = "1".getBytes();
                    if(mTask != null) {
                        mTask.cancelEsptouch();
                    }
                    mTask = new EsptouchAsyncTask(LockInfoActivity.this);
                    mTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,ssid, bssid, password, deviceCount);


                }

            }
        });

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues cv = new ContentValues();
                boolean dataAdded = false;
                if(rbLockAccessPoint.isChecked()){
                    cv.put(LockDataContract.COLUMN_CWMODE, SOFTAP);
                    dataAdded = true;
                }
                if(rbLockStation.isChecked()){
                    cv.put(LockDataContract.COLUMN_CWMODE, STATION);
                    dataAdded = true;
                }
                if(!etLock1Title.getText().toString().contentEquals(lock1Title)){
                    cv.put(LockDataContract.COLUMN_LOCK1_TITLE, etLock1Title.getText().toString());
                    dataAdded = true;
                }
                if(!etLock2Title.getText().toString().contentEquals(lock2Title)){
                    cv.put(LockDataContract.COLUMN_LOCK2_TITLE, etLock2Title.getText().toString());
                    dataAdded = true;
                }
                if(dataAdded){
                    mDb.update(LockDataContract.TABLE_NAME_LOCK_DATA, cv,
                            LockDataContract._ID + "= ?", new String[] {String.valueOf(rowId)});

                }
                finish();
            }
        });
    }


    private Bundle espStartSynchronization(){
        Bundle bundle = new Bundle();
        createArray(inputData);
        bundle.putString("port", "48910");
        bundle.putByteArray("syncKey", admKey);

        //lockRowId = mDb.insert(LockDataContract.TABLE_NAME_LOCK_DATA, null, cvLock);
        return bundle;
    }

    private void buttonSaveEnableRequest(){
        if(((rbLockAccessPoint.isChecked() && cwmode == SOFTAP) || (rbLockStation.isChecked() && cwmode == STATION))
                    && etLock1Title.getText().toString().contentEquals(lock1Title)
                    && etLock2Title.getText().toString().contentEquals(lock2Title)) {
            bSave.setEnabled(false);
            setTextInfo();
        }else{
            if((rbLockAccessPoint.isChecked() && cwmode != SOFTAP) || (rbLockStation.isChecked() && cwmode != STATION)) tvInfo.append("\n\nBefore switching make sure you changed lock settings with button double click before saving");
            bSave.setEnabled(true);
        }

    }
    private void setTextInfo(){
        tvInfo.setText(lock1Title + " ID: " + door1Id +
                 "\n" + lock2Title +" ID: " + door2Id
                   + "\nIP address: " + ipAddress);

        if(cwmode == SOFTAP){
            tvInfo.append("\nLock is in access point mode\nLock's SSID: " + softApSsid);
        }else if(cwmode == STATION) {
            tvInfo.append("\nLock is connected to Wi-Fi\nWi-Fi SSID: " + stationSsid);
        }

        if(aesCursor != null && aesCursor.getCount()>0){
            int count = aesCursor.getCount();
            int availableAES = 0;
            aesCursor.moveToPosition(0);
            for(int i = 0; i < count; i++){
                if(aesCursor.getInt(aesCursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY_USED_FLAG)) == 0){
                    availableAES++;
                }
                aesCursor.moveToNext();
            }
            tvInfo.append("\nAll AES keys: " + count);
            tvInfo.append("\nAvailable AES keys: " + (availableAES-1));
            tvInfo.append("\nAES keys in use: " + (count - availableAES));

        }else{
            tvInfo.append("\nNo AES keys found");
        }

        tvInfo.append("\nExpired AES:");
        if(expiredAes == null){
            tvInfo.append(" no");
        }else{
            for(byte b: expiredAes){
                tvInfo.append(" " + String.valueOf(b));
            }
        }


    }

    private void elementsDisableOnStart(){
        progressBar.setEnabled(false);
        progressBar.setVisibility(View.INVISIBLE);
        if(needSync){
            bSync.setEnabled(true);
            bSync.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sync_colored_24dp, 0,0,0);
        }else{
            bSync.setEnabled(false);
            bSync.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sync_disabled_24dp, 0,0,0);
        }
        rbLockAccessPoint.setEnabled(accessPointEnabled);
        rbLockStation.setEnabled(stationEnabled);

        etLockSsid.setEnabled(false);
        etLockPwd.setEnabled(false);
        bWiFiSet.setEnabled(false);
        bWiFiSet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_disabled_24dp, 0,0,0);

        bSave.setEnabled(false);


    }

    private void wifiSettingsEditEnable(boolean state){

        if(state){
            rbLockAccessPoint.setEnabled(true);
            rbLockStation.setEnabled(true);

        }else{
            rbLockAccessPoint.setEnabled(accessPointEnabled);
            rbLockStation.setEnabled(stationEnabled);

        }

        etLockSsid.setEnabled(state);
        etLockPwd.setEnabled(state);

        bWiFiSet.setEnabled(state);
        if(state){
            bWiFiSet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_colored_24dp, 0,0,0);
        }else{
            bWiFiSet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_disabled_24dp, 0,0,0);
        }


    }
    private void progressBarEnable(boolean state){
        progressBar.setEnabled(state);
        etLock1Title.setEnabled(!state);
        etLock2Title.setEnabled(!state);
        rbLockAccessPoint.setEnabled(!state);
        rbLockStation.setEnabled(!state);

        bSync.setEnabled(!state);

        if(sEdit.isChecked()){
            bWiFiSet.setEnabled(!state);
            etLockSsid.setEnabled(!state);
            etLockPwd.setEnabled(!state);
        }




        bSave.setEnabled(false);
        sEdit.setEnabled(!state);
        if(state){
            progressBar.setVisibility(View.VISIBLE);
            bSync.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sync_disabled_24dp, 0,0,0);
            bWiFiSet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_disabled_24dp, 0,0,0);
        }else{
            progressBar.setVisibility(View.INVISIBLE);
            bSync.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sync_colored_24dp, 0,0,0);
            bWiFiSet.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_colored_24dp, 0,0,0);
        }
    }


    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(LockInfoActivity.this, text,
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    private class EsptouchAsyncTask extends AsyncTask<byte[], Void, List<IEsptouchResult>> {
        private WeakReference<LockInfoActivity> mActivity;

        EsptouchAsyncTask(LockInfoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        void cancelEsptouch() {
            cancel(true);

            if (mEsptouchTask != null) {
                mEsptouchTask.interrupt();
            }
        }

        @Override
        protected void onPreExecute() {

            tvInfo.setText("Esptouch is configuring, " + '\n' +
                    "please wait for a moment...");

            progressBarEnable(true);

        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            LockInfoActivity activity = mActivity.get();
            int taskResultCount;
            synchronized (mLock) {
                // !!!NOTICE
                byte[] apSsid = params[0];
                byte[] apBssid = params[1];
                byte[] apPassword = params[2];
                byte[] deviceCountData = params[3];
                taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
                Context context = activity.getApplicationContext();

                mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, null, context);

                mEsptouchTask.setEsptouchListener(activity.myListener);
            }
            return mEsptouchTask.executeForResults(taskResultCount);
        }
        @Override
        protected void onCancelled() {
            progressBarEnable(false);

        }
        @Override
        protected void onPostExecute(List<IEsptouchResult> result) {
            progressBarEnable(false);


            if (result == null) {


                tvInfo.setText("Create Esptouch task failed, the esptouch " + '\n' +
                        "port could be used by other thread");
                return;
            }
            IEsptouchResult firstResult = result.get(0);
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled()) {
                int count = 0;
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                final int maxDisplayCount = 5;
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc()) {
                    StringBuilder sb = new StringBuilder();
                    for (IEsptouchResult resultInList : result) {
                        sb.append("Esptouch success, bssid = ")
                                .append(resultInList.getBssid())
                                .append(", InetAddress = ")
                                .append(resultInList.getInetAddress().getHostAddress())
                                .append("\n");
                        deviceIpAddress = resultInList.getInetAddress().getHostAddress();
                        //spEditor.putString("deviceIpAddress", deviceIpAddress);
                        //spEditor.commit();

                        count++;
                        if (count >= maxDisplayCount) {
                            break;
                        }
                    }
//                    if (count < result.size()) {
//                        sb.append("\nthere's ")
//                                .append(result.size() - count)
//                                .append(" more result(s) without showing\n");
//                    }
                    //tvInfo.setText("Device IP: " + deviceIpAddress);
                    saveNewWiFiToDataBase(deviceIpAddress, STATION);
                    setTextInfo();


                } else {
                    deviceIpAddress = "";
                    tvInfo.setText("Esptouch fail");
                }
            }
        }
    }


    private void saveNewWiFiToDataBase(String ip, int cwm){
        ContentValues cv = new ContentValues();

        if(cwm == STATION){
            ipAddress   = ip;
            stationSsid = etLockSsid.getText().toString();
            stationPwd  = etLockPwd.getText().toString();
            cwmode = cwm;

            cv.put(LockDataContract.COLUMN_CIPSTA_IP, ipAddress);
            cv.put(LockDataContract.COLUMN_CWJAP_SSID, stationSsid);
            cv.put(LockDataContract.COLUMN_CWJAP_PWD, stationPwd);
            cv.put(LockDataContract.COLUMN_CWMODE, cwmode);
            stationEnabled = true;


        }else if(cwm == SOFTAP){
            softApSsid = etLockSsid.getText().toString();
            softApPwd  = etLockPwd.getText().toString();
            cwmode = cwm;

            cv.put(LockDataContract.COLUMN_CWSAP_SSID, softApSsid);
            cv.put(LockDataContract.COLUMN_CWSAP_PWD, softApPwd);
            cv.put(LockDataContract.COLUMN_CWMODE, cwmode);
            accessPointEnabled = true;

        }else{
            return;
        }
        mDb.update(LockDataContract.TABLE_NAME_LOCK_DATA, cv,
                LockDataContract._ID + "= ?", new String[] {String.valueOf(rowId)});

    }
























    byte[]encryptedData = new byte[80];
    byte[]decryptedData = new byte[80];
    byte[] initVectorTX = new byte[16];
    byte[] initVectorRX = new byte[16];

    class NetworkTask extends AsyncTask< Bundle , Bundle , String> {
        private Socket nsocket; //Network Socket
        private String ipaddr;
        private int port = 0;
        byte[] synchroKey = new byte[32];
        byte[] nonce = new byte[16];
        byte[] buffer = new byte[100];
        byte XORcheck = 0;
        byte XORin = 0;
        byte[] doorOneId = new byte[4];
        byte[] doorTwoId = new byte[4];
        private ContentValues cvLock = new ContentValues();
        ArrayList<ContentValues> cvUser = new ArrayList<ContentValues>();
        String lock1Title = etLock1Title.getText().toString();
        String lock2Title = etLock2Title.getText().toString();



        int byteCnt;
        InputStream nis; //Network Input Stream
        OutputStream nos; //Network Output Stream

        byte[] temp = new byte[16];
        int dialogStage;

        @Override
        protected void onPreExecute() {
            tvInfo.setText("Network connection started...");
            cvLock.put(LockDataContract.COLUMN_LOCK1_TITLE, lock1Title);
            cvLock.put(LockDataContract.COLUMN_LOCK2_TITLE, lock2Title);
            progressBarEnable(true);
        }

        @Override
        protected String doInBackground(Bundle... bundle) {
            if(deviceIpAddress.length() == 0){
                return "No IP address";
            }

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(!wifiManager.isWifiEnabled()) {
                return "ERROR 01: turn ON your Wi-Fi adapter";
            }
            ipaddr = deviceIpAddress;
            if(!isConecctedToDevice(ipaddr)){
                return "ERROR 04: device's IP address (" + ipaddr + ") not found";

            }

            port = Integer.parseInt(bundle[0].getString("port"));
            synchroKey = bundle[0].getByteArray("syncKey");
            //    readLock = bundle[0].getBoolean("readLock");

            try {
                nsocket = new Socket(ipaddr, port);
                if (nsocket.isConnected()) {
                    nos = nsocket.getOutputStream();
                    nis = nsocket.getInputStream();
                    dialogStage = SYNC_FIRST;
                    do{
                        encryptedData = prepareData(dialogStage);
                        nos.write(encryptedData);
                        nos.flush();
                        byteCnt = nis.read(buffer, 0, 100); //This is blocking
                        TimeUnit.MILLISECONDS.sleep(200);
                        dialogStage = readData(dialogStage, byteCnt, buffer);

                        if (dialogStage == SYNC_FLOW){
                            for(byte[] bytes: inputData){
                                encryptedData = encrypt(bytes, initVectorTX, synchroKey);
                                System.arraycopy(encryptedData, bytes.length - 16, initVectorRX, 0, 16);//SAVE it to receive message
                                nos.write(encryptedData);
                                nos.flush();
                                byteCnt = nis.read(buffer, 0, 100); //This is blocking
                                if (byteCnt == -1) break;
                                System.arraycopy(buffer, byteCnt - 16, initVectorTX, 0, 16);
                                byte[] dataToDecrypt = new byte[byteCnt];
                                System.arraycopy(buffer, 0, dataToDecrypt, 0, byteCnt);
                                decryptedData = decrypt(dataToDecrypt, initVectorRX, synchroKey);
                                //outputData.add(decryptedData);
                                Bundle outBundle = new Bundle();
                                outBundle.putByteArray("inputData", bytes);
                                outBundle.putByteArray("decryptedData", decryptedData);
                                //String s = new String(decryptedData);//.split("\0")[0];
                                publishProgress(outBundle);
                                TimeUnit.MILLISECONDS.sleep(150);
                            }
                        }
                    }while(dialogStage != SYNC_FLOW);
                    //-------------------------------------------------------------------------

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Synchronization complete";


        }

        public boolean isConecctedToDevice(String ip) {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process ipProcess = runtime.exec("/system/bin/ping -c 1 " + ip);
                int     exitValue = ipProcess.waitFor();
                return (exitValue == 0);
            } catch (IOException e)          { e.printStackTrace(); }
            catch (InterruptedException e) { e.printStackTrace(); }
            return false;
        }
        @Override
        protected void onProgressUpdate(Bundle... b) {
            byte[] requestSent = b[0].getByteArray("inputData").clone();
            byte[] answerReceived = b[0].getByteArray("decryptedData").clone();
            String reqSent = new String(requestSent).split("\0")[0];
            String ansReceived = new String(answerReceived).split("\0")[0];
            byte[] dataToSave = new byte[32];
            int memoryPage = LAST_USER_BOUND+1;
            if(reqSent.contains("rtc") || reqSent.contains("restart") || reqSent.contains("sync stop")){
                tvInfo.setText(reqSent);
            }else{
                tvInfo.setText("\n" + reqSent + " " + ansReceived);
                String page = reqSent.split("\\D+")[1];
                if(!page.isEmpty()) memoryPage = Integer.parseInt(page);
                if(reqSent.contains("set memory")){
                    System.arraycopy(requestSent, 16, dataToSave, 0, dataToSave.length);//S
                }else if(reqSent.contains("get memory")){
                    System.arraycopy(answerReceived, 0, dataToSave, 0, dataToSave.length);//S
                }

                if(memoryPage == CWSAP_SSID_PAGE){
                    String apSsid = new String(dataToSave).split("\0")[0];
                    cvLock.put(LockDataContract.COLUMN_CWSAP_SSID, apSsid);
                }else if(memoryPage == CWSAP_PWD_PAGE){
                    String apPwd = new String(dataToSave).split("\0")[0];
                    cvLock.put(LockDataContract.COLUMN_CWSAP_PWD, apPwd);
                }else if(11 <= memoryPage && memoryPage <= LAST_USER_BOUND){
                    if(memoryPage % 2 == 1){//This is USER AES KEY
                        ContentValues cv = new ContentValues();
                        cv.put(LockDataContract.COLUMN_AES_KEY, dataToSave);
                        cv.put(LockDataContract.COLUMN_AES_KEY_USED_FLAG, 0);
                        cv.put(LockDataContract.COLUMN_AES_DOOR1_ID, Base64.encodeToString(doorOneId, Base64.NO_WRAP));
                        cv.put(LockDataContract.COLUMN_AES_DOOR2_ID, Base64.encodeToString(doorTwoId, Base64.NO_WRAP));
                        cv.put(LockDataContract.COLUMN_AES_MEM_PAGE, memoryPage);
                        cv.put(LockDataContract.COLUMN_LOCK_ROW_ID, rowId);
                        cvUser.add(cv);
                    }
                }

            }
        }
        @Override
        protected void onCancelled() {
            progressBarEnable(false);

        }
        @Override
        protected void onPostExecute(String result) {
            progressBarEnable(false);
            //door1IdString
            if(result.equals("Synchronization complete")) {
                if(accessPointMode){
                    saveNewWiFiToDataBase(deviceIpAddress, SOFTAP);

                }else if(aesSyncMode){
                    for (ContentValues cv : cvUser) {
                        mDb.insert(LockDataContract.TABLE_NAME_AES_DATA, null, cv);
                    }

                    ContentValues contentValues = new ContentValues();
                    contentValues.putNull(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES);

                    mDb.update(LockDataContract.TABLE_NAME_LOCK_DATA, contentValues,
                            LockDataContract._ID + "= ?", new String[] {String.valueOf(rowId)});
                    needSync = false;

                    bSync.setEnabled(false);
                    bSync.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_sync_disabled_24dp, 0,0,0);

                    aesCursor = mDb.query(
                            LockDataContract.TABLE_NAME_AES_DATA,
                            projectionAes,
                            LockDataContract.COLUMN_LOCK_ROW_ID + "= ?",
                            new String[] {String.valueOf(rowId)},
                            null,
                            null,
                            LockDataContract.COLUMN_TIMESTAMP
                    );
                    expiredAes = null;
                }
                setTextInfo();







//                if(accessPointMode){
//                    mDb.update(LockDataContract.TABLE_NAME_LOCK_DATA, cvLock, LockDataContract.COLUMN_DOOR1_ID + "= ?", new String[]{String.valueOf(rowId)});
//                }else{
//                    for (ContentValues cv : cvAes) {
//                        mDb.insert(LockDataContract.TABLE_NAME_AES_DATA, null, cv);
//                    }
//                }
            }
            Toast.makeText(LockInfoActivity.this, result, Toast.LENGTH_LONG).show();
            // screenComponentsDisableOnStart(true);
            try {
                nos.close();
                nis.close();
                nsocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

           // Intent intent = new Intent();
           // setResult(RESULT_OK, intent);
           // finish();

        }

        private byte[] prepareData(int dialogStage){

            switch (dialogStage){
                case SYNC_FIRST:
                    SecureRandom random = new SecureRandom();
                    byte[] encryptedFirst = new byte[17];
                    byte[] encrypted;
                    random.nextBytes(nonce);//random generating
                    XORcheck = XORcalc(nonce);//here we calculate xor of this random
                    encrypted = encrypt(nonce, synchroKey);
                    System.arraycopy(encrypted, 0, initVectorRX, 0, 16);//SAVE it to receive message
                    System.arraycopy(encrypted, 0, encryptedFirst, 0, 16);//SAVE it to receive message
                    //encryptedFirst[16] = 0;//TODO using of admin and sync key need to be handled
                    encryptedFirst[16] = 1;//additional byte to make 17 bytes32 of data// use adminKey
                    return encryptedFirst;

                case SYNC_XOR:
                    byte[] plaintext = new byte[16];
                    System.arraycopy("sync start".getBytes(), 0, plaintext, 0, "sync start".length());
                    System.arraycopy(decryptedData, 0, temp, 0, 16);
                    plaintext[15] = XORcalc(temp);
                    encryptedData = encrypt(plaintext, initVectorTX, synchroKey);
                    System.arraycopy(encryptedData, 0, initVectorRX, 0, 16);//SAVE it to receive message
                    return encryptedData;

                case SYNC_FLOW:

                    return null;
                case FINISH:
                    return null;
            }
            return null;

        }

        private int readData(int dialogStage, int byteCnt, byte[] buffer){
            if (byteCnt == -1) return FINISH;
            switch (dialogStage){
                case SYNC_FIRST:
                    System.arraycopy(buffer, 0, initVectorTX, 0, 16);
                    System.arraycopy(buffer, 0, temp, 0, 16);
                    decryptedData = decrypt(temp, initVectorRX, synchroKey);
                    XORin = decryptedData[15];
                    if (XORcheck == XORin) return SYNC_XOR;
                    else return FINISH;

                case SYNC_XOR:
                    System.arraycopy(buffer, 0, initVectorTX, 0, 16);
                    System.arraycopy(buffer, 0, temp, 0, byteCnt);
                    decryptedData = decrypt(temp, initVectorRX, synchroKey);
                    if(new String(decryptedData).contains("SYNC START OK"))return SYNC_FLOW;
                    else return FINISH;
                case SYNC_FLOW:

                    return SYNC_FLOW;
                case FINISH:
                    return FINISH;


            }
            return 0;
        }
    }
    private byte[] getRtc(){
        Calendar c = Calendar.getInstance();
        int day    = c.get(Calendar.DAY_OF_MONTH);
        int month  = c.get(Calendar.MONTH);
        int year   = c.get(Calendar.YEAR);
        int hour   = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        return timeIntToHex(hour, minute, day, month, year);
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
    private byte[] encrypt(byte[] data, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            SecretKeySpec k = new SecretKeySpec(key, "AES_256");
            cipher.init(Cipher.ENCRYPT_MODE, k);
            byte[] cipherByte = cipher.doFinal(data);
            return cipherByte;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public byte[] decrypt(byte[] data, byte[] initVector, byte[] key) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec k = new SecretKeySpec(key, "AES_256");
            cipher.init(Cipher.DECRYPT_MODE, k, iv);
            byte[] plainText = cipher.doFinal(data);
            return plainText;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    public byte XORcalc(byte[] input){
        byte output = input[0];
        for(int i=1; i<input.length; i++) output = (byte) (output ^ input[i]);
        return output;
    }
    void createArray (ArrayList<byte[]> array){
        byte[] data48 = new byte[48];
        byte[] data16 = new byte[16];
        String mem;
        String text;
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[32];
        // SET RTC
        Arrays.fill(data16, (byte)0);
        mem = new String("set rtc");
        System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
        byte[] rtc = getRtc();//{0x18, 0x10, 0x20, 0x21, 0x28, 0x30};
        System.arraycopy(rtc, 0, data16, mem.length() + 1, rtc.length);//S
        array.add(data16.clone());

        if(accessPointMode == true){
            Arrays.fill(data48, (byte) 0);
            mem = new String("set memory 3");
            text = etLockSsid.getText().toString();
            System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
            System.arraycopy(text.getBytes(), 0, data48, 16, text.length());//S
            array.add(data48.clone());

            //------SET CWPSAP_PWD, MEM PAGE 4
            Arrays.fill(data48, (byte)0);
            mem = new String("set memory 4");
            text = etLockPwd.getText().toString();
            System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
            System.arraycopy(text.getBytes(), 0, data48, 16, text.length());//S
            array.add(data48.clone());

            Arrays.fill(data16, (byte)0);
            mem = new String("restart");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

        }else if(aesSyncMode == true){
            // ---- SET USER_AES 11 - 19: [11, 13, 15, 17, 19]
            for(int pages : userAes){//LAST_USER_BOUND = 20;
                Arrays.fill(data48, (byte)0);
                mem = new String("set memory " + pages);
                System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
                byte[] rnd16 = new byte[16];
                random.nextBytes(rnd16);//random generating
                System.arraycopy(rnd16, 0, data48, 16, rnd16.length);//S
                System.arraycopy(rnd16, 0, data48, 32, rnd16.length);//S
                array.add(data48.clone());
            }
            // ---- SET USER_DATA 12 - 20
            for(int pages : userData){//LAST_USER_BOUND = 20;
                Arrays.fill(data48, (byte)0);
                mem = new String("set memory " + pages);
                System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
                byte[] rnd = new byte[28];
                random.nextBytes(rnd);//random generating
                System.arraycopy(rnd, 0, data48, 20, rnd.length);//S
                array.add(data48.clone());
            }

            //------STOP SYNCHRONIZATION IN STATION-----
            Arrays.fill(data16, (byte)0);
            mem = new String("sync stop");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());
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

}
