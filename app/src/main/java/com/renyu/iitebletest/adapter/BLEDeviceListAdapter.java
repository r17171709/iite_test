package com.renyu.iitebletest.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.activity.BLEDeviceListActivity;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by renyu on 16/3/31.
 */
public class BLEDeviceListAdapter extends RecyclerView.Adapter<BLEDeviceListAdapter.BLEDeviceListHolder> {

    Context context;
    ArrayList<BluetoothDevice> models;

    public BLEDeviceListAdapter(Context context, ArrayList<BluetoothDevice> models) {
        this.context = context;
        this.models = models;
    }

    @Override
    public BLEDeviceListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.adapter_bledevicelist, parent, false);
        return new BLEDeviceListHolder(view);
    }

    @Override
    public void onBindViewHolder(BLEDeviceListHolder holder, final int position) {
        holder.adapter_bledevice.setText(models.get(position).getName()==null?"null":models.get(position).getName()+" "+models.get(position).getAddress());
        holder.adapter_bledevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                Bundle bundle=new Bundle();
                bundle.putString("address", models.get(position).getAddress());
                intent.putExtras(bundle);
                ((BLEDeviceListActivity) context).setResult(Activity.RESULT_OK, intent);
                ((BLEDeviceListActivity) context).finish();
            }
        });
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public static class BLEDeviceListHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.adapter_bledevice)
        TextView adapter_bledevice;

        public BLEDeviceListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
