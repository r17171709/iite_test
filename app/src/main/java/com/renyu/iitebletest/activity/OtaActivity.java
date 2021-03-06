package com.renyu.iitebletest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Utils;
import com.renyu.iitebletest.R;
import com.renyu.iitebletest.common.ACache;
import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

/**
 * Created by renyu on 16/4/1.
 */
public class OtaActivity extends BaseActivity {

    @Bind(R.id.ota_desp)
    TextView ota_desp;
    @Bind(R.id.ota_begin)
    Button ota_begin;

    String filePath1;
    String filePath2;

    int otaStep;

    String lastAddress="";
    String cpuid="";

    @Override
    public int initContentView() {
        return R.layout.activity_ota;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        otaStep = 0;
        filePath1= Environment.getExternalStorageDirectory().getPath()+File.separator+"20160828V1.3.1FOR1.6.cyacd";
        filePath2= Environment.getExternalStorageDirectory().getPath()+File.separator+"20160828V1.3.1FOR1.7.cyacd";
        ota_desp.setText("点击【开始】进行扫描");

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        closeBLE();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==ParamUtils.RESULT_SCANBLE && resultCode==RESULT_OK) {
            ota_desp.setText("连接中...");
            lastAddress=data.getExtras().getString("address");
            sendCommandWithName(ParamUtils.BLE_COMMAND_CONNECT, lastAddress);
        }
        if (requestCode==ParamUtils.RESULT_SCANBLE && resultCode==RESULT_CANCELED) {
            ota_desp.setText("没有设备可以升级了");
        }
    }

    @OnClick({R.id.ota_begin})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ota_begin: {
                if(otaStep == 0) {
                    Intent intent = new Intent(OtaActivity.this, BLEDeviceListActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("type", 2);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, ParamUtils.RESULT_SCANBLE);
                    ota_begin.setVisibility(View.GONE);
                    break;
                }
                else if(otaStep == 1)
                {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendCommand(ParamUtils.BLE_COMMAND_UPDATE);
                            ota_desp.setText("准备进入固件升级");
                        }
                    }, 1000);
                    ota_begin.setVisibility(View.GONE);
                    break;
                }
            }
        }
    }

    public void onEventMainThread(BLECommandModel model) {
        if (ParamUtils.OTA_COMMAND_PROGRESS==model.getCommand()) {
            String value=model.getValue();
            ota_desp.setText("升级已完成："+value+"%");
        }
        else if (ParamUtils.OTA_COMMAND_END==model.getCommand()) {
            ota_desp.setText("升级成功");
            if (ACache.get(this).getAsObject("finishOta")==null) {
                ArrayList<String> otas=new ArrayList<>();
                otas.add(lastAddress);
                ACache.get(this).put("finishOta", otas);
            }
            else {
                ArrayList<String> otas= (ArrayList<String>) ACache.get(this).getAsObject("finishOta");
                otas.add(lastAddress);
                ACache.get(this).put("finishOta", otas);
            }
            lastAddress="";
            closeBLE();
            otaStep = 0;
            ota_begin.setVisibility(View.VISIBLE);
        }
        else if (ParamUtils.OTA_COMMAND_ERROR==model.getCommand()) {
            ota_desp.setText("升级失败");
            closeBLE();
            otaStep = 0;
            ota_begin.setVisibility(View.VISIBLE);
        }
        else if (model.getCommand()==ParamUtils.BLE_COMMAND_CPUID) {
            cpuid=model.getValue();
            ota_desp.setText("BLE连接成功");
            ota_begin.setVisibility(View.VISIBLE);
            otaStep = 1;
        }
    }

    public void onEventMainThread(BLEConnectModel model) {
        if (model.getBlestate()== BLEConnectModel.BLESTATE.STATE_CONNECTED) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommand(ParamUtils.BLE_COMMAND_CPUID);
                }
            }, 1000);

        }
        else if (model.getBlestate()== BLEConnectModel.BLESTATE.STATE_DISCONNECTED) {
            Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_ONE_NAME, "Default");
            Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_PATH, "Default");
            Utils.setStringSharedPreference(this, Constants.PREF_OTA_FILE_TWO_NAME, "Default");
            Utils.setStringSharedPreference(this, Constants.PREF_BOOTLOADER_STATE, "Default");
            Utils.setIntSharedPreference(this, Constants.PREF_PROGRAM_ROW_NO, 0);

            ota_desp.setText("BLE设备连接断开");
            //如果在刷机过程中断开，则重连这个设备继续刷
            if (!lastAddress.equals("")) {
                sendCommandWithName(ParamUtils.BLE_COMMAND_CONNECT, lastAddress);
            }
            //如果在刷机结束后断开，则重写扫描
//            else {
//                Intent intent=new Intent(OtaActivity.this, BLEDeviceListActivity.class);
//                Bundle bundle=new Bundle();
//                bundle.putInt("type", 2);
//                intent.putExtras(bundle);
//                startActivityForResult(intent, ParamUtils.RESULT_SCANBLE);
//            }
        }
        else if (model.getBlestate()==BLEConnectModel.BLESTATE.STATE_OTA) {
            ota_desp.setText("BLE进入固件升级模式");
            if(cpuid.equals("1.6"))
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendCommandWithName(ParamUtils.OTAEnterBootLoaderCmd, filePath1);
                    }
                }, 1000);
            else if(cpuid.equals("1.7"))
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendCommandWithName(ParamUtils.OTAEnterBootLoaderCmd, filePath2);
                    }
                }, 1000);
        }
    }
}
