package com.example.trifonsheykin.admin.Key;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.trifonsheykin.admin.R;

public class ViewHolderKey extends RecyclerView.ViewHolder {
    public TextView keyTitle;
    public TextView keyUser;

    public ViewHolderKey(View itemView, View.OnClickListener clickListener) {
        super(itemView);

        keyTitle = itemView.findViewById(R.id.tvKeyTitle);
        keyUser = itemView.findViewById(R.id.tvKeyUser);
        itemView.setOnClickListener(clickListener);

    }
}
