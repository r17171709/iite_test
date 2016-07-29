package com.renyu.iitebletest.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.adapter.BLEDeviceListAdapter;
import com.renyu.iitebletest.common.ACache;
import com.renyu.iitebletest.model.BLEConnectModel;

import java.util.ArrayList;

import butterknife.Bind;
import de.greenrobot.event.EventBus;

/**
 * Created by renyu on 16/3/31.
 */
public class BLEDeviceListActivity extends BaseActivity {

    @Bind(R.id.device_list_title)
    Toolbar device_list_title;
    @Bind(R.id.device_list)
    RecyclerView device_list;
    BLEDeviceListAdapter adapter;

    ArrayList<com.renyu.iitebletest.bluetooth.BluetoothDevice> models;

    @Override
    public int initContentView() {
        return R.layout.activity_bledevicelst;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        models=new ArrayList<>();

        initViews();

        EventBus.getDefault().register(this);

        if (getIntent().getExtras()!=null && getIntent().getExtras().getString("name")!=null) {
            openBlueTooth(getIntent().getExtras().getString("name"));
        }
        else {
            openBlueTooth();
        }
    }

    private void initViews() {
        device_list_title.setTitle("");
        setSupportActionBar(device_list_title);
        adapter=new BLEDeviceListAdapter(this, models);
        device_list.setHasFixedSize(true);
        device_list.setLayoutManager(new LinearLayoutManager(this));
        device_list.setAdapter(adapter);
    }

    public void onEventMainThread(com.renyu.iitebletest.bluetooth.BluetoothDevice model) {
        if (getIntent().getExtras().getInt("type")==2) {
            if (ACache.get(this).getAsObject("finishOta")!=null) {
                ArrayList<String> otas= (ArrayList<String>) ACache.get(this).getAsObject("finishOta");
                if (otas.contains(model.getDevice().getAddress())) {
                    return;
                }
            }
        }
        if (models.size()==0) {
            models.add(model);
            adapter.notifyItemInserted(0);
        }
        else {
            for (int i = 0; i < models.size(); i++) {
                if (models.get(i).getRssi()<model.getRssi()) {
                    models.add(i, model);
                    adapter.notifyItemInserted(i);
                    return;
                }
                if (i==models.size()-1) {
                    models.add(model);
                    adapter.notifyItemInserted(i);
                    return;
                }
            }
        }

    }

    public void onEventMainThread(BLEConnectModel model) {
        if (model.getBlestate()== BLEConnectModel.BLESTATE.STATE_NOSCAN) {
            dismissDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }
}
