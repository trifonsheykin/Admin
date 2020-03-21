package com.smartlocks.trifonsheykin.admin.User;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartlocks.trifonsheykin.admin.DbHelper;
import com.smartlocks.trifonsheykin.admin.LockDataContract;
import com.smartlocks.trifonsheykin.admin.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class UserInfoActivity extends AppCompatActivity {
    private ImageView ivQrShow;
    private Button bSendAsText;
    private TextView tvUserInfo;

    private long id;
    byte[] accessCode;
    String[] projectionUser = {
            LockDataContract._ID,
            LockDataContract.COLUMN_USER_NAME,
            LockDataContract.COLUMN_USER_LOCKS,
            LockDataContract.COLUMN_USER_ACCESS_CODE,
            LockDataContract.COLUMN_AC_CREATION_DATE,
            LockDataContract.COLUMN_USER_KEY_TITLE,
            LockDataContract.COLUMN_LOCK_ROW_ID,
            LockDataContract.COLUMN_AES_KEY_ROW_ID,

    };

    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        ivQrShow = findViewById(R.id.iv_qr_code);
        bSendAsText = findViewById(R.id.b_send);
        tvUserInfo = findViewById(R.id.tv_user_info);


        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        Intent intent = getIntent();
        id = intent.getLongExtra("id", -1);
        Cursor cursor = mDb.query(
                LockDataContract.TABLE_NAME_USER_DATA,
                projectionUser,
                LockDataContract._ID + "= ?",
                new String[] {String.valueOf(id)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
        cursor.moveToPosition(0);

        accessCode = cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_USER_ACCESS_CODE));
        String userName = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_USER_NAME));
        String userLocks = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_USER_LOCKS));
        String date = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_AC_CREATION_DATE));
        String keyTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_USER_KEY_TITLE));
        long lockRowId = cursor.getLong(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK_ROW_ID));
        long aesRowId = cursor.getLong(cursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY_ROW_ID));

        if(accessCode != null){
            tvUserInfo.setText("User name: "         + userName +
                    "\nAccess time for: "  + userLocks +
                    "\n" + decodeAccessCode(accessCode) +
                    "\nKey title: "         + keyTitle +
                    "\nCode created: " + date +
                    "\nLock: " + lockRowId + ", AES: " + aesRowId);

            String encoded = Base64.encodeToString(accessCode, Base64.NO_WRAP);
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            try{
                BitMatrix bitMatrix = multiFormatWriter.encode(encoded, BarcodeFormat.QR_CODE, 500, 500);
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                ivQrShow.setImageBitmap(bitmap);

            }catch (WriterException e){
                e.printStackTrace();
            }
        }else{
            tvUserInfo.setText("User info error");

        }





        bSendAsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String encoded = Base64.encodeToString(accessCode, Base64.NO_WRAP);
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, encoded);
                sendIntent.setType("text/plain");
                if (sendIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(sendIntent, "Choose an app"));
                }
            }
        });

    }



    private String decodeAccessCode(byte[] ac){
        byte[] ipAddr = new byte[4];
        String ipAddress;
        String out;
        if(ac.length == 78){

            byte[] door1StartTime = new byte[5];
            byte[] door1StopTime = new byte[5];
            byte[] door2StartTime = new byte[5];
            byte[] door2StopTime = new byte[5];

            System.arraycopy(ac, 48, ipAddr, 0, 4);
            ipAddress = ipByteToStr(ipAddr);

            System.arraycopy(ac, 57, door1StartTime, 0, 5);
            System.arraycopy(ac, 62, door1StopTime, 0, 5);
            System.arraycopy(ac, 67, door2StartTime, 0, 5);
            System.arraycopy(ac, 72, door2StopTime, 0, 5);

            out = "Door 1 from: " + accessTimeByteToStr(door1StartTime) +
                    "\n                  to: " + accessTimeByteToStr(door1StopTime) +
                    "\nDoor 2 from: " + accessTimeByteToStr(door2StartTime) +
                    "\n                  to: " + accessTimeByteToStr(door2StopTime) +
                    "\nIP address: " + ipAddress;

        }else{
            byte[] doorStopTime = new byte[5];
            System.arraycopy(ac, 33, doorStopTime, 0, 5);
            System.arraycopy(ac, 38, ipAddr, 0, 4);
            ipAddress = ipByteToStr(ipAddr);
            out = "Expiration date: " + accessTimeByteToStr(doorStopTime) +
                    "\nIP address: " + ipAddress;


        }
        return out;

    }
    private String accessTimeByteToStr(byte[] date){
        String hour, minute, day, month, year;

        year =  new String("" + (date[0] >> 4) + (date[0] & 0x0F));
        month =  new String("" + (date[1] >> 4) + (date[1] & 0x0F));
        day =  new String("" + (date[2] >> 4) + (date[2] & 0x0F));
        hour =  new String("" + (date[3] >> 4) + (date[3] & 0x0F));
        minute =  new String("" + (date[4] >> 4) + (date[4] & 0x0F));

        return new String(hour + ":" + minute + " " + day + "." + month + "." + year);
    }
    private String ipByteToStr(byte[] ip){
        String output = new String();
        for(int i = 0; i < ip.length; i++){
            output = output + Integer.toString((int)ip[i] & 0xFF);
            if(i!=3) output = output + ".";
        }
        return output;
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


