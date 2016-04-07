package com.renyu.iitebletest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.bluetooth.QueueUtils;
import com.renyu.iitebletest.common.ACache;
import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.common.RetrofitUtils;
import com.renyu.iitebletest.db.Dao;
import com.renyu.iitebletest.model.BLECheckModel;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;
import com.zbar.lib.CaptureActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by renyu on 16/3/11.
 */
public class CheckActivity extends BaseActivity {

    @Bind(R.id.check_title)
    Toolbar check_title;
    @Bind(R.id.check_state)
    TextView check_state;
    @Bind(R.id.rssi_result)
    ImageView rssi_result;
    @Bind(R.id.command_result)
    ImageView command_result;
    @Bind(R.id.battery_result)
    ImageView battery_result;

    boolean battery;
    int batteryNum=0;
    boolean rssi;
    int rssiNum=0;
    boolean command;
    String uniqueid;
    String deviceName="";
    String cpuid="";

    @Override
    public int initContentView() {
        return R.layout.activity_check;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        check_title.setTitle("");
        setSupportActionBar(check_title);
        check_title.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId()==R.id.menu_check_setting) {
                    startActivity(new Intent(CheckActivity.this, SettingActivity.class));
                }
                if (item.getItemId()==R.id.menu_check_upload) {
                    if (ParamUtils.currentUploadCount==ParamUtils.totalUploadCount) {
                        List<BLECheckModel> lists=Dao.getInstance(CheckActivity.this).getAllData();
                        if (lists.size()==0) {
                            showToast("暂无数据");
                            return false;
                        }
                        showToast("开始上传，上传结束之前，将不能再次执行上传工作");
                        ParamUtils.totalUploadCount=lists.size();
                        ParamUtils.currentUploadCount=0;
                        for (BLECheckModel list : lists) {
                            RetrofitUtils.getInstance().upload(list.getCpuid(), list.getBd_sn(), list.getQc_id_a(), list.getQc_date_a(),
                                    list.getBd_old(), list.getBd_rssi(), Boolean.parseBoolean(list.getBd_swith()),
                                    Boolean.parseBoolean(list.getQc_result_a()), CheckActivity.this);
                        }
                    }
                    else {
                        showToast("正在上传，请稍后再试");
                    }
                }
                return false;
            }
        });

        QueueUtils.getInstance();

        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_check, menu);
        return true;
    }

    @OnClick({R.id.save_result, R.id.check_qrcode, R.id.check_scan})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_result:
                if (uniqueid.equals("")) {
                    showToast("无设备唯一ID不能保存");
                    return;
                }
                BLECheckModel checkModel=new BLECheckModel();
                checkModel.setBd_sn(uniqueid);
                checkModel.setQc_id_a(ParamUtils.IDS);
                checkModel.setQc_date_a(""+System.currentTimeMillis());
                checkModel.setBd_old(""+batteryNum);
                if (battery && !command && rssi) {
                    checkModel.setQc_result_a(""+true);
                }
                else {
                    checkModel.setQc_result_a(""+false);
                }
                checkModel.setBd_swith(""+command);
                checkModel.setBd_rssi(""+rssiNum);
                checkModel.setCpuid(cpuid);
                Dao.getInstance(this).addData(checkModel);
                showToast("保存成功");
                closeBLE();
                break;
            case R.id.check_qrcode:
                closeBLE();
                if (ACache.get(this).getAsString("rssi")==null || ACache.get(this).getAsString("battery")==null) {
                    showToast("请输入相应测试限额");
                    startActivity(new Intent(CheckActivity.this, SettingActivity.class));
                    return;
                }
                Intent intent=new Intent(CheckActivity.this, CaptureActivity.class);
                startActivityForResult(intent, ParamUtils.RESULT_QRCODE);
                break;
            case R.id.check_scan:
                closeBLE();
                if (ACache.get(this).getAsString("rssi")==null || ACache.get(this).getAsString("battery")==null) {
                    showToast("请输入相应测试限额");
                    startActivity(new Intent(CheckActivity.this, SettingActivity.class));
                    return;
                }
                startActivityForResult(new Intent(CheckActivity.this, BLEDeviceListActivity.class), ParamUtils.RESULT_SCANBLE);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);

        closeBLE();
    }

    public void onEventMainThread(BLECommandModel model) {
        if (model.getCommand()== ParamUtils.BLE_COMMAND_GETINFO) {
            deviceName=model.getValue();
            check_state.setText("设备"+deviceName+"已经连接");
        }
        else if (model.getCommand()==ParamUtils.BLE_COMMAND_RSSI) {
            if (Math.abs(Integer.parseInt(model.getValue()))<Integer.parseInt(ACache.get(this).getAsString("rssi"))) {
                rssi_result.setImageResource(R.mipmap.ic_state3);
                rssi=true;
            }
            else {
                rssi_result.setImageResource(R.mipmap.ic_state2);
                rssi=false;
            }
            rssiNum=Integer.parseInt(model.getValue());
        }
        else if (model.getCommand()==ParamUtils.BLE_COMMAND_BATTERY) {
            if (Math.abs(Integer.parseInt(model.getValue()))>Integer.parseInt(ACache.get(this).getAsString("battery"))) {
                battery_result.setImageResource(R.mipmap.ic_state3);
                battery=true;
            }
            else {
                battery_result.setImageResource(R.mipmap.ic_state2);
                battery=false;
            }
            batteryNum=Integer.parseInt(model.getValue());
        }
        else if (model.getCommand()==ParamUtils.BLE_COMMAND_TEST) {
            try {
                JSONObject jsonObject=new JSONObject(model.getValue());
                if (jsonObject.getInt("result")==1) {
                    command_result.setImageResource(R.mipmap.ic_state3);
                    command=true;
                }
                else {
                    command_result.setImageResource(R.mipmap.ic_state2);
                    command=false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                command_result.setImageResource(R.mipmap.ic_state2);
                command=false;
            }
        }
        else if (model.getCommand()==ParamUtils.BLE_COMMAND_CPUID) {
            cpuid=model.getValue();
        }
        else if (model.getCommand()==ParamUtils.BLE_COMMAND_GETUID) {
            try {
                JSONObject object=new JSONObject(model.getValue());
                uniqueid=object.getJSONObject("data").getString("uniqueid");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onEventMainThread(BLEConnectModel model) {
        changeState(model.getBlestate());
    }

    private void changeState(BLEConnectModel.BLESTATE blestate) {
        if (blestate== BLEConnectModel.BLESTATE.STATE_CONNECTED) {
            dismissDialog();
            Observable.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    sendCommand(ParamUtils.BLE_COMMAND_SETMOTOR);
                    sendCommand(ParamUtils.BLE_COMMAND_GETINFO);
                    sendCommand(ParamUtils.BLE_COMMAND_RSSI);
                    sendCommand(ParamUtils.BLE_COMMAND_BATTERY);
                    sendCommand(ParamUtils.BLE_COMMAND_GETUID);
                    sendCommand(ParamUtils.BLE_COMMAND_TEST);
                    sendCommand(ParamUtils.BLE_COMMAND_CPUID);
                }
            });
        }
        else if (blestate== BLEConnectModel.BLESTATE.STATE_NOSCAN) {
            dismissDialog();
            check_state.setText("扫描结束");
        }
        else if (blestate== BLEConnectModel.BLESTATE.STATE_DISCONNECTED) {
            dismissDialog();
            check_state.setText("设备已断开");
            //断开连接之后重新初始化
            rssi_result.setImageResource(R.mipmap.ic_state1);
            command_result.setImageResource(R.mipmap.ic_state1);
            battery_result.setImageResource(R.mipmap.ic_state1);

            rssi=false;
            rssiNum=0;
            command=false;
            battery=false;
            batteryNum=0;
            uniqueid="";
            deviceName="";
            cpuid="";
        }
        else if (blestate== BLEConnectModel.BLESTATE.STATE_CANCELSCAN) {
            dismissDialog();
            check_state.setText("扫描取消");
        }
        else {
            dismissDialog();
            check_state.setText("正在连接设备中");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode==RESULT_OK && requestCode==ParamUtils.RESULT_QRCODE) {

            Intent intent=new Intent(CheckActivity.this, BLEDeviceListActivity.class);
            Bundle bundle=new Bundle();
            bundle.putString("name", data.getExtras().getString("result"));
            intent.putExtras(bundle);
            startActivityForResult(intent, ParamUtils.RESULT_SCANBLE);
        }
        else if (resultCode==RESULT_OK && requestCode==ParamUtils.RESULT_SCANBLE) {
            sendCommandWithName(ParamUtils.BLE_COMMAND_CONNECT, data.getExtras().getString("address"));
            check_state.setText("正在连接设备中");
        }
    }
}
