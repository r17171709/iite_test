package com.zbar.lib;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.renyu.iitebletest.R;
import com.renyu.iitebletest.activity.BaseActivity;

public class CaptureActivity extends BaseActivity implements CaptureCallBackListener {

	CaptureFragment qrcode=null;
	CaptureFragment barcode=null;

	@Override
	public int initContentView() {
		return R.layout.activity_capture;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initViews();
	}

	private void initViews() {
		switchFragment("two");
	}

	@Override
	public void onCaptureSuccess(String result) {
		Intent resultIntent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("result", result);
		resultIntent.putExtras(bundle);
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	@Override
	public void onCaptureFail() {
		Toast.makeText(this, "请允许摄像头使用权限", Toast.LENGTH_LONG).show();
		startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
		finish();
	}

	@Override
	public void onSwitchListener() {

	}

	private void switchFragment(String title) {
		FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();
		if (title.equals("one")) {
			if (barcode!=null) {
				transaction.hide(barcode);
			}
			if (qrcode==null) {
				qrcode= CaptureFragment.getInstance(CaptureFragment.QRCODE);
				transaction.add(R.id.capture_fl, qrcode, "one");
			}
			else {
				transaction.show(qrcode);
			}
		}
		else if (title.equals("two")) {
			if (qrcode!=null) {
				transaction.hide(qrcode);
			}
			if (barcode==null) {
				barcode= CaptureFragment.getInstance(CaptureFragment.BARCODE);
				transaction.add(R.id.capture_fl, barcode, "two");
			}
			else {
				transaction.show(barcode);
			}
		}
		transaction.commit();
	}
}