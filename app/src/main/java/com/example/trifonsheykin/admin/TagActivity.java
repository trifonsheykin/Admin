package com.example.trifonsheykin.admin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trifonsheykin.admin.Lock.LockSelectActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class TagActivity extends AppCompatActivity {

    private Button bSelectLock;
    private Button bPrintQr;
    private Button bShareQr;
    private RadioGroup rgLockSelect;
    private RadioButton rbDoorOne;
    private RadioButton rbDoorTwo;
    private Switch sNfcWrite;
    private ImageView qrImage;
    private ImageView nfcImage;
    private Bitmap bitmap;
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
        bPrintQr = findViewById(R.id.b_print_qr);
        bShareQr = findViewById(R.id.b_share_qr);
        rgLockSelect = findViewById(R.id.rg_lock_select);
        rbDoorOne = findViewById(R.id.rb_door1_select);
        rbDoorTwo = findViewById(R.id.rb_door2_select);
        sNfcWrite = findViewById(R.id.s_nfc_write);
        qrImage = findViewById(R.id.iv_qr);
        nfcImage = findViewById(R.id.iv_nfc);


        bSelectLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_disabled_24dp, 0,0,0);

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

        rgLockSelect.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(rbDoorOne.isChecked()){
                    qrImageUpdate(cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID)));
                }else if(rbDoorTwo.isChecked()){
                    qrImageUpdate(cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR2_ID)));
                }
            }
        });

        sNfcWrite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    enableForegroundDispatchSystem();
                    nfcImage.setImageResource(R.drawable.ic_nfc_enabled_24dp);
                }else{
                    disableForegroundDispatchSystem();
                    nfcImage.setImageResource(R.drawable.ic_nfc_disabled_24dp);
                }
            }
        });

        bShareQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File imagePath = new File(getApplicationContext().getCacheDir(), "images");
                File newFile = new File(imagePath, "image.png");
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.trifonsheykin.admin.fileprovider", newFile);

                if (contentUri != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                    shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(shareIntent, "Choose an app"));
                }

            }
        });

        bPrintQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doPhotoPrint();
            }
        });
    }
    private void doPhotoPrint() {
        PrintHelper photoPrinter = new PrintHelper(TagActivity.this);
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.printBitmap("qr-code", resizeBitmap(bitmap, 1500, 2300));
    }
    public Bitmap resizeBitmap(Bitmap Src, int padding_x, int padding_y) {
        Bitmap outputimage = Bitmap.createBitmap(Src.getWidth() + padding_x,Src.getHeight() + padding_y, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(outputimage);
        can.drawARGB(0xFF,0xFF,0xFF,0xFF); //This represents White color
        can.drawBitmap(Src, 10, 0, null);
        return outputimage;
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
                String lock1id = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR1_ID));
                String lock2id = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_DOOR2_ID));
                rbDoorOne.setEnabled(true);
                rbDoorTwo.setEnabled(true);
                rbDoorOne.setChecked(true);
                rbDoorOne.setText("Door 1: " + lock1Title + "\nid: " + lock1id);
                rbDoorTwo.setText("Door 2: " + lock2Title + "\nid: " + lock2id);
                sNfcWrite.setEnabled(true);
                bPrintQr.setEnabled(true);
                bShareQr.setEnabled(true);
                qrImageUpdate(lock1id);
                bSelectLock.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_https_colored_24dp, 0,0,0);
            }
        }
    }

    private void qrImageUpdate(String doorId){
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try{
            BitMatrix bitMatrix = multiFormatWriter.encode(doorId, BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrImage.setImageBitmap(bitmap);
            File cachePath = new File(getApplicationContext().getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

        }catch (WriterException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
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
        final Ndef ndef = Ndef.get(tag);
        try{
            if(tag == null){
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }
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
                if(ndef.canMakeReadOnly()){
                    ndef.makeReadOnly();
                    Toast.makeText(this, "Tag written", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "Rewritable tag written", Toast.LENGTH_SHORT).show();
                }
                ndef.close();


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
