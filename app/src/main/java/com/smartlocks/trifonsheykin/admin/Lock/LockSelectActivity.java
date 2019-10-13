package com.smartlocks.trifonsheykin.admin.Lock;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.smartlocks.trifonsheykin.admin.DbHelper;
import com.smartlocks.trifonsheykin.admin.LockDataContract;
import com.smartlocks.trifonsheykin.admin.R;

public class LockSelectActivity extends AppCompatActivity {
    private SQLiteDatabase mDb;
    private DataAdapterLock dataAdapterLock;
    private Cursor cursor;

    RecyclerView recyclerView;

    private final View.OnClickListener itemClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //HERE WE RETURN IN INTENTS ACTIVITY RESULT ITEM ID IN DATABASE
                    Intent intent = new Intent();
                    long id = (long) view.getTag();
                    intent.putExtra("rowId", id);
                    setResult(RESULT_OK, intent);
                    finish();

                } };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_select);

        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(Intent.EXTRA_TEXT)) {

        }

        recyclerView = (RecyclerView) findViewById(R.id.lockRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create a DB helper (this will create the DB if run for the first time)
        DbHelper dbHelperLock = DbHelper.getInstance(this);

        // Keep a reference to the mDb until paused or killed. Get a writable database
        // because you will be adding customers
        mDb = dbHelperLock.getWritableDatabase();

        // Get all user info from the database and save in a cursor
        cursor = getAllLocks();

        // Create an adapter for that cursor to display the data
        dataAdapterLock = new DataAdapterLock(this, cursor, itemClickListener);
        recyclerView.setAdapter(dataAdapterLock);

    }



    private Cursor getAllLocks() {
        return mDb.query(
                LockDataContract.TABLE_NAME_LOCK_DATA,
                null,
                null,
                null,
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
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
