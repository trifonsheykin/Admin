package com.smartlocks.trifonsheykin.admin.Lock;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.util.ByteUtil;
import com.espressif.iot.esptouch.util.EspNetUtil;
import com.smartlocks.trifonsheykin.admin.DbHelper;
import com.smartlocks.trifonsheykin.admin.LockDataContract;
import com.smartlocks.trifonsheykin.admin.QrReadActivity;
import com.smartlocks.trifonsheykin.admin.R;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchTask;

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


public class LockEditActivity extends AppCompatActivity implements View.OnClickListener{


    private static final int FINISH = 0;
    private static final int SYNC_FIRST = 1;
    private static final int SYNC_XOR = 2;
    private static final int SYNC_FLOW = 3;
    private static final int ERROR = 4;
    private static final int LAST_USER_BOUND = 212;

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

    private static final String ESP_AP_STATIC_IP = "192.168.4.1";

    private IEsptouchTask mEsptouchTask;
    private static final Object mLock = new Object();
    private byte[] syncKey =  new byte[32];


    private ProgressBar pbStatus;
    private EditText etLock1Title;
    private EditText etLock2Title;
    private TextView tvWiFiSettings;
    private TextView tvStatus;
    private RadioGroup radioGroup;
    private RadioButton rbAccessPoint;
    private RadioButton rbStation;
    private EditText etApSsid;
    private String mApBssidTV;
    private EditText etApPassword;
    private Button bSynchronize;
    private Button bConnect;
    private long lockRowId;
    private String deviceIpAddress;
    private String door1IdString;

    private TextWatcher textWatcher;
    private SQLiteDatabase mDb;
    ArrayList<byte[]> inputData = new ArrayList<byte[]>();

 //   private SharedPreferences sharedPreferences;
 //   private SharedPreferences.Editor spEditor;
    private boolean spShowSavedData;
    private boolean ipAddressReceived;





    private IEsptouchListener myListener = new IEsptouchListener() {

        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            onEsptoucResultAddedPerform(result);
        }
    };

    private EsptouchAsyncTask4 mTask;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
//                    WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
//                    onWifiChanged(wifiInfo);

                    WifiManager mainWifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = mainWifi.getConnectionInfo();
                    onWifiChanged(wifiInfo);
                    break;
            }
        }
    };




    @Override
    protected void onStart() {
        super.onStart();
    //    spShowSavedData = sharedPreferences.getBoolean("spShowSavedData", false);
    //    deviceIpAddress = "";
    //    if(spShowSavedData){
      //      etLock1Title.setText(sharedPreferences.getString("lock1Title", ""));
      //      etLock2Title.setText(sharedPreferences.getString("lock2Title", ""));
//            if(sharedPreferences.getBoolean("accessPoint", true)){
//                rbAccessPoint.setChecked(true);
//                rbStation.setChecked(false);
//                deviceIpAddress = ESP_AP_STATIC_IP;
//
//            }else{
//                rbAccessPoint.setChecked(false);
//                rbStation.setChecked(true);
//
//            }
//            etApPassword.setText(sharedPreferences.getString("password", ""));
      //  }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_edit);
        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //spEditor = sharedPreferences.edit();
        ipAddressReceived = false;
        tvStatus = findViewById(R.id.tv_status);
        tvWiFiSettings = findViewById(R.id.tv_wifi_settings);

        pbStatus = findViewById(R.id.progressBar);
        etApSsid = findViewById(R.id.ap_ssid_edit);
        etApPassword = findViewById(R.id.ap_password_edit);
        radioGroup = findViewById(R.id.radioGroup);
        rbAccessPoint = findViewById(R.id.rb_access_point);
        rbStation = findViewById(R.id.rb_station);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                buttonConnectEnableRequest();
                buttonSyncEnableRequest();
                if(rbAccessPoint.isChecked()){

                    //spEditor.putString("deviceIpAddress", deviceIpAddress);
                    //spEditor.commit();
                    deviceIpAddress = ESP_AP_STATIC_IP;
                }else if(rbStation.isChecked()){
                    //deviceIpAddress = sharedPreferences.getString("deviceIpAddress", "");
                }
            }
        });
        etLock1Title = findViewById(R.id.et_lock1_title);
        etLock2Title = findViewById(R.id.et_lock2_title);

        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonSyncEnableRequest();
                if(etLock1Title.getText().toString().length() !=0 && etLock2Title.isEnabled() == false){
                    etLock2Title.setEnabled(true);
                }else if(etLock2Title.getText().toString().length() != 0 && tvWiFiSettings.isEnabled() == false){
                    tvWiFiSettings.setEnabled(true);
                    etApSsid.setEnabled(true);
                    etApPassword.setEnabled(true);
                    rbAccessPoint.setEnabled(true);
                    rbStation.setEnabled(true);
                    rbAccessPoint.setChecked(true);
                }else{
                    buttonConnectEnableRequest();
                }



            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        etLock1Title.addTextChangedListener(textWatcher);
        etLock2Title.addTextChangedListener(textWatcher);
        etApSsid.addTextChangedListener(textWatcher);
        etApPassword.addTextChangedListener(textWatcher);

        bSynchronize = findViewById(R.id.confirm_btn);
        bSynchronize.setOnClickListener(this);
        bConnect = findViewById(R.id.b_connect);
        bConnect.setOnClickListener(this);

        screenComponentsDisableOnStart();

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();


        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
    }


    private void buttonSyncEnableRequest(){
        if(etLock1Title.getText().toString().length() == 0
                || etLock2Title.getText().toString().length() == 0
                || etApSsid.getText().toString().length() == 0
                || etApPassword.getText().toString().length() == 0
                || (rbStation.isChecked() && ipAddressReceived == false)){
            bSynchronize.setEnabled(false);
        }else{
            bSynchronize.setEnabled(true);
        }
    }
    private void buttonConnectEnableRequest(){
        if(etApSsid.getText().toString().length() == 0 || etApPassword.getText().toString().length() == 0 || rbAccessPoint.isChecked()){
            bConnect.setEnabled(false);
            bConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_disabled_24dp, 0,0,0);
            tvStatus.setText("");
        }else{
            bConnect.setEnabled(true);
            bConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_colored_24dp, 0,0,0);
            tvStatus.setText("Press MODE button on device\nbefore CONNECT");
        }

    }



    @Override
    protected void onStop() {
        super.onStop();
        //spEditor.putString("lock1Title", etLock1Title.getText().toString());
        //spEditor.putString("lock2Title", etLock2Title.getText().toString());
        //if(rbAccessPoint.isChecked()) spEditor.putBoolean("accessPoint",true);
        //else spEditor.putBoolean("accessPoint",false);
        //spEditor.putString("password", etApPassword.getText().toString());
        //spEditor.commit();
    }

    private void screenComponentsDisableOnStart(){
        etLock1Title.setEnabled(true);
        etLock2Title.setEnabled(false);
        tvWiFiSettings.setEnabled(false);
        rbAccessPoint.setEnabled(false);
        rbStation.setEnabled(false);
        etApSsid.setEnabled(false);
        etApPassword.setEnabled(false);
        pbStatus.setVisibility(View.INVISIBLE);
        tvStatus.setText("");
        bConnect.setEnabled(false);
        bConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_disabled_24dp, 0,0,0);
        bSynchronize.setEnabled(false);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }
    private void onWifiChanged(WifiInfo info) {
        etApPassword.setText("");
        //spEditor.putString("password", etApPassword.getText().toString());
        //spEditor.commit();
        if (info == null) {
            etApSsid.setText("");
            etApSsid.setTag(null);
            mApBssidTV = "";
          //  bSynchronize.setEnabled(false);
            bSynchronize.setTag(null);

            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
                new AlertDialog.Builder(LockEditActivity.this)
                        .setMessage("Wifi disconnected or changed")
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        } else {
            String ssid = info.getSSID();
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }

            etApSsid.setText(ssid);
            etApSsid.setTag(ByteUtil.getBytesByString(ssid));
            byte[] ssidOriginalData = EspUtils.getOriginalSsidBytes(info);
            etApSsid.setTag(ssidOriginalData);

            String bssid = info.getBSSID();
            mApBssidTV = bssid;

    //        bSynchronize.setEnabled(true);
            bSynchronize.setTag(Boolean.FALSE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int frequence = info.getFrequency();
                if (frequence > 4900 && frequence < 5900) {
                    // Connected 5G wifi. Device does not support 5G
                    bSynchronize.setTag(Boolean.TRUE);
                }
            }
        }
    }
    @Override
    public void onClick(View v) {
        if (v == bSynchronize) {
            Intent intent= new Intent(LockEditActivity.this, QrReadActivity.class);
            startActivityForResult(intent, SOFTAP);//
//            syncKey = ByteUtil.getBytesByString("12345098764444422222777771111100");
//            NetworkTask networktask = new NetworkTask(); //New instance of NetworkTask
//            Bundle myBundle = espStartSynchronization();
//            networktask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myBundle);

        }else if(v == bConnect){
            if ((Boolean) bSynchronize.getTag()) {
                Toast.makeText(this, R.string.wifi_5g_message, Toast.LENGTH_SHORT).show();
                return;
            }else{
               // spEditor.putBoolean("spShowSavedData", true);
               // spEditor.commit();
                byte[] ssid = etApSsid.getTag() == null ? ByteUtil.getBytesByString(etApSsid.getText().toString())
                            : (byte[]) etApSsid.getTag();
                byte[] password = ByteUtil.getBytesByString(etApPassword.getText().toString());
                byte [] bssid = EspNetUtil.parseBssid2bytes(mApBssidTV);
                byte[] deviceCount = "1".getBytes();
                if(mTask != null) {
                    mTask.cancelEsptouch();
                }
                mTask = new EsptouchAsyncTask4(this);
                mTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,ssid, bssid, password, deviceCount);
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            String result = data.getStringExtra("result");
            if(result.length() != 44){
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Sync key not found");
            }else{
                syncKey = Base64.decode(result, Base64.DEFAULT);
                if(rbAccessPoint.isChecked())deviceIpAddress = ESP_AP_STATIC_IP;
                NetworkTask networktask = new NetworkTask(); //New instance of NetworkTask
                Bundle myBundle = espStartSynchronization();
                networktask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myBundle);
            }
        }else{
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("Sync key not found");
        }
    }
    private Bundle espStartSynchronization(){
        Bundle bundle = new Bundle();
        createArray(inputData);
        bundle.putString("port", "48910");
        bundle.putByteArray("syncKey", syncKey);

        //lockRowId = mDb.insert(LockDataContract.TABLE_NAME_LOCK_DATA, null, cvLock);
        return bundle;
    }

    private void progressBarEnable(boolean state){
        pbStatus.setEnabled(state);
        etLock1Title.setEnabled(!state);
        etLock2Title.setEnabled(!state);
        tvWiFiSettings.setEnabled(!state);
        rbAccessPoint.setEnabled(!state);
        rbStation.setEnabled(!state);
        etApSsid.setEnabled(!state);
        etApPassword.setEnabled(!state);
        bConnect.setEnabled(!state);
        bSynchronize.setEnabled(!state);
        if(state){
            pbStatus.setVisibility(View.VISIBLE);
            bConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_disabled_24dp, 0,0,0);
        }else{
            pbStatus.setVisibility(View.INVISIBLE);
            bConnect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi_colored_24dp, 0,0,0);
        }


    }

    private void onEsptoucResultAddedPerform(final IEsptouchResult result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = result.getBssid() + " is connected to the wifi";
                Toast.makeText(LockEditActivity.this, text,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
    private class EsptouchAsyncTask4 extends AsyncTask<byte[], Void, List<IEsptouchResult>> {
        private WeakReference<LockEditActivity> mActivity;

        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running




        EsptouchAsyncTask4(LockEditActivity activity) {
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

            tvStatus.setText("Esptouch is configuring, " + '\n' +
                    "please wait for a moment...");


            progressBarEnable(true);



        }

        @Override
        protected List<IEsptouchResult> doInBackground(byte[]... params) {
            LockEditActivity activity = mActivity.get();
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


                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("Create Esptouch task failed, the esptouch " + '\n' +
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
                    tvStatus.setText("Device IP: " + deviceIpAddress);
                    ipAddressReceived = true;
                    buttonSyncEnableRequest();

                } else {
                    deviceIpAddress = "";
                    tvStatus.setText("Esptouch fail");
                }
                tvStatus.setVisibility(View.VISIBLE);

            }


        }
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
        ArrayList<ContentValues> cvAes = new ArrayList<ContentValues>();
        String lock1Title = etLock1Title.getText().toString();
        String lock2Title = etLock2Title.getText().toString();




        int byteCnt;
        InputStream nis; //Network Input Stream
        OutputStream nos; //Network Output Stream

        byte[] temp = new byte[16];
        int dialogStage;
        int memPages = 0;
        @Override
        protected void onPreExecute() {
            tvStatus.setText("Network connection started...");
            cvLock.put(LockDataContract.COLUMN_LOCK1_TITLE, lock1Title);
            cvLock.put(LockDataContract.COLUMN_LOCK2_TITLE, lock2Title);
            memPages = 0;
            progressBarEnable(true);

        }

        @Override
        protected String doInBackground(Bundle... bundle) {
            if(deviceIpAddress.length() == 0){
                return "No IP address";
            }
            ipaddr = deviceIpAddress;

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
                        if(dialogStage == ERROR) break;
                    }while(dialogStage != SYNC_FLOW);
                    //-------------------------------------------------------------------------

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            if(dialogStage == ERROR){
                return "Synchronization error, check sync key";
            }
            return "Synchronization complete";




        }




        @Override
        protected void onProgressUpdate(Bundle... b) {
            byte[] requestSent = b[0].getByteArray("inputData").clone();
            byte[] answerReceived = b[0].getByteArray("decryptedData").clone();
            String reqSent = new String(requestSent).split("\0")[0];
            byte[] dataToSave = new byte[32];
            int memoryPage = LAST_USER_BOUND+1;
            memPages++;
            if(reqSent.contains("rtc") || reqSent.contains("restart") || reqSent.contains("sync stop")){
                tvStatus.setText(reqSent + "\n" + memPages + " of 214");
            }else{
                tvStatus.setText(reqSent + "\n" + memPages + " of 214");
                String page = reqSent.split("\\D+")[1];
                if(!page.isEmpty()) memoryPage = Integer.parseInt(page);
                if(reqSent.contains("set memory")){
                    System.arraycopy(requestSent, 16, dataToSave, 0, dataToSave.length);//S
                }else if(reqSent.contains("get memory")){
                    System.arraycopy(answerReceived, 0, dataToSave, 0, dataToSave.length);//S
                }
                if(memoryPage == DOORID_PAGE){
                    System.arraycopy(dataToSave, 0, doorOneId, 0, doorOneId.length);//S
                    System.arraycopy(dataToSave, 4, doorTwoId, 0, doorTwoId.length);//S
                    door1IdString = Base64.encodeToString(doorOneId, Base64.NO_WRAP);
                    cvLock.put(LockDataContract.COLUMN_DOOR1_ID, Base64.encodeToString(doorOneId, Base64.NO_WRAP));
                    cvLock.put(LockDataContract.COLUMN_DOOR2_ID, Base64.encodeToString(doorTwoId, Base64.NO_WRAP));
                }else if(memoryPage == CWMODE_PAGE){
                    cvLock.put(LockDataContract.COLUMN_CWMODE, dataToSave[0]);
                    cvLock.putNull(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES);
                }else if(memoryPage == CWSAP_SSID_PAGE){
                    String apSsid = new String(dataToSave).split("\0")[0];
                    cvLock.put(LockDataContract.COLUMN_CWSAP_SSID, apSsid);
                }else if(memoryPage == CWSAP_PWD_PAGE){
                    String apPwd = new String(dataToSave).split("\0")[0];
                    cvLock.put(LockDataContract.COLUMN_CWSAP_PWD, apPwd);
                }else if(memoryPage == CWJAP_SSID_PAGE){
                    String staSsid = new String(dataToSave).split("\0")[0];
                    cvLock.put(LockDataContract.COLUMN_CWJAP_SSID, staSsid);
                }else if(memoryPage == CWJAP_PWD_PAGE){
                    String staPwd = new String(dataToSave).split("\0")[0];
                    cvLock.put(LockDataContract.COLUMN_CWJAP_PWD, staPwd);
                }else if(memoryPage == SECRET_KEY_PAGE){
                    cvLock.put(LockDataContract.COLUMN_SECRET_KEY, dataToSave);
                }else if(memoryPage == CIPIP_PAGE){
                    String ipString = new String(dataToSave).split("\0")[0];
                    if(rbAccessPoint.isChecked())ipString = ESP_AP_STATIC_IP;
                    cvLock.put(LockDataContract.COLUMN_CIPSTA_IP, ipString);
                }else if(memoryPage == ADM_KEY_PAGE){
                    cvLock.put(LockDataContract.COLUMN_ADM_KEY, dataToSave);
                }else if(memoryPage == ADM_ID_PAGE){
                    byte[] aId = new byte[4];
                    System.arraycopy(dataToSave, 0, aId, 0, aId.length);//S
                    cvLock.put(LockDataContract.COLUMN_ADM_ID, aId);
                }else if(11 <= memoryPage && memoryPage <= LAST_USER_BOUND){
                    if(memoryPage % 2 == 1){//This is USER AES KEY
                        ContentValues cv = new ContentValues();
                        cv.put(LockDataContract.COLUMN_AES_KEY, dataToSave);
                        cv.put(LockDataContract.COLUMN_AES_KEY_USED_FLAG, 0);
                        cv.put(LockDataContract.COLUMN_AES_DOOR1_ID, Base64.encodeToString(doorOneId, Base64.NO_WRAP));
                        cv.put(LockDataContract.COLUMN_AES_DOOR2_ID, Base64.encodeToString(doorTwoId, Base64.NO_WRAP));
                        cv.put(LockDataContract.COLUMN_AES_MEM_PAGE, memoryPage);
                        cvAes.add(cv);
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
            Toast.makeText(LockEditActivity.this, result, Toast.LENGTH_LONG).show();
            //door1IdString
            if(result.equals("Synchronization complete")) {
                mDb.delete(LockDataContract.TABLE_NAME_LOCK_DATA, LockDataContract.COLUMN_DOOR1_ID + "= ?", new String[]{door1IdString});
                mDb.delete(LockDataContract.TABLE_NAME_AES_DATA, LockDataContract.COLUMN_AES_DOOR1_ID + "= ?", new String[]{door1IdString});

                long row = mDb.insert(LockDataContract.TABLE_NAME_LOCK_DATA, null, cvLock);
                for (ContentValues cv : cvAes) {
                    cv.put(LockDataContract.COLUMN_LOCK_ROW_ID, row);
                    mDb.insert(LockDataContract.TABLE_NAME_AES_DATA, null, cv);
                }
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();

            }
           // screenComponentsDisableOnStart(true);
            try {
                nos.close();
                nis.close();
                nsocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


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
                    encryptedFirst[16] = 0;//TODO using of admin and sync key need to be handled
                    //else encryptedFirst[16] = 1;//additional byte to make 17 bytes32 of data// use adminKey
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
                    else {
                       return ERROR;
                    }

                case SYNC_XOR:
                    System.arraycopy(buffer, 0, initVectorTX, 0, 16);
                    System.arraycopy(buffer, 0, temp, 0, byteCnt);
                    decryptedData = decrypt(temp, initVectorRX, synchroKey);
                    if(new String(decryptedData).contains("SYNC START OK"))return SYNC_FLOW;
                    else return ERROR;
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

            //------------------------------GET and SET MEMORY: GET DOOR ID, CWMODE
            //-----GET DOORID MEM 1
            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 1");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

            //-----GET CWMODE MEM 2
            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 2");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

            //-----------------------------------SET MEMORY---------------------------
            //------SET CWPSAP_SSID, MEM PAGE 3
        if(rbAccessPoint.isChecked()){
            Arrays.fill(data48, (byte) 0);
            mem = new String("set memory 3");
            text = etApSsid.getText().toString();
            System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
            System.arraycopy(text.getBytes(), 0, data48, 16, text.length());//S
            array.add(data48.clone());

            //------SET CWPSAP_PWD, MEM PAGE 4
            Arrays.fill(data48, (byte)0);
            mem = new String("set memory 4");
            text = etApPassword.getText().toString();
            System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
            System.arraycopy(text.getBytes(), 0, data48, 16, text.length());//S
            array.add(data48.clone());
        }else{ //-----------------------------------GET MEMORY---------------------------
            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 3");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 4");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());
        }


            //-----------------------------------GET MEMORY----------------------------

            //-----GET AP ssid in station mode MEM 5
            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 5");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

            //-----GET AP password in station mode MEM 6
            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 6");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

            //-----GET station IP address MEM 8
            Arrays.fill(data16, (byte)0);
            mem = new String("get memory 8");
            System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
            array.add(data16.clone());

            //---------------------------------SET MEMORY-------------------------
            //------SET SECRET KEY 7
            // ----- SET ADM KEY 9
            for(int pages = 7; pages <= 9; pages = pages + 2){
                Arrays.fill(data48, (byte)0);
                mem = new String("set memory " + pages);
                System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
                random.nextBytes(randomBytes);//random generating
                System.arraycopy(randomBytes, 0, data48, 16, randomBytes.length);//S
                array.add(data48.clone());
            }

            // -------SET ADM ID 10
            Arrays.fill(data48, (byte)0);
            mem = new String("set memory 10");
            System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
            byte[] newAdmId = new byte[4];
            random.nextBytes(newAdmId);//random generating
            System.arraycopy(newAdmId, 0, data48, 16, newAdmId.length);//S
            array.add(data48.clone());

            // ---- SET USER_AES 11 - 19: [11, 13, 15, 17, 19]
            for(int pages = 11; pages < LAST_USER_BOUND; pages = pages + 2){//LAST_USER_BOUND = 20;
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
            for(int pages = 12; pages <= LAST_USER_BOUND; pages = pages + 2){//LAST_USER_BOUND = 20;
                Arrays.fill(data48, (byte)0);
                mem = new String("set memory " + pages);
                System.arraycopy(mem.getBytes(), 0, data48, 0, mem.length());//S
                byte[] rnd = new byte[28];
                random.nextBytes(rnd);//random generating
                System.arraycopy(rnd, 0, data48, 20, rnd.length);//S
                array.add(data48.clone());
            }

            if(rbAccessPoint.isChecked()){
                // RESTART WE IN AP MODE
                Arrays.fill(data16, (byte)0);
                mem = new String("restart");
                System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
                array.add(data16.clone());
            }else{
                //------STOP SYNCHRONIZATION IN STATION-----
                Arrays.fill(data16, (byte)0);
                mem = new String("sync stop");
                System.arraycopy(mem.getBytes(), 0, data16, 0, mem.length());//S
                array.add(data16.clone());
            }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //spEditor.putBoolean("spShowSavedData", false);
        //spEditor.commit();

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
