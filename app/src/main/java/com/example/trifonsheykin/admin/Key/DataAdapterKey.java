package com.example.trifonsheykin.admin.Key;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.trifonsheykin.admin.LockDataContract;
import com.example.trifonsheykin.admin.R;

public class DataAdapterKey extends RecyclerView.Adapter<ViewHolderKey>  {

    LayoutInflater inflater;
    private Cursor cursor;
    private View.OnClickListener clickListener;

    public DataAdapterKey(Context context, Cursor cursor, View.OnClickListener clickListener) {
        this.cursor = cursor;
        this.clickListener = clickListener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolderKey onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_key, parent, false);

        return new ViewHolderKey(view, clickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolderKey holder, int position) {
        //String msg = keyTitle.get(position);
        //holder.keyTitle.setText(msg);

        //String acc = keyUser.get(position);
        //holder.keyUser.setText(acc);

        // Move the mCursor to the position of the item to be displayed
        if (!cursor.moveToPosition(position))
            return; // bail if returned null

        // Update the view holder with the information needed to display
        String keyTitle= cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_NAME));
        String lockTitle = cursor.getString(cursor.getColumnIndex(LockDataContract.COLUMN_KEY_LOCKS));
        // COMPLETED (6) Retrieve the id from the cursor and
        long id = cursor.getLong(cursor.getColumnIndex(LockDataContract._ID));

        holder.keyTitle.setText(keyTitle);

        holder.keyUser.setText(lockTitle);

        holder.itemView.setTag(id);
    }

    @Override
    public int getItemCount()  {
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
