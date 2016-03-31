package com.renyu.iitebletest.activity;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.common.ParamUtils;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by renyu on 16/3/11.
 */
public class LoginActivity extends BaseActivity {

    @Bind(R.id.login_text)
    EditText login_text;

    @Override
    public int initContentView() {
        return R.layout.activity_login;
    }

    @OnClick(R.id.login)
    public void onClick(View view) {
        if (login_text.getText().equals("")) {
            showToast("请输入工号");
            return;
        }
        ParamUtils.IDS=login_text.getText().toString();
        startActivity(new Intent(LoginActivity.this, CheckActivity.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        ParamUtils.IDS="";
    }
}
