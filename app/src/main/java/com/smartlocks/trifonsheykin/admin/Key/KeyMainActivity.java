package com.smartlocks.trifonsheykin.admin.Key;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;

import com.smartlocks.trifonsheykin.admin.DbHelper;
import com.smartlocks.trifonsheykin.admin.LockDataContract;
import com.smartlocks.trifonsheykin.admin.R;

public class KeyMainActivity extends AppCompatActivity {

    private final int EDIT_KEY = 2;
    private final int NEW_KEY = 1;
    private int counter = 0;
    RecyclerView recyclerViewKey;

    private SQLiteDatabase mDb;
    private DataAdapterKey dataAdapterKey;
    private Cursor cursor;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor spEditor;

    private final View.OnClickListener keyItemClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long id = (long) view.getTag();
                    Intent intent= new Intent(KeyMainActivity.this, KeyInfoActivity.class);
                    intent.putExtra("id", id);
                    startActivity(intent);////request code edit lock

                } };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_main);

        recyclerViewKey = findViewById(R.id.keyRecycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewKey.setLayoutManager(linearLayoutManager);

        DbHelper dbHelperKey = DbHelper.getInstance(this);
        mDb = dbHelperKey.getWritableDatabase();
        cursor = getAllKeys();
        dataAdapterKey = new DataAdapterKey(this, cursor, keyItemClickListener);
        recyclerViewKey.setAdapter(dataAdapterKey);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        spEditor = sharedPreferences.edit();

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //do nothing, we only care about swiping
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {

                final long id = (long) viewHolder.itemView.getTag();
                final long spId = sharedPreferences.getLong("keyRowId", -1);//sharedPreferences.getLong("keyRowId", -1)
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(KeyMainActivity.this);
                alertDialog.setTitle("Key delete");
                alertDialog.setMessage("Do you want to delete this key?");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int which) {
                        if(id == spId){
                            spEditor.putLong("keyRowId", -1);
                            spEditor.putString("keyButton", "default key");
                            spEditor.commit();
                        }
                        removeKey(id);
                        dataAdapterKey.swapCursor(getAllKeys());
                    }
                });
                alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dataAdapterKey.swapCursor(getAllKeys());
                        dialog.cancel();
                    }
                });
                alertDialog.show();

            }
        }).attachToRecyclerView(recyclerViewKey);

        FloatingActionButton fabKey = findViewById(R.id.fab_key);
        fabKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(KeyMainActivity.this, KeyEditActivity.class);
                startActivityForResult(intent, NEW_KEY);////request code edit lock
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            dataAdapterKey.swapCursor(getAllKeys());
            recyclerViewKey.smoothScrollToPosition(dataAdapterKey.getItemCount());
        }
    }

    private boolean removeKey(long id) {
        // Inside, call mDb.delete to pass in the TABLE_NAME and the condition that WaitlistEntry._ID equals id
        return mDb.delete(LockDataContract.TABLE_NAME_KEY_DATA, LockDataContract._ID + "=" + id, null) > 0;
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
