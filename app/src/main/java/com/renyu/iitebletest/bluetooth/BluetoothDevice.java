package com.renyu.iitebletest.bluetooth;

/**
 * Created by renyu on 16/2/29.
 */
public class BluetoothDevice {
    android.bluetooth.BluetoothDevice device;
    int rssi;

    public android.bluetooth.BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(android.bluetooth.BluetoothDevice device) {
        this.device = device;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
