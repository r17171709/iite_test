package com.renyu.iitebletest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.adapter.MainAdapter;
import com.renyu.iitebletest.bluetooth.QueueUtils;
import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;

import java.util.ArrayList;

import butterknife.Bind;
import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity {

    @Bind(R.id.main_state)
    TextView main_state;
    @Bind(R.id.main_rv)
    RecyclerView main_rv;
    MainAdapter adapter;
    @Bind(R.id.main_value)
    TextView main_value;

    String[] commandsText={
            "设定时间",
            "读取时间",
            "读取牙刷状态",
            "设置userid",
            "读取userid",
            "设置惯用手",
            "恢复出厂设置",
            "获取牙刷当前姿态",
            "获取最近一次刷牙记录",
            "获取所有刷牙记录",
            "获取指定一条刷牙记录",
            "获取当前姿态识别模式",
            "设定姿态识别模式",
            "进入游戏模式",
            "游戏模式的暂停和恢复",
            "清除所有刷牙数据",
            "校准模式的进入和退出",
            "设定led1",
            "设定led2",
            "设定motor",
            "设备reset",
            "获取uniqueid",
            "获取qrcode",
            "进入固件升级模式",
            "读取电池电压",
            "绑定牙刷"};

    int[] commands={
            ParamUtils.BLE_COMMAND_TIME,
            ParamUtils.BLE_COMMAND_GETTIME,
            ParamUtils.BLE_COMMAND_GETSTATE,
            ParamUtils.BLE_COMMAND_SETUSERID,
            ParamUtils.BLE_COMMAND_GETUSERID,
            ParamUtils.BLE_COMMAND_DOMINANT,
            ParamUtils.BLE_COMMAND_FACTORY,
            ParamUtils.BLE_COMMAND_ATTITUDE,
            ParamUtils.BLE_COMMAND_GETCURRENTTEETHINFO,
            ParamUtils.BLE_COMMAND_GETALLINFO,
            ParamUtils.BLE_COMMAND_GETONEINFO,
            ParamUtils.BLE_COMMAND_ATTITUDEMODE,
            ParamUtils.BLE_COMMAND_SETATTITUDEMODE,
            ParamUtils.BLE_COMMAND_GAMESTART,
            ParamUtils.BLE_COMMAND_GAMECONTROL,
            ParamUtils.BLE_COMMAND_CLEANTEETH,
            ParamUtils.BLE_COMMAND_CALIBRATION,
            ParamUtils.BLE_COMMAND_SETLED1,
            ParamUtils.BLE_COMMAND_SETLED2,
            ParamUtils.BLE_COMMAND_SETMOTOR,
            ParamUtils.BLE_COMMAND_SETRESET,
            ParamUtils.BLE_COMMAND_GETUID,
            ParamUtils.BLE_COMMAND_GETQRCODE,
            ParamUtils.BLE_COMMAND_UPDATE,
            ParamUtils.BLE_COMMAND_GETV,
            ParamUtils.BLE_COMMAND_BINDTEETH};

    ArrayList<String> commandList;

    @Override
    public int initContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        commandList=new ArrayList<>();

        QueueUtils.getInstance();

        initViews();

        EventBus.getDefault().register(this);

        //openBlueTooth();
        startActivityForResult(new Intent(MainActivity.this, BLEDeviceListActivity.class), ParamUtils.RESULT_SCANBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK && requestCode==ParamUtils.RESULT_QRCODE) {

            Intent intent=new Intent(MainActivity.this, BLEDeviceListActivity.class);
            Bundle bundle=new Bundle();
            bundle.putString("name", data.getExtras().getString("result"));
            intent.putExtras(bundle);
            startActivityForResult(intent, ParamUtils.RESULT_SCANBLE);
        }
        else if (resultCode==RESULT_OK && requestCode==ParamUtils.RESULT_SCANBLE) {
            sendCommandWithName(ParamUtils.BLE_COMMAND_CONNECT, data.getExtras().getString("address"));
            main_state.setText("正在连接设备中");
        }
    }

    private void initViews() {
        for (String command : commandsText) {
            commandList.add(command);
        }
        adapter=new MainAdapter(this, commandList);
        adapter.setOnItemClickListener(new MainAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                sendCommand(commands[position]);
            }
        });
        main_rv.setHasFixedSize(true);
        main_rv.setLayoutManager(new LinearLayoutManager(this));
    }

    public void onEventMainThread(BLECommandModel model) {
        if (model.getCommand()==ParamUtils.BLE_COMMAND_GETINFO) {
            main_state.setText("设备"+model.getValue()+"已经连接");
        }
        else {
            main_value.setText(model.getValue());
        }
    }

    public void onEventMainThread(BLEConnectModel model) {
        changeState(model.getBlestate());
    }

    private void changeState(BLEConnectModel.BLESTATE blestate) {
        if (blestate== BLEConnectModel.BLESTATE.STATE_CONNECTED) {
            dismissDialog();
            sendCommand(ParamUtils.BLE_COMMAND_GETINFO);
            main_rv.setAdapter(adapter);
        }
        else if (blestate== BLEConnectModel.BLESTATE.STATE_DISCONNECTED ||
                blestate== BLEConnectModel.BLESTATE.STATE_NOSCAN ||
                blestate== BLEConnectModel.BLESTATE.STATE_CANCELSCAN ||
                blestate== BLEConnectModel.BLESTATE.STATE_MOREDEVICE) {
            dismissDialog();
            main_state.setText("设备已断开");
        }
        else {
            main_state.setText("正在连接设备中");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

}
