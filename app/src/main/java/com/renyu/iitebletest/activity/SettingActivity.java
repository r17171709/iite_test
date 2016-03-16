package com.renyu.iitebletest.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
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
    TextInputLayout setting_rssi;
    EditText rssi;
    @Bind(R.id.setting_battery)
    TextInputLayout setting_battery;
    EditText battery;

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
        rssi=setting_rssi.getEditText();
        rssi.setText(ACache.get(this).getAsString("rssi")!=null?ACache.get(this).getAsString("rssi"):"0");
        battery=setting_battery.getEditText();
        battery.setText(ACache.get(this).getAsString("battery")!=null?ACache.get(this).getAsString("battery"):"0");
    }

    @OnClick(R.id.setting)
    public void onClick(View view) {
        if (rssi.getText().toString().equals("") || battery.getText().toString().equals("")) {
            showToast("请输入相应测试限额");
            return;
        }
        ACache.get(this).put("rssi", rssi.getText().toString());
        ACache.get(this).put("battery", battery.getText().toString());
        showToast("设置成功");
        finish();
    }
}
