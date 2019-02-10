package com.example.trifonsheykin.admin.Lock;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.trifonsheykin.admin.R;

public class ViewHolderLock extends RecyclerView.ViewHolder {
    public TextView locksTitle;
    public TextView ssidTitle;

    public ViewHolderLock(View itemView, View.OnClickListener clickListener) {
        super(itemView);

        locksTitle = itemView.findViewById(R.id.tvLocksTitle);
        ssidTitle = itemView.findViewById(R.id.tvSsidTitle);
        itemView.setOnClickListener(clickListener);

    }
}
