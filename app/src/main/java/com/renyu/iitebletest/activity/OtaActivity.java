package com.renyu.iitebletest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Utils;
import com.renyu.iitebletest.bluetooth.QueueUtils;
import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;

import de.greenrobot.event.EventBus;

/**
 * Created by renyu on 16/4/1.
 */
public class OtaActivity extends BaseActivity {

    String filePath= Environment.getExternalStorageDirectory().getPath()+"/CySmart/20160405V1.1.2FOR1.5.cyacd";

    @Override
    public int initContentView() {
        return 0;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        QueueUtils.getInstance();

        EventBus.getDefault().register(this);

        startActivityForResult(new Intent(OtaActivity.this, BLEDeviceListActivity.class), ParamUtils.RESULT_SCANBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==ParamUtils.RESULT_SCANBLE && resultCode==RESULT_OK) {
            sendCommandWithName(ParamUtils.BLE_COMMAND_CONNECT, data.getExtras().getString("address"));
        }
    }

    public void onEventMainThread(BLECommandModel model) {
        if (ParamUtils.BLE_COMMAND_UPDATE==model.getCommand()) {
            startActivityForResult(new Intent(OtaActivity.this, BLEDeviceListActivity.class), ParamUtils.RESULT_SCANBLE);
        }

    }

    public void onEventMainThread(BLEConnectModel model) {
        if (model.getBlestate()== BLEConnectModel.BLESTATE.STATE_CONNECTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommand(ParamUtils.BLE_COMMAND_UPDATE);
                }
            }, 1000);
        }
        else if (model.getBlestate()== BLEConnectModel.BLESTATE.STATE_DISCONNECTED) {
            Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_ONE_NAME, "Default");
            Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_PATH, "Default");
            Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_NAME, "Default");
            Utils.setStringSharedPreference(this, Constants.PREF_BOOTLOADER_STATE, "Default");
            Utils.setIntSharedPreference(this, Constants.PREF_PROGRAM_ROW_NO, 0);
        }
        else if (model.getBlestate()==BLEConnectModel.BLESTATE.STATE_OTA) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommandWithName(ParamUtils.OTAEnterBootLoaderCmd, filePath);
                }
            }, 1000);
        }
    }
}
