package com.smartlocks.trifonsheykin.admin.User;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.smartlocks.trifonsheykin.admin.LockDataContract;
import com.smartlocks.trifonsheykin.admin.R;

public class DataAdapterUser extends RecyclerView.Adapter<ViewHolderUser>{

    LayoutInflater inflater;
    private Cursor cursor;
    private View.OnClickListener clickListener;

    public DataAdapterUser(Context context, Cursor cursor, View.OnClickListener clickListener) {
        this.cursor = cursor;
        this.clickListener = clickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolderUser onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.item_user, viewGroup, false);
        return new ViewHolderUser(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderUser holder, int position) {
        if (!cursor.moveToPosition(position))
            return;
        String userName= cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_USER_NAME));
        String userLocks = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_USER_LOCKS));
        long id = cursor.getLong(cursor.getColumnIndex(LockDataContract._ID));
        int userExpired = cursor.getInt(cursor.getColumnIndex(LockDataContract.COLUMN_USER_EXPIRED));

        if(userExpired != 0){
            holder.userName.setText(userName + ": EXPIRED (swipe to delete)");
            holder.userLocks.setText("Access denied to: " + userLocks);
            holder.userName.setEnabled(false);
            holder.userLocks.setEnabled(false);
        } else{
            holder.userName.setText(userName);
            holder.userLocks.setText("Access to: " + userLocks);
            holder.userName.setEnabled(true);
            holder.userLocks.setEnabled(true);
        }

        holder.itemView.setTag(id);

    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) cursor.close();
        cursor = newCursor;
        if (newCursor != null) {
            this.notifyDataSetChanged();
        }
    }
}
