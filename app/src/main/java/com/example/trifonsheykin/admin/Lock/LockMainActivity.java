package com.example.trifonsheykin.admin.Lock;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;

import com.example.trifonsheykin.admin.DbHelper;
import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

public class LockMainActivity extends AppCompatActivity {

    private final int EDIT_LOCK = 2;
    private final int NEW_LOCK = 1;
    private SQLiteDatabase mDb;
    private DataAdapterLock dataAdapterLock;
    private Cursor cursor;

    private RecyclerView recyclerView;

    private final View.OnClickListener itemClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long id = (long) view.getTag();
                    Intent intent= new Intent(LockMainActivity.this, LockInfoActivity.class);
                    intent.putExtra("rowId", id);
                    startActivityForResult(intent, EDIT_LOCK);////request code edit lock
                } };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_main);

        recyclerView = findViewById(R.id.lockRecycler);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();

        cursor = getAllLocks();
        dataAdapterLock = new DataAdapterLock(this, cursor, itemClickListener);
        recyclerView.setAdapter(dataAdapterLock);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //do nothing, we only care about swiping
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                removeLock(id);
                dataAdapterLock.swapCursor(getAllLocks());
            }
        }).attachToRecyclerView(recyclerView);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(LockMainActivity.this, LockEditActivity.class);
                startActivityForResult(intent, NEW_LOCK);//request code new lock
            }
        });
    }//onCreate


    private boolean removeLock(long id) {
        return mDb.delete(LockDataContract.TABLE_NAME_LOCK_DATA, LockDataContract._ID + "=" + id, null) > 0;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dataAdapterLock.swapCursor(getAllLocks());
        if(resultCode == RESULT_OK){
            recyclerView.smoothScrollToPosition(dataAdapterLock.getItemCount());
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
