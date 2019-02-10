package com.example.trifonsheykin.admin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trifonsheykin.admin.Key.KeyEditActivity;
import com.example.trifonsheykin.admin.Lock.LockSelectActivity;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class TagActivity extends AppCompatActivity {

    private Button bSelectLock;
    private Button bGenerateQr;
    private Button bSaveQr;
    private Button bGenerateNfc;
    private RadioGroup rgLockSelect;
    private RadioButton rbDoorOne;
    private RadioButton rbDoorTwo;
    private ImageView qrImage;
    private TextView tvNfcStatus;
    private SQLiteDatabase mDb;
    private Cursor cursor;
    private long rowId;
    private static final int REQUEST_ROW_ID = 1;
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

    NfcAdapter nfcAdapter;
    private String doorId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        bSelectLock = findViewById(R.id.b_lock_select);
        bGenerateQr = findViewById(R.id.b_generate_qr);
        bSaveQr = findViewById(R.id.b_save_qr);
        bGenerateNfc = findViewById(R.id.b_generate_nfc);
        rgLockSelect = findViewById(R.id.rg_lock_select);
        rbDoorOne = findViewById(R.id.rb_door1_select);
        rbDoorTwo = findViewById(R.id.rb_door2_select);
        qrImage = findViewById(R.id.iv_qr);
        tvNfcStatus = findViewById(R.id.tv_nfc_status);

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        bSelectLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = TagActivity.this;
                Class destinationActivity = LockSelectActivity.class;
                Intent intent= new Intent(context, destinationActivity);//
                startActivityForResult(intent, REQUEST_ROW_ID);
            }
        });

        bGenerateNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rbDoorOne.isChecked()) {
                    doorId = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));

                }else{
                    doorId = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR2_ID));
                }

                tvNfcStatus.setText(doorId);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_ROW_ID) {
            rowId = data.getLongExtra("rowId", -1);
            if (rowId == -1) {
                rbDoorOne.setText("ERROR ROW ID");
                rbDoorTwo.setText("ERROR ROW ID");
            } else {
                cursor = mDb.query(
                        LockDataContract.TABLE_NAME_LOCK_DATA,
                        projection,
                        LockDataContract._ID + "= ?",
                        new String[]{String.valueOf(rowId)},
                        null,
                        null,
                        LockDataContract.COLUMN_TIMESTAMP
                );
                cursor.moveToPosition(0);
                String lock1Title = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
                String lock2Title = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
                rbDoorTwo.setChecked(true);
                rbDoorOne.setText("Generate door ID for " + lock1Title);
                rbDoorTwo.setText("Generate door ID for " + lock2Title);

            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.hasExtra(NfcAdapter.EXTRA_TAG)){
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NdefMessage ndefMessage = createNdefMessage(doorId);
            writeNdefMessage(tag, ndefMessage);

        }
    }
    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage){
        try{
            if(tag == null){
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }
            Ndef ndef = Ndef.get(tag);
            if(ndef == null){
                formatTag(tag, ndefMessage);
            }else{
                ndef.connect();
                if(!ndef.isWritable()){
                    Toast.makeText(this, "Tag is not writable", Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }
                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                Toast.makeText(this, "Tag written", Toast.LENGTH_SHORT).show();

            }

        }catch (Exception e){
            Log.e("writeNdefMessage", e.getMessage());

        }

    }
    private void formatTag(Tag tag, NdefMessage ndefMessage){
        try{
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if(ndefFormatable == null){
                Toast.makeText(this, "TAG is not ndef formatable", Toast.LENGTH_SHORT).show();
                return;
            }
            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag written", Toast.LENGTH_SHORT).show();

        }catch (Exception e){
            Log.e("formatTag", e.getMessage());

        }

    }

    private NdefRecord createTextRecord(String content){
        try{
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        }catch (UnsupportedEncodingException e){
            Log.e("createTextRecord", e.getMessage());

        }
        return null;

    }

    private NdefMessage createNdefMessage(String content) {
        NdefRecord ndefRecord = createTextRecord(content);
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord, NdefRecord.createApplicationRecord("com.smartlock.client")});

        return ndefMessage;

    }


    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }

    private void enableForegroundDispatchSystem(){
        Intent intent = new Intent(this, TagActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[]{};
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }

    private void disableForegroundDispatchSystem(){
        nfcAdapter.disableForegroundDispatch(this);
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
