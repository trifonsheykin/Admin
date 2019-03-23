package com.example.trifonsheykin.admin.Lock;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

import static com.example.trifonsheykin.admin.LockDataContract.SOFTAP;
import static com.example.trifonsheykin.admin.LockDataContract.STATION;

public class DataAdapterLock extends RecyclerView.Adapter<ViewHolderLock> {
    private Cursor cursor;
    LayoutInflater inflater;
    private View.OnClickListener clickListener;

    public DataAdapterLock(Context context, Cursor cursor, View.OnClickListener clickListener) {
        this.cursor = cursor;
        this.clickListener = clickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolderLock onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_lock, parent, false);
        return new ViewHolderLock(view, clickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolderLock holder, int position) {
// Move the mCursor to the position of the item to be displayed
        if (!cursor.moveToPosition(position))
            return; // bail if returned null
        String lock1Title = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK1_TITLE));
        String lock2Title = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_LOCK2_TITLE));
        int cwMode = cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_CWMODE));
        String ssidTitle;
        if(cwMode == SOFTAP){
            ssidTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWSAP_SSID));
        }else if(cwMode == STATION){
            ssidTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_CWJAP_SSID));
        }else{
            ssidTitle = "No SSID";
        }
        long id = cursor.getLong(cursor.getColumnIndex(LockDataContract._ID));
        if(cursor.getBlob(cursor.getColumnIndex(LockDataContract.COLUMN_EXPIRED_AES_KEYS_PAGES)) == null){
            holder.imageView.setImageResource(R.drawable.ic_https_colored_24dp);
        }else{
            holder.imageView.setImageResource(R.drawable.ic_sync_colored_24dp);
        }
        holder.locksTitle.setText(lock1Title + " & " + lock2Title);
        holder.ssidTitle.setText(ssidTitle);
        holder.itemView.setTag(id);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        // Always close the previous mCursor first
        if (cursor != null) cursor.close();
        cursor = newCursor;
        if (newCursor != null) {
            // Force the RecyclerView to refresh
            this.notifyDataSetChanged();

        }
    }
}
