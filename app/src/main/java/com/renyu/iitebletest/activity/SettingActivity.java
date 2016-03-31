package com.renyu.iitebletest.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.common.ACache;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by renyu on 16/3/11.
 */
public class SettingActivity extends BaseActivity {

    @Bind(R.id.setting_rssi)
    EditText setting_rssi;
    @Bind(R.id.setting_battery)
    EditText setting_battery;

    @Override
    public int initContentView() {
        return R.layout.activity_setting;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
    }

    private void initViews() {
        setting_rssi.setText(ACache.get(this).getAsString("rssi")!=null?ACache.get(this).getAsString("rssi"):"0");
        setting_battery.setText(ACache.get(this).getAsString("battery")!=null?ACache.get(this).getAsString("battery"):"0");
    }

    @OnClick(R.id.setting)
    public void onClick(View view) {
        if (setting_rssi.getText().toString().equals("") || setting_battery.getText().toString().equals("")) {
            showToast("请输入相应测试限额");
            return;
        }
        ACache.get(this).put("rssi", setting_rssi.getText().toString());
        ACache.get(this).put("battery", setting_battery.getText().toString());
        showToast("设置成功");
        finish();
    }
}
