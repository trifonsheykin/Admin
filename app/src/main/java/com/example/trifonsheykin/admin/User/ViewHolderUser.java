package com.example.trifonsheykin.admin.User;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.example.trifonsheykin.admin.R;

public class ViewHolderUser extends RecyclerView.ViewHolder{
    public TextView userName;
    public TextView userLocks;
//
//    public long aesId;
//    public long lockId;
//    public byte userTag;


    public ViewHolderUser(View itemView, View.OnClickListener clickListener) {
        super(itemView);

        userName = itemView.findViewById(R.id.tvUserName);
        userLocks = itemView.findViewById(R.id.tvUserLocks);
        itemView.setOnClickListener(clickListener);

    }

}
