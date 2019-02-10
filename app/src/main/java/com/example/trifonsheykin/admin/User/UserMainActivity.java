package com.example.trifonsheykin.admin.User;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.example.trifonsheykin.admin.Lock.LockEditActivity;
import com.example.trifonsheykin.admin.Lock.LockMainActivity;
import com.example.trifonsheykin.admin.R;

public class UserMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);


        FloatingActionButton fab = findViewById(R.id.fab_user);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(UserMainActivity.this, UserEditActivity.class);
                startActivityForResult(intent, 1);//request code new lock
            }
        });
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
