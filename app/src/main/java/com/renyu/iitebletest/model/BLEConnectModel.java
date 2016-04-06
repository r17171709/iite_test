package com.renyu.iitebletest.model;

/**
 * Created by renyu on 16/3/3.
 */
public class BLEConnectModel {

    public enum BLESTATE {
        //BLE 已连接
        STATE_CONNECTED,
        //BLE 断开连接
        STATE_DISCONNECTED,
        //BLE 正在连接
        STATE_CONNECTING,
        //BLE 正在扫描
        STATE_SCAN,
        //BLE 初始状态
        STATE_NOSCAN,
        //BLE 取消扫描
        STATE_CANCELSCAN,
        //BLE 发现多设备
        STATE_MOREDEVICE,
        //BLE OTA
        STATE_OTA
    }

    BLESTATE blestate;

    public BLESTATE getBlestate() {
        return blestate;
    }

    public void setBlestate(BLESTATE blestate) {
        this.blestate = blestate;
    }
}
