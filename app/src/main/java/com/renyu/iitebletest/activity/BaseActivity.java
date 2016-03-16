package com.renyu.iitebletest.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.renyu.iitebletest.bluetooth.BLEService;
import com.renyu.iitebletest.bluetooth.BLEUtils;
import com.renyu.iitebletest.common.ParamUtils;

import butterknife.ButterKnife;

/**
 * Created by renyu on 16/3/9.
 */
public abstract class BaseActivity extends AppCompatActivity {

    public abstract int initContentView();

    ProgressDialog pd=null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (initContentView()!=0) {
            setContentView(initContentView());
        }
        ButterKnife.bind(this);
    }

    /**
     * 打开蓝牙开关
     */
    public void openBlueTooth() {
        if (BLEUtils.checkBluetoothAvaliable(this)) {
            if (BLEUtils.checkBluetoothOpen(this)) {
                showDialog("提示", "正在绑定");
                scanBLE();
            }
            else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ParamUtils.RESULT_ENABLE_BT);
            }
        }
        else {
            showToast("该设备不支持BLE功能，无法使用牙小白功能");
        }
    }

    public void openBlueTooth(String name) {
        if (BLEUtils.checkBluetoothAvaliable(this)) {
            if (BLEUtils.checkBluetoothOpen(this)) {
                showDialog("提示", "正在绑定");
                scanBLE(name);
            }
            else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ParamUtils.RESULT_ENABLE_BT);
            }
        }
        else {
            showToast("该设备不支持BLE功能，无法使用牙小白功能");
        }
    }

    public void scanBLE() {
        sendCommand(ParamUtils.BLE_COMMAND_SCAN);
    }

    public void scanBLE(String name) {
        sendCommandWithName(ParamUtils.BLE_COMMAND_SCAN, name);
    }

    public void closeBLE() {
        sendCommand(ParamUtils.BLE_COMMAND_CLOSE);
    }

    public void sendCommand(int command) {
        Intent intent=new Intent(this, BLEService.class);
        Bundle bundle=new Bundle();
        bundle.putInt("command", command);
        intent.putExtras(bundle);
        startService(intent);
    }

    public void sendCommandWithName(int command, String name) {
        Intent intent=new Intent(this, BLEService.class);
        Bundle bundle=new Bundle();
        bundle.putInt("command", command);
        bundle.putString("value", name);
        intent.putExtras(bundle);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK) {
            if (requestCode==ParamUtils.RESULT_ENABLE_BT) {
                scanBLE();
            }
        }
        else {
            if (requestCode== ParamUtils.RESULT_ENABLE_BT) {

            }
        }
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void showDialog(String title, String message) {
        pd= ProgressDialog.show(this, title, message);
    }

    public void dismissDialog() {
        if (pd!=null) {
            pd.dismiss();
        }
    }
}
