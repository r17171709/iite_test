package com.renyu.iitebletest.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.renyu.iitebletest.common.ACache;
import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by renyu on 16/2/29.
 */
public class BLEService extends Service {

    MyLeScanCallback callback;
    //扫描到的所有设备
    HashMap<String, com.renyu.iitebletest.bluetooth.BluetoothDevice> deviceHashMap;
    //BLE通用属性配置文件
    BluetoothGatt gatt;
    //当前设备状态
    public static BLEConnectModel.BLESTATE blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;
    //BLE整体配置
    Subscription bleSubscription;

    //断开连接是否需要发送广播
    boolean needBroadcast=true;

    @Override
    public void onCreate() {
        super.onCreate();

        blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;
        deviceHashMap=new HashMap<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!=null && intent.getExtras()!=null) {
            int command=intent.getExtras().getInt("command");
            if (command== ParamUtils.BLE_COMMAND_SCAN) {
                Log.d("BLEService", "开始搜索BLE");

                BLEConnectModel model=new BLEConnectModel();
                model.setBlestate(BLEConnectModel.BLESTATE.STATE_SCAN);
                EventBus.getDefault().post(model);

                scan(intent.getExtras().getString("value"));
            }
            else if (command==ParamUtils.BLE_COMMAND_RSSI) {
                if (gatt!=null)
                gatt.readRemoteRssi();
            }
            else if (command==ParamUtils.BLE_COMMAND_SEND) {
                writeCharacteristic(ParamUtils.UUID_SERVICE_WRITE, intent.getExtras().getByteArray("value"));
            }
            else if (command==ParamUtils.BLE_COMMAND_READ) {
                readCharacteristic((UUID) (intent.getExtras().getSerializable("serviceUUID")), (UUID) intent.getExtras().getSerializable("characUUID"));
            }
            else if (command==ParamUtils.BLE_COMMAND_CLOSE) {
                needBroadcast=false;
                if (callback!=null) {
                    callback.cancelScan();
                }
                closeAllBLEConnect();
            }
            else if (command==ParamUtils.BLE_COMMAND_SAVE) {
                ACache.get(this).put("lastIiteName", gatt.getDevice().getName());
            }
            else if (command==ParamUtils.BLE_COMMAND_GETINFO) {
                BLECommandModel model=new BLECommandModel();
                model.setCommand(ParamUtils.BLE_COMMAND_GETINFO);
                model.setValue(gatt.getDevice().getName());
                EventBus.getDefault().post(model);
            }
            else if (command==ParamUtils.BLE_COMMAND_TIME) {
                HashMap<String, String> params=new HashMap<>();
                Calendar calendar=Calendar.getInstance();
                SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                params.put("time", format.format(calendar.getTime()));
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_TIME, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETTIME) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETTIME, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETSTATE) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETSTATE, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_BINDTEETH) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_BINDTEETH, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETUID) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETUID, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETUSERID) {
                HashMap<String, String> params=new HashMap<>();
                params.put("userid", "123123abcabc");
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETUSERID, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETUSERID) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETUSERID, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_FACTORY) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_FACTORY, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_ATTITUDE) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_ATTITUDE, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETCURRENTTEETHINFO) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETCURRENTTEETHINFO, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETALLINFO) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETALLINFO, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETONEINFO) {
                HashMap<String, String> params=new HashMap<>();
                params.put("serial", "1");
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETONEINFO, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_ATTITUDEMODE) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_ATTITUDEMODE, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETATTITUDEMODE) {
                HashMap<String, String> params=new HashMap<>();
                params.put("model", "1");
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETATTITUDEMODE, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETLED1) {
                HashMap<String, String> params=new HashMap<>();
                params.put("state", "1");
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETLED1, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETLED2) {
                HashMap<String, String> params=new HashMap<>();
                params.put("state", "1");
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETLED2, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETMOTOR) {
                HashMap<String, String> params=new HashMap<>();
                params.put("state", "1");
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETMOTOR, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETRESET) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETRESET, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETQRCODE) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETQRCODE, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_UPDATE) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_UPDATE, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GETV) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GETV, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_BATTERY) {
                QueueUtils.getInstance().addReadTask(ParamUtils.UUID_SERVICE_BATTERY, ParamUtils.UUID_SERVICE_BATTERY_READ, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_INFONAME) {
                QueueUtils.getInstance().addReadTask(ParamUtils.UUID_SERVICE_DEVICEINFO, ParamUtils.UUID_SERVICE_DEVICEINFO_NAME, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_INFOID) {
                QueueUtils.getInstance().addReadTask(ParamUtils.UUID_SERVICE_DEVICEINFO, ParamUtils.UUID_SERVICE_DEVICEINFO_ID, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_INFOVERSION) {
                QueueUtils.getInstance().addReadTask(ParamUtils.UUID_SERVICE_DEVICEINFO, ParamUtils.UUID_SERVICE_DEVICEINFO_VERSION, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_LOGOUT) {
                needBroadcast=false;
                if (callback!=null) {
                    callback.cancelScan();
                }
                closeAllBLEConnect();
                ACache.get(this).remove("lastIiteName");
            }
            else if (command==ParamUtils.BLE_COMMAND_TEST) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_TEST, null, this);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 扫描BLE
     */
    private void scan(final String name) {
        if (blestate== BLEConnectModel.BLESTATE.STATE_SCAN) {
            return;
        }
        deviceHashMap.clear();
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        callback=new MyLeScanCallback(10, adapter) {
            @Override
            public void scanCancelCallBack() {
                Log.d("BLEService", "取消扫描");

                if (blestate==BLEConnectModel.BLESTATE.STATE_SCAN) {
                    blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                    BLEConnectModel model=new BLEConnectModel();
                    model.setBlestate(BLEConnectModel.BLESTATE.STATE_CANCELSCAN);
                    EventBus.getDefault().post(model);
                }
            }

            @Override
            public void scanEndCallBack() {
                Log.d("BLEService", "扫描结束");

                if (deviceHashMap.size()==0) {
                    Log.d("BLEService", "没有扫描到设备");

                    blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                    BLEConnectModel model=new BLEConnectModel();
                    model.setBlestate(BLEConnectModel.BLESTATE.STATE_NOSCAN);
                    EventBus.getDefault().post(model);

                    return;
                }
                if (deviceHashMap.size()>4) {
                    Log.d("BLEService", "设备太多了");

                    blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                    BLEConnectModel model=new BLEConnectModel();
                    model.setBlestate(BLEConnectModel.BLESTATE.STATE_MOREDEVICE);
                    EventBus.getDefault().post(model);

                    return;
                }
                callback=null;
                int rssi=-10000;
                BluetoothDevice tempDevice=null;
                //循环遍历找出信号最强的设备
                Iterator<Map.Entry<String, com.renyu.iitebletest.bluetooth.BluetoothDevice>> devices=deviceHashMap.entrySet().iterator();
                while (devices.hasNext()) {
                    Map.Entry<String, com.renyu.iitebletest.bluetooth.BluetoothDevice> entry=devices.next();
                    com.renyu.iitebletest.bluetooth.BluetoothDevice device=entry.getValue();
                    if (rssi<device.getRssi()) {
                        tempDevice=device.getDevice();
                        rssi=device.getRssi();
                    }
                }
                connectBLE(tempDevice);
            }

            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("BLEService", device.getAddress()+" "+device.getName()+" "+device.getUuids()+" "+rssi);
                com.renyu.iitebletest.bluetooth.BluetoothDevice device1=new com.renyu.iitebletest.bluetooth.BluetoothDevice();
                device1.setDevice(device);
                device1.setRssi(rssi);
                if (device.getName()!=null && device.getName().startsWith("iite")) {
                    if (name==null || name.equals("")) {
                        deviceHashMap.put(device.getName(), device1);
                    }
                    else {
                        if (device.getName().equals(name) || device.getName().substring(device.getName().indexOf("-")+1).equals(name)) {
                            deviceHashMap.put(device.getName(), device1);
                        }
                    }
                }
            }
        };
        callback.startSetTime();
        adapter.startLeScan(new UUID[]{ParamUtils.UUID_SERVICE_MILI}, callback);
        blestate= BLEConnectModel.BLESTATE.STATE_SCAN;
    }

//    public void userLastDevice() {
//        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
//        connectBLE(adapter.getRemoteDevice(ACache.get(this).getAsString("lastIiteAddress")));
//    }

    private void connectBLE(BluetoothDevice device) {
        closeAllBLEConnect();
        //超过30s还不能连接上，直接认为连接失败
        bleSubscription= Observable.timer(30, TimeUnit.SECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                closeAllBLEConnect();
            }
        });

        BLEConnectModel model=new BLEConnectModel();
        model.setBlestate(BLEConnectModel.BLESTATE.STATE_CONNECTING);
        EventBus.getDefault().post(model);

        blestate= BLEConnectModel.BLESTATE.STATE_CONNECTING;

        Log.d("BLEService", "开始连接");

        gatt=device.connectGatt(this, false, new MyBluetoothGattCallback() {
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                QueueUtils.getInstance().putReadCommand(characteristic.getValue(), characteristic.getUuid());
                QueueUtils.getInstance().release();
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                QueueUtils.getInstance().release();
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.d("BLEService", "收到指令"+characteristic.getValue()[0]+" "+characteristic.getValue()[1]+" "+characteristic.getValue()[2]);
//                byte[] receive=new byte[characteristic.getValue().length-3];
//                for (int i=0;i<receive.length;i++) {
//                    receive[i]=characteristic.getValue()[i+3];
//                }
//                Log.d("BLEService", "指令结果:"+new String(receive));
                QueueUtils.getInstance().putCommand(characteristic.getValue());
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        gatt.discoverServices();
                        Log.d("BLEService", "BLE设备连接成功");
                        Log.d("BLEService", "BLE设备正在配置服务中");
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.d("BLEService", "BLE设备连接断开");

                        bleSubscription.unsubscribe();

                        gatt.close();

                        BLEConnectModel model=new BLEConnectModel();
                        model.setBlestate(BLEConnectModel.BLESTATE.STATE_DISCONNECTED);
                        EventBus.getDefault().post(model);

                        if (needBroadcast) {
                            Intent intent=new Intent();
                            intent.setAction("BLE_RETRY");
                            sendBroadcast(intent);
                        }
                        needBroadcast=true;

                        blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status==BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLEService", "BLE设备配置服务成功");

                    if (gatt.getService(ParamUtils.UUID_SERVICE_MILI)==null) {
                        Log.d("BLEService", "虽然BLE设备配置服务成功，但是服务已经关闭");

                        closeAllBLEConnect();
                    }
                    else {
                        //开启通知服务
                        BluetoothGattCharacteristic characteristic = gatt.getService(ParamUtils.UUID_SERVICE_MILI).getCharacteristic(ParamUtils.UUID_SERVICE_READ);
                        if (enableNotification(characteristic, gatt)) {
                            BLEConnectModel model=new BLEConnectModel();
                            model.setBlestate(BLEConnectModel.BLESTATE.STATE_CONNECTED);
                            EventBus.getDefault().post(model);

                            blestate= BLEConnectModel.BLESTATE.STATE_CONNECTED;

                            bleSubscription.unsubscribe();

                            Intent intent=new Intent();
                            intent.setAction("BLE_RETRY_DIS");
                            sendBroadcast(intent);
                        }
                        else {
                            closeAllBLEConnect();
                        }
                    }
                }
                else {
                    Log.d("BLEService", "BLE设备配置服务失败");

                    closeAllBLEConnect();
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);

                BLECommandModel model=new BLECommandModel();
                model.setCommand(ParamUtils.BLE_COMMAND_RSSI);
                model.setValue(""+rssi);
                EventBus.getDefault().post(model);
            }
        });
    }

    /**
     * 关闭所有gatt连接
     */
    private void closeAllBLEConnect() {
        if (gatt!=null) {
            gatt.disconnect();
//            refreshDeviceCache(gatt);
            gatt=null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Clears the device cache. After uploading new hello4 the DFU target will have other services than before.
     * @param gatt
     * @return
     */
    public boolean refreshDeviceCache(BluetoothGatt gatt) {
        /*
         * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
		 */
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                final boolean success = (Boolean) refresh.invoke(gatt);
                Log.i("refreshDeviceCache", "Refreshing result: " + success);
                return success;
            }
        } catch (Exception e) {
            Log.e("refreshDeviceCache", "An exception occured while refreshing device", e);
        }
        return false;
    }

    private boolean enableNotification(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt) {
        boolean success = gatt.setCharacteristicNotification(characteristic, true);
        if(!success) {
            Log.d("BLEService", "设置通知失败");
            return false;
        }
        if (characteristic.getDescriptors().size()>0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(ParamUtils.UUID_DESCRIPTOR);
            if(descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                Log.d("BLEService", "设置通知成功");
                return true;
            }
            else {
                Log.d("BLEService", "characteristic没有相关的descriptor，设置通知失败");
                return false;
            }
        }
        else {
            Log.d("BLEService", "characteristic没有相关的descriptor");
            return false;
        }
    }

    public void writeCharacteristic(UUID uuid, byte[] value) {
        if (gatt!=null) {
            BluetoothGattCharacteristic characteristic = gatt.getService(ParamUtils.UUID_SERVICE_MILI).getCharacteristic(uuid);
            if (characteristic==null) {
                Log.d("BluetoothIO", "gatt.getCharacteristic " + uuid + " is not exsit");
                return;
            }
            characteristic.setValue(value);
            if (!gatt.writeCharacteristic(characteristic)) {
                Log.d("BluetoothIO", "gatt.writeCharacteristic() return false");
            }
            else {
                Log.d("BluetoothIO", "gatt.writeCharacteristic() return true");
            }
        }
    }

    public void readCharacteristic(UUID serviceUUID, UUID CharacUUID) {
        if (gatt!=null) {
            BluetoothGattCharacteristic characteristic = gatt.getService(serviceUUID).getCharacteristic(CharacUUID);
            if (characteristic==null) {
                Log.d("BluetoothIO", "gatt.getCharacteristic uuid is not exsit");
                return;
            }
            if (!gatt.readCharacteristic(characteristic)) {
                Log.d("BluetoothIO", "gatt.readCharacteristic() return false");
            }
        }
    }
}
