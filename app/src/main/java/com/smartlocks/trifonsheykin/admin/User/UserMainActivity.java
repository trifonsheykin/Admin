package com.smartlocks.trifonsheykin.admin.User;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

public class UserMainActivity extends AppCompatActivity {
    private SQLiteDatabase mDb;
    RecyclerView recyclerViewUser;
    private DataAdapterUser dataAdapterUser;
    private Cursor cursor;

    String[] projectionUser = {
            LockDataContract._ID,
            LockDataContract.COLUMN_USER_ACCESS_CODE,
            LockDataContract.COLUMN_LOCK_ROW_ID,
            LockDataContract.COLUMN_AES_KEY_ROW_ID
    };
    String[] projectionLock = {
            LockDataContract._ID,
            LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES
    };


    private final View.OnClickListener userItemClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long id = (long) view.getTag();
                    Intent intent= new Intent(UserMainActivity.this, UserInfoActivity.class);
                    intent.putExtra("id", id);
                    startActivity(intent);//request code new lock
                } };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        recyclerViewUser = findViewById(R.id.userRecycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerViewUser.setLayoutManager(linearLayoutManager);

        DbHelper dbHelperLock = DbHelper.getInstance(this);
        mDb = dbHelperLock.getWritableDatabase();
        cursor = getAllUsers();
        dataAdapterUser = new DataAdapterUser(this, cursor, userItemClickListener);
        recyclerViewUser.setAdapter(dataAdapterUser);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                //do nothing, we only care about swiping
                return false;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                long id = (long) viewHolder.itemView.getTag();
                notifyLockAboutUserRemove(id);
                removeUser(id);//remove from DB
                Toast.makeText(getApplicationContext(),"User removed. Sync with lock",Toast.LENGTH_SHORT).show();
                dataAdapterUser.swapCursor(getAllUsers());//update the list
            }
        }).attachToRecyclerView(recyclerViewUser);

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
    protected void onResume() {
        super.onResume();
        dataAdapterUser.swapCursor(getAllUsers());//update the list

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == 1){
                long id = data.getLongExtra("id", -1);//intent.putExtra("rowId", id);
                if(id != -1){
                    Intent intent= new Intent(UserMainActivity.this, UserInfoActivity.class);
                    intent.putExtra("id", id);
                    startActivity(intent);//request code new lock
                }
            }
        }
    }

    private void notifyLockAboutUserRemove(long id){

        Cursor newcursor = mDb.query(
                LockDataContract.TABLE_NAME_USER_DATA,
                projectionUser,
                LockDataContract._ID + "= ?",
                new String[] {String.valueOf(id)},
                null,
                null,
                LockDataContract.COLUMN_TIMESTAMP
        );
        newcursor.moveToPosition(0);
        long aesId = newcursor.getLong(newcursor.getColumnIndex(LockDataContract.COLUMN_AES_KEY_ROW_ID));
        mDb.delete(LockDataContract.TABLE_NAME_AES_DATA, LockDataContract._ID + "= ?", new String[]{String.valueOf(aesId)});

        byte[] accessCode = newcursor.getBlob(newcursor.getColumnIndex(LockDataContract.COLUMN_USER_ACCESS_CODE));
        long lockId = newcursor.getLong(newcursor.getColumnIndex(LockDataContract.COLUMN_LOCK_ROW_ID));
        byte userTag;

        if(accessCode != null){
            if(accessCode.length == 78){
                userTag = accessCode[56];
            }else{
                userTag = accessCode[32];
            }

            Cursor cursorLock = mDb.query(
                    LockDataContract.TABLE_NAME_LOCK_DATA,
                    projectionLock,
                    LockDataContract._ID + "= ?",
                    new String[] {String.valueOf(lockId)},
                    null,
                    null,
                    LockDataContract.COLUMN_TIMESTAMP
            );

            if(cursorLock.getCount() != 0){
                cursorLock.moveToPosition(0);
                byte[] expiredAes  = cursorLock.getBlob(cursorLock.getColumnIndex(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES));
                ContentValues cv = new ContentValues();
                if(expiredAes == null){
                    byte[] newExpiredAes = {userTag};
                    cv.put(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES, newExpiredAes);
                }else{
                    boolean byteFound = false;
                    for(byte b: expiredAes){
                        if(b == userTag) byteFound = true;
                    }
                    if(byteFound){
                        cv.put(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES, expiredAes);
                    }else{
                        byte[] newExpAes = new byte[expiredAes.length+1];
                        System.arraycopy(expiredAes, 0, newExpAes, 0, expiredAes.length);
                        newExpAes[expiredAes.length] = userTag;
                        cv.put(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES, newExpAes);
                    }

                }
                mDb.update(LockDataContract.TABLE_NAME_LOCK_DATA, cv,
                        LockDataContract._ID + "= ?", new String[] {String.valueOf(lockId)});
                cursorLock.close();

            }
        }

        newcursor.close();

    }

    private boolean removeUser(long id) {
        return mDb.delete(LockDataContract.TABLE_NAME_USER_DATA, LockDataContract._ID + "=" + id, null) > 0;
    }

    private Cursor getAllUsers() {
        return mDb.query(
                LockDataContract.TABLE_NAME_USER_DATA,
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
