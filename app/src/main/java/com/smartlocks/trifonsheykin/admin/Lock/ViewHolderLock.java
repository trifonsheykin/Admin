package com.smartlocks.trifonsheykin.admin.Lock;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smartlocks.trifonsheykin.admin.R;

public class ViewHolderLock extends RecyclerView.ViewHolder {
    public TextView locksTitle;
    public TextView ssidTitle;
    public ImageView imageView;

    public ViewHolderLock(View itemView, View.OnClickListener clickListener) {
        super(itemView);

        imageView = itemView.findViewById(R.id.iv_lock_item);
        locksTitle = itemView.findViewById(R.id.tvLocksTitle);
        ssidTitle = itemView.findViewById(R.id.tvSsidTitle);
        itemView.setOnClickListener(clickListener);

    }
}
