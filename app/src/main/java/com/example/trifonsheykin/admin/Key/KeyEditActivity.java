package com.example.trifonsheykin.admin.Key;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.trifonsheykin.admin.DbHelper;
import com.example.trifonsheykin.admin.Lock.LockSelectActivity;
import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyEditActivity extends AppCompatActivity implements View.OnClickListener{


    private Button bChooseLock;
    private TextView tvLockStatus;
    private Button bReadKey;
    private Button bPlayKey;
    private Button bSaveKey;
    private EditText etKeyTitle;
    private RadioGroup radioGroup;
    private RadioButton rbDoor1Title;
    private RadioButton rbDoor2Title;
    private boolean keyHasBeenRead;
    private long rowId;
    KeyNetworkTask keyNetworkTask;
    private SQLiteDatabase mDb;
    private Cursor cursor;
    byte[] wiegandRealPass = {0x1a, (byte)0x82, (byte)0xaf, 0x5f, 0x02, 0x00, 0x00, 0x00};//new byte[8];
                           //00011010 10000010 10101111 01011111 00000010
    //byte[] wiegandSecondPass = {0x1a, (byte)0x82, (byte)0xaf, 0x5c, 0x02, 0x00, 0x00, 0x00};//new byte[8];
    byte[] initVectorTX = new byte[16];
    byte[] initVectorRX = new byte[16];
    byte[] txData = new byte[80];
    byte[] decryptedData = new byte[80];

    String[] projection = {
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
    private static final int REQUEST_ROW_ID = 1;

    private static final int CHOOSE_LOCK = 1;
    private static final int READ_KEY = 2;
    private static final int PLAY_KEY = 3;
    private static final byte ADMIN = 9;
    private static final int CHANNEL_1 = 0;
    private static final int CHANNEL_2 = 1;
    private byte channel = 0;
    private static final int STATION = 1;
    private static final int SOFTAP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_edit);
        keyHasBeenRead = false;
        bChooseLock  = findViewById(R.id.b_choose_lock);
        tvLockStatus = findViewById(R.id.tv_lock_status);
        bReadKey     = findViewById(R.id.b_read_key);
        bPlayKey     = findViewById(R.id.b_play_key);
        bSaveKey     = findViewById(R.id.b_save_key);
        etKeyTitle   = findViewById(R.id.et_key_title);
        radioGroup   = findViewById(R.id.rg_lock_titles);
        rbDoor1Title = findViewById(R.id.rb_lock1_title);
        rbDoor2Title = findViewById(R.id.rb_lock2_title);

        disableAllElements();

        etKeyTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() != 0){
                    bChooseLock.setEnabled(true);
                }
                saveButtonEnableRequest();


            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        bChooseLock.setOnClickListener(this);
        bReadKey.setOnClickListener(this);
        bPlayKey.setOnClickListener(this);
        bSaveKey.setOnClickListener(this);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(rbDoor1Title.isChecked()){
                    channel = CHANNEL_1;
                }else if(rbDoor2Title.isChecked()){
                    channel = CHANNEL_2;
                }
            }
        });
        rbDoor1Title.setChecked(true);
        Intent intentThatStartedThisActivity = getIntent();
        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        if (intentThatStartedThisActivity.hasExtra(Intent.EXTRA_TEXT)) {

        }
    }

    private void saveButtonEnableRequest(){
        if((etKeyTitle.getText().toString().length() != 0)&&
               keyHasBeenRead){
            bSaveKey.setEnabled(true);
        }else{
            bSaveKey.setEnabled(false);
        }

    }

    private void disableAllElements(){
        bChooseLock.setEnabled(false);
        bPlayKey.setEnabled(false);
        bReadKey.setEnabled(false);
        bSaveKey.setEnabled(false);
        rbDoor1Title.setEnabled(false);
        rbDoor2Title.setEnabled(false);
        tvLockStatus.setEnabled(false);
        bChooseLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_disabled_24dp, 0,0,0);
        bReadKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file_download_disabled_24dp, 0,0,0);
        bPlayKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_disabled_24dp, 0,0,0);
    }

    private void enableKeyElements(){
        bPlayKey.setEnabled(true);
        bReadKey.setEnabled(true);
        rbDoor1Title.setEnabled(true);
        rbDoor2Title.setEnabled(true);
        bReadKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file_download_colored_24dp, 0,0,0);
        bPlayKey.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_colored_24dp, 0,0,0);


    }


    @Override
    public void onClick(View v) {
        Bundle bundle;// = new Bundle();
        Intent intent;
        switch (v.getId()){
            case R.id.b_choose_lock://ping + xored const key send
                //HERE WE JUMP TO CHOOSE LOCK ACTIVITY

                keyHasBeenRead = false;
                bSaveKey.setEnabled(false);

                Context context = KeyEditActivity.this;
                Class destinationActivity = LockSelectActivity.class;
                intent= new Intent(context, destinationActivity);//
                startActivityForResult(intent, REQUEST_ROW_ID);

                break;

            case R.id.b_read_key:
                keyNetworkTask = new KeyEditActivity.KeyNetworkTask(); //New instance of NetworkTask
                bundle = new Bundle();
                cursor = mDb.query(
                        LockDataContract.TABLE_NAME_LOCK_DATA,
                        projection,
                        LockDataContract._ID + "= ?",
                        new String[] {String.valueOf(rowId)},
                        null,
                        null,
                        LockDataContract.COLUMN_TIMESTAMP
                );
                cursor.moveToPosition(0);
                bundle.putInt("BUTTON", READ_KEY);
                bundle.putByteArray("ADM_KEY", cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_KEY)));
                bundle.putByteArray("ADM_ID", cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_ID)));
                bundle.putInt("CWMODE", cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_CWMODE)));
                bundle.putString("CIPSTA_IP", cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CIPSTA_IP)));
                keyNetworkTask.execute(bundle);
                break;

            case R.id.b_play_key:
                keyNetworkTask = new KeyEditActivity.KeyNetworkTask(); //New instance of NetworkTask
                bundle = new Bundle();
                cursor = mDb.query(
                        LockDataContract.TABLE_NAME_LOCK_DATA,
                        projection,
                        LockDataContract._ID + "= ?",
                        new String[] {String.valueOf(rowId)},
                        null,
                        null,
                        LockDataContract.COLUMN_TIMESTAMP
                );
                cursor.moveToPosition(0);
                bundle.putInt("BUTTON", PLAY_KEY);
                bundle.putByteArray("ADM_KEY", cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_KEY)));
                bundle.putByteArray("ADM_ID", cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_ID)));
                bundle.putInt("CWMODE", cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_CWMODE)));
                bundle.putString("CIPSTA_IP", cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CIPSTA_IP)));
                keyNetworkTask.execute(bundle);

                break;

            case R.id.b_save_key:


                if(etKeyTitle.getText().toString().isEmpty())  tvLockStatus.setText("Key title couldn't be empty");
                else{
                    ContentValues cv = new ContentValues();
                    cv.put(LockDataContract.COLUMN_KEY_NAME, etKeyTitle.getText().toString());
                    cv.put(LockDataContract.COLUMN_KEY_LOCKS, rbDoor1Title.getText().toString() + ", " + rbDoor2Title.getText().toString());
                    cv.put(LockDataContract.COLUMN_KEY_DATA , wiegandRealPass);
                    mDb.insert(LockDataContract.TABLE_NAME_KEY_DATA, null, cv);
                    intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish();
                }




                break;
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ROW_ID){
            rowId = data.getLongExtra("rowId", -1);//intent.putExtra("rowId", id);

            if(rowId == -1) tvLockStatus.setText("ERROR ROW ID");
            else{
                Cursor cursor = mDb.query(
                        LockDataContract.TABLE_NAME_LOCK_DATA,
                        projection,
                        LockDataContract._ID + "= ?",
                        new String[] {String.valueOf(rowId)},
                        null,
                        null,
                        LockDataContract.COLUMN_TIMESTAMP
                );
                cursor.moveToPosition(0);
                String lock1Title = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
                String lock2Title = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
                rbDoor1Title.setText(lock1Title);
                rbDoor2Title.setText(lock2Title);
                tvLockStatus.setEnabled(true);
                bChooseLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_colored_24dp, 0,0,0);
                /**
                 * we need in bundle:
                 * CWMODE to decide usage of ip address to send
                 * CWJAP_SSID to check wifi network
                 * CIPSTA_IP to use it if we in Station mode
                 * CONST_KEY to make xor byte[]
                 * */

                keyNetworkTask = new KeyEditActivity.KeyNetworkTask(); //New instance of NetworkTask
                Bundle bundle = new Bundle();
                bundle.putInt("BUTTON", CHOOSE_LOCK);
                //TODO Add channel selection into bundle
                bundle.putByteArray("ADM_KEY", cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_KEY)));
                bundle.putByteArray("ADM_ID", cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_ADM_ID)));
                bundle.putInt("CWMODE", cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_CWMODE)));
                bundle.putString("CWJAP_SSID", cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWJAP_SSID)));
                bundle.putString("CIPSTA_IP", cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CIPSTA_IP)));
                keyNetworkTask.execute(bundle);//networktask.execute(bundle);
            }
        }
    }
    private byte XORcalc(byte[] input){
        byte output = input[0];
        for(int i=1; i<input.length; i++) output = (byte) (output ^ input[i]);
        return output;
    }

    /**
     * here we perform:
     * 1. Lock accessibility check (send: ping receive: ping ok)
     * 2. Send request for pass data
     * 3. Play pass data
     * */
    class KeyNetworkTask extends AsyncTask<Bundle, String, String> {
        private Socket nsocket; //Network Socket
        private String ipaddr;
        private int cwmode;
        private String ssid;
        private int port = 48910;

        byte[] admKey = new byte[32];
        byte[] nextKey = new byte[32];
        byte[] admId =  new byte [4];//
        private InputStream nis; //Network Input Stream
        private OutputStream nos; //Network Output Stream
        int byteCnt;
        int buttonPressed;

        byte XORcheck = 0;
        byte XORin = 0;
        byte XORout = 0;//4
        byte xor;

        @Override
        protected void onPreExecute() {
            tvLockStatus.setText("Connection...");

        }

        private void prepareData(Bundle bundle){
            buttonPressed = bundle.getInt("BUTTON");
            cwmode = bundle.getInt("CWMODE");
            if(cwmode == SOFTAP) ipaddr = "192.168.4.1";
            else if(cwmode == STATION) ipaddr = bundle.getString("CIPSTA_IP");
            ssid = bundle.getString("CWJAP_SSID");
            admKey = bundle.getByteArray("ADM_KEY").clone();
            admId = bundle.getByteArray("ADM_ID").clone();

        }

        private byte[] makeChallengeRequestFor(byte userTag){
            SecureRandom random = new SecureRandom();
            byte[] nonce= new byte[16];
            random.nextBytes(nonce);
            nonce[0] = userTag;
            XORcheck = XORcalc(nonce);
            System.arraycopy(nonce, 0, initVectorRX, 0, 16);//SAVE it to receive message
            return nonce;
        }

        private byte[] encryptCommand(byte[] buffer, int command){
            System.arraycopy(buffer, 0, initVectorTX, 0, 16);
            byte[] temp = new byte[16];
            System.arraycopy(buffer, 0, temp, 0, temp.length);
            decryptedData = decrypt(temp, initVectorRX, admKey);
            System.arraycopy(decryptedData, 0, temp, 0, 16);
            XORin = decryptedData[0];
            if (XORcheck == XORin){
                byte[] plaintext = new byte[16];
                switch (command){
                    case READ_KEY:
                        plaintext[0] = 10;//MESSAGE TYPE: PASS_ACTION 10
                        plaintext[1] = XORcalc(temp);//XOR
                        System.arraycopy(admId, 0, plaintext, 2, admId.length);//userID[USER_ID_SIZE];//4
                        plaintext[6] = 0;// p1s0; 0 - save pass
                        plaintext[7] = channel;//
                        break;
                    case PLAY_KEY:
                        plaintext[0] = 10;//MESSAGE TYPE: PASS_ACTION 10
                        plaintext[1] = XORcalc(temp);//XOR
                        System.arraycopy(admId, 0, plaintext, 2, admId.length);//userID[USER_ID_SIZE];//4
                        plaintext[6] = 1;// p1s0; 1 - play pass
                        plaintext[7] = channel;//
                        System.arraycopy(wiegandRealPass, 0, plaintext, 8, wiegandRealPass.length);//userID[USER_ID_SIZE];//4
                        break;
                }
                XORcheck = XORcalc(plaintext);
                txData = encrypt(plaintext, initVectorTX, admKey);
                System.arraycopy(txData, 0, initVectorRX, 0, 16);//SAVE it to receive message
                return txData;
            }
            return null;
        }

        private int replyHandle(byte[] buffer, int command){
            // System.arraycopy(buffer, 32, initVectorTX, 0, 16);
            byte[] temp = new byte[48];
            System.arraycopy(buffer, 0, temp, 0, temp.length);
            decryptedData = decrypt(temp, initVectorRX, admKey);
            XORin = decryptedData[1];
            if (XORcheck == XORin) {
                System.arraycopy(decryptedData, 2, nextKey, 0, nextKey.length);
                byte[]reply = new byte[6];
                System.arraycopy(decryptedData, 42, reply, 0, reply.length);
                if(command == READ_KEY){
                    System.arraycopy(decryptedData, 34, wiegandRealPass, 0, wiegandRealPass.length);
                    publishProgress(new String(reply).split("\0")[0]);
                }else if(command == PLAY_KEY){
                    publishProgress(new String(reply).split("\0")[0]);
                }

                ContentValues cv = new ContentValues();
                cv.put(LockDataContract.COLUMN_ADM_KEY, nextKey);
                mDb.update(LockDataContract.TABLE_NAME_LOCK_DATA, cv,
                        LockDataContract._ID + "= ?", new String[] {String.valueOf(rowId)});

            }
            return 0;
        }

        @Override
        protected String doInBackground(Bundle... bundles) {
            byte[] buffer = new byte[100];
            prepareData(bundles[0]);
            int read = 0;
            int dialogStage = 0;
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(!wifiManager.isWifiEnabled()) {
                publishProgress("Not connected.\nTurn on Wi-Fi");
                return null;
            }
            if(!isConecctedToDevice()){
                publishProgress("IP address "+ipaddr+" not found.\nCheck your Wi-Fi network\nsettings or select another lock");
                return null;
            }

            try {
                nsocket = new Socket(ipaddr, port);
                if (nsocket.isConnected()) {
                    //private InputStream nis; //Network Input Stream
                    //private OutputStream nos; //Network Output Stream
                    nos = nsocket.getOutputStream();
                    nis = nsocket.getInputStream();
                    if(buttonPressed == CHOOSE_LOCK){

                        byte[] ping = new byte[6];
                        System.arraycopy("ping".getBytes(), 0, ping, 0, 4);//S
                        xor = XORcalc(admKey);
                        ping[5] = xor;
                        nos.write(ping);
                        nos.flush();
                        byteCnt = nis.read(buffer, 0, 100); //This is blocking
                        if (byteCnt != -1){
                            String rx = new String(buffer).split("\0")[0];
                            publishProgress(rx);
                        }else{
                            publishProgress("Error");
                        }
                    }else if(buttonPressed == READ_KEY || buttonPressed == PLAY_KEY){
                        while(read != -1 && dialogStage < 3){
                            if(dialogStage == 0){
                                txData = makeChallengeRequestFor(ADMIN);
                            }else if(dialogStage == 1){
                                txData = encryptCommand(buffer, buttonPressed);
                            }else if(dialogStage == 2){
                                replyHandle(buffer, buttonPressed);
                            }
                            if (txData == null || dialogStage == 2) break;
                            nos.write(txData);
                            nos.flush();
                            dialogStage++;
                            read = nis.read(buffer, 0, 100); //This is blocking
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                publishProgress(e.getMessage());
            }
            return null;
        }

        public boolean isConecctedToDevice() {
            Runtime runtime = Runtime.getRuntime();
            try {
                Process ipProcess = runtime.exec("/system/bin/ping -c 1 " + ipaddr);
                int     exitValue = ipProcess.waitFor();
                return (exitValue == 0);
            } catch (IOException e)          { e.printStackTrace(); }
            catch (InterruptedException e) { e.printStackTrace(); }
            return false;
        }


        @Override
        protected void onProgressUpdate(String... strings) {
            tvLockStatus.setText(strings[0]);
            if(strings[0].contentEquals("OK")){
                keyHasBeenRead = true;
                tvLockStatus.append("\n" + Arrays.toString(wiegandRealPass));
                saveButtonEnableRequest();
            }if(strings[0].contentEquals("PING OK")){
                enableKeyElements();

            }

        }

        @Override
        protected void onPostExecute(String result) {


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
