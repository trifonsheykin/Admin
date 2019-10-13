package com.smartlocks.trifonsheykin.admin.Key;

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

public class KeySelectActivity extends AppCompatActivity {

    private SQLiteDatabase mDb;
    private DataAdapterKey dataAdapterKey;
    private Cursor cursor;

    private RecyclerView recyclerViewKey;

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
        setContentView(R.layout.activity_key_select);

        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(Intent.EXTRA_TEXT)) {

        }


        recyclerViewKey = findViewById(R.id.keyRecycler);
        recyclerViewKey.setLayoutManager(new LinearLayoutManager(this));
        DbHelper dbHelperKey = DbHelper.getInstance(this);
        mDb = dbHelperKey.getWritableDatabase();
        cursor = getAllKeys();
        dataAdapterKey = new DataAdapterKey(this, cursor, itemClickListener);
        recyclerViewKey.setAdapter(dataAdapterKey);

    }


    private Cursor getAllKeys() {
        return mDb.query(
                LockDataContract.TABLE_NAME_KEY_DATA,
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
