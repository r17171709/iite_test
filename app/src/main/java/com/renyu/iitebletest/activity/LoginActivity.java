package com.renyu.iitebletest.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
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
    TextInputLayout login_text;
    EditText login_edit;

    @Override
    public int initContentView() {
        return R.layout.activity_login;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
    }

    private void initViews() {
        login_edit=login_text.getEditText();
    }

    @OnClick(R.id.login)
    public void onClick(View view) {
        if (login_edit.getText().equals("")) {
            showToast("请输入工号");
            return;
        }
        ParamUtils.IDS=login_edit.getText().toString();
        startActivity(new Intent(LoginActivity.this, CheckActivity.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        ParamUtils.IDS="";
    }
}
