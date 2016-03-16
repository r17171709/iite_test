package com.renyu.iitebletest.bluetooth;

import android.bluetooth.BluetoothAdapter;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * BLE连接回调类
 * Created by renyu on 16/2/29.
 */
public abstract class MyLeScanCallback implements BluetoothAdapter.LeScanCallback {

    private int timeSeconds;
    private BluetoothAdapter adapter;

    private Subscription subscription;

    private MyLeScanCallback() {

    }

    public MyLeScanCallback(int timeSeconds, BluetoothAdapter adapter) {
        this.timeSeconds=timeSeconds;
        this.adapter=adapter;
    }

    /**
     * 开始计时
     */
    public void startSetTime() {
        subscription=Observable.timer(timeSeconds, TimeUnit.SECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                adapter.stopLeScan(MyLeScanCallback.this);
                scanEndCallBack();
            }
        });
    }

    /**
     * 取消扫描
     */
    public void cancelScan() {
        subscription.unsubscribe();
        adapter.stopLeScan(MyLeScanCallback.this);
        scanCancelCallBack();
    }

    /**
     * 取消计时回调
     */
    public abstract void scanCancelCallBack();

    /**
     * 计时结束回调
     */
    public abstract void scanEndCallBack();
}
