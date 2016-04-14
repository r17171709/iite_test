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
    ArrayList<com.renyu.iitebletest.bluetooth.BluetoothDevice> models;

    public BLEDeviceListAdapter(Context context, ArrayList<com.renyu.iitebletest.bluetooth.BluetoothDevice> models) {
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
        holder.adapter_bledevice.setText(models.get(position).getDevice().getName()==null?"null":models.get(position).getDevice().getName()+" "+models.get(position).getDevice().getAddress());
        holder.adapter_bledevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                Bundle bundle=new Bundle();
                bundle.putString("address", models.get(position).getDevice().getAddress());
                intent.putExtras(bundle);
                ((BLEDeviceListActivity) context).setResult(Activity.RESULT_OK, intent);
                ((BLEDeviceListActivity) context).finish();
            }
        });
        holder.adapter_bledevice_rssi.setText(""+models.get(position).getRssi());
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public static class BLEDeviceListHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.adapter_bledevice)
        TextView adapter_bledevice;
        @Bind(R.id.adapter_bledevice_rssi)
        TextView adapter_bledevice_rssi;

        public BLEDeviceListHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
