package com.smartlocks.trifonsheykin.admin.Lock;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.smartlocks.trifonsheykin.admin.DbHelper;
import com.smartlocks.trifonsheykin.admin.LockDataContract;
import com.smartlocks.trifonsheykin.admin.R;

public class LockMainActivity extends AppCompatActivity {

    private final int EDIT_LOCK = 2;
    private final int NEW_LOCK = 1;
    private SQLiteDatabase mDb;
    private DataAdapterLock dataAdapterLock;
    private Cursor cursor;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;

    private RecyclerView recyclerView;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
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
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(LockMainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = sharedPreferences.edit();

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //do nothing, we only care about swiping
                return false;
            }
            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                final long id = (long) viewHolder.itemView.getTag();
                final long spId = sharedPreferences.getLong("lockRowId", -1);


                AlertDialog.Builder alertDialog = new AlertDialog.Builder(LockMainActivity.this);
                alertDialog.setTitle("Lock delete");
                alertDialog.setMessage("Do you want to delete this lock?");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int which) {
                        if(id == spId){
                            spEditor.putLong("lockRowId", -1);
                            spEditor.putString("lockButton", "default lock");
                            spEditor.commit();
                        }
                        removeLock(id);
                        dataAdapterLock.swapCursor(getAllLocks());
                    }
                });

                // Обработчик на нажатие НЕТ
                alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dataAdapterLock.swapCursor(getAllLocks());
                        dialog.cancel();
                    }
                });
                alertDialog.show();


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

