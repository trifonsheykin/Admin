package com.example.trifonsheykin.admin;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity {

    CheckBox cbQrScanner;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
//        cbQrScanner = findViewById(R.id.cbScanQrFirst);
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        spEditor = sharedPreferences.edit();
//
//        cbQrScanner.setChecked(sharedPreferences.getBoolean("qrScanner", false));
//        cbQrScanner.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//
//                spEditor.putBoolean("qrScanner", isChecked);
//                spEditor.commit();
//            }
//        });
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
