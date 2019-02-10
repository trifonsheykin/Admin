package com.example.trifonsheykin.admin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.Toast;

import com.example.trifonsheykin.admin.Key.KeyMainActivity;
import com.example.trifonsheykin.admin.Lock.LockMainActivity;
import com.example.trifonsheykin.admin.User.UserMainActivity;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{

    private final int QR = 1;
    private final int NFC = 2;
    private final int BUTTON = 3;
    private boolean qrScanner;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;
    private ImageView imageView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        imageView = findViewById(R.id.iReaderIcon);
        imageView.setVisibility(View.INVISIBLE);
        imageView.setEnabled(false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        qrScanner = sharedPreferences.getBoolean("qrScanner", false);
        if(qrScanner){
            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
            startActivityForResult(intent, 0);
        }



    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == 0){
            Toast.makeText(getApplicationContext(),data.getStringExtra("result"),Toast.LENGTH_SHORT).show();


        }

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

        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
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
        if (item.getItemId() == R.id.nav_qr_scan) {
            Intent intent = new Intent(MainActivity.this, QrReadActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }
        return false;
    }


}
