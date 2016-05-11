package com.renyu.iitebletest.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.OTAFlashRowModel;
import com.cypress.cysmart.OTAFirmwareUpdate.BootLoaderCommands;
import com.cypress.cysmart.OTAFirmwareUpdate.BootLoaderUtils;
import com.cypress.cysmart.OTAFirmwareUpdate.CustomFileReader;
import com.cypress.cysmart.OTAFirmwareUpdate.FileReadStatusUpdater;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareWrite;
import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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
public class BLEService extends Service implements FileReadStatusUpdater {

    MyLeScanCallback callback;
    //扫描到的所有设备
    HashMap<String, com.renyu.iitebletest.bluetooth.BluetoothDevice> deviceHashMap;
    //BLE通用属性配置文件
    static BluetoothGatt gatt;
    //当前设备状态
    public static BLEConnectModel.BLESTATE blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;
    //BLE整体配置
    Subscription bleSubscription;

    //断开连接是否需要发送广播
    boolean needBroadcast=true;

    //是否进入bootloader模式
    boolean isBootloader=false;

    //***********************************************************

    private static String mSiliconID;
    private static String mSiliconRev;
    private static String mCheckSumType;
    private boolean HANDLER_FLAG = true;
    private int mTotalLines = 0;
    private ArrayList<OTAFlashRowModel> mFlashRowList;
    //本次连接设备的地址
    private String mBluetoothDeviceAddress;
    //ota特征值
    private BluetoothGattCharacteristic mOTACharacteristic;
    //自定义ota写对象
    private OTAFirmwareWrite otaFirmwareWrite;
    private static boolean m_otaExitBootloaderCmdInProgress = false;
    private int mProgressBarPosition = 0;

    BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                final String sharedPrefStatus = Utils.getStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE);
                final String action = intent.getAction();
                Bundle extras = intent.getExtras();
                if (BootLoaderUtils.ACTION_OTA_STATUS.equals(action)) {
                    if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.ENTER_BOOTLOADER)) {
                        String siliconIDReceived, siliconRevReceived;
                        if (extras.containsKey(Constants.EXTRA_SILICON_ID) && extras.containsKey(Constants.EXTRA_SILICON_REV)) {
                            siliconIDReceived = extras.getString(Constants.EXTRA_SILICON_ID);
                            siliconRevReceived = extras.getString(Constants.EXTRA_SILICON_REV);
                            if (siliconIDReceived.equalsIgnoreCase(mSiliconID) && siliconRevReceived.equalsIgnoreCase(mSiliconRev)) {
                                /**
                                 * SiliconID and SiliconRev Verified
                                 * Sending Next coommand
                                 */
                                //Getting the arrayID
                                OTAFlashRowModel modelData = mFlashRowList.get(0);
                                byte[] data = new byte[1];
                                data[0] = (byte) modelData.mArrayId;
                                int dataLength = 1;
                                /**
                                 * Writing the next command
                                 * Changing the shared preference value
                                 */
                                otaFirmwareWrite.OTAGetFlashSizeCmd(data, mCheckSumType, dataLength);
                                Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.GET_FLASH_SIZE);
                                Log.d("BLEService", "执行获取flash大小的命令");
                            }
                        }

                    }
                    else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.GET_FLASH_SIZE)) {
                        /**
                         * verifying the rows to be programmed within the bootloadable area of flash
                         * not done for time being
                         */
                        int PROGRAM_ROW_NO = Utils.getIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_NO);
                        writeProgrammableData(PROGRAM_ROW_NO);
                    }
                    else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.SEND_DATA)) {
                        /**
                         * verifying the status and sending the next command
                         * Changing the shared preference value
                         */
                        if (extras.containsKey(Constants.EXTRA_SEND_DATA_ROW_STATUS)) {
                            String statusReceived = extras.getString(Constants.EXTRA_SEND_DATA_ROW_STATUS);
                            if (statusReceived.equalsIgnoreCase("00")) {
                                //Succes status received.Send programmable data
                                int PROGRAM_ROW_NO = Utils.getIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_NO);
                                writeProgrammableData(PROGRAM_ROW_NO);
                            }
                        }
                    }
                    else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.PROGRAM_ROW)) {
                        String statusReceived;
                        if (extras.containsKey(Constants.EXTRA_PROGRAM_ROW_STATUS)) {
                            statusReceived = extras.getString(Constants.EXTRA_PROGRAM_ROW_STATUS);
                            if (statusReceived.equalsIgnoreCase("00")) {
                                /**
                                 * Program Row Status Verified
                                 * Sending Next coommand
                                 */
                                //Getting the arrayI
                                int PROGRAM_ROW = Utils.getIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_NO);
                                OTAFlashRowModel modelData = mFlashRowList.get(PROGRAM_ROW);
                                long rowMSB = Long.parseLong(modelData.mRowNo.substring(0, 2), 16);
                                long rowLSB = Long.parseLong(modelData.mRowNo.substring(2, 4), 16);
                                /**
                                 * Writing the next command
                                 * Changing the shared preference value
                                 */
                                otaFirmwareWrite.OTAVerifyRowCmd(rowMSB, rowLSB, modelData, mCheckSumType);
                                Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.VERIFY_ROW);
                                Log.d("BLEService", "执行获取flash大小的命令");
                            }
                        }
                    }
                    else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.VERIFY_ROW)) {
                        String statusReceived, checksumReceived;
                        if (extras.containsKey(Constants.EXTRA_VERIFY_ROW_STATUS) && extras.containsKey(Constants.EXTRA_VERIFY_ROW_CHECKSUM)) {
                            statusReceived = extras.getString(Constants.EXTRA_VERIFY_ROW_STATUS);
                            checksumReceived = extras.getString(Constants.EXTRA_VERIFY_ROW_CHECKSUM);
                            if (statusReceived.equalsIgnoreCase("00")) {
                                /**
                                 * Program Row Status Verified
                                 * Sending Next coommand
                                 */
                                int PROGRAM_ROW_NO = Utils.getIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_NO);
                                //Getting the arrayID
                                OTAFlashRowModel modelData = mFlashRowList.get(PROGRAM_ROW_NO);
                                long rowMSB = Long.parseLong(modelData.mRowNo.substring(0, 2), 16);
                                long rowLSB = Long.parseLong(modelData.mRowNo.substring(2, 4), 16);

                                byte[] checkSumVerify = new byte[6];
                                checkSumVerify[0] = (byte) modelData.mRowCheckSum;
                                checkSumVerify[1] = (byte) modelData.mArrayId;
                                checkSumVerify[2] = (byte) rowMSB;
                                checkSumVerify[3] = (byte) rowLSB;
                                checkSumVerify[4] = (byte) (modelData.mDataLength);
                                checkSumVerify[5] = (byte) ((modelData.mDataLength) >> 8);
                                String fileCheckSumCalculated = Integer.toHexString(BootLoaderUtils.calculateCheckSumVerifyRow(6, checkSumVerify));
                                int fileCheckSumCalculatedLength = fileCheckSumCalculated.length();
                                if(fileCheckSumCalculatedLength == 1){
                                    fileCheckSumCalculated = "0" + fileCheckSumCalculated;
                                    fileCheckSumCalculatedLength ++;
                                }
                                String fileCheckSumByte = fileCheckSumCalculated.substring((fileCheckSumCalculatedLength - 2), fileCheckSumCalculatedLength);
                                if (fileCheckSumByte.equalsIgnoreCase(checksumReceived)) {
                                    PROGRAM_ROW_NO = PROGRAM_ROW_NO + 1;
                                    //Shows ProgressBar status
                                    showProgress(mProgressBarPosition, PROGRAM_ROW_NO, mFlashRowList.size());
                                    if (PROGRAM_ROW_NO < mFlashRowList.size()) {
                                        Utils.setIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_NO, PROGRAM_ROW_NO);
                                        Utils.setIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_START_POS, 0);
                                        writeProgrammableData(PROGRAM_ROW_NO);
                                    }
                                    if (PROGRAM_ROW_NO == mFlashRowList.size()) {
                                        Utils.setIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_NO, 0);
                                        Utils.setIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_START_POS, 0);
                                        /**
                                         * Writing the next command
                                         * Changing the shared preference value
                                         */
                                        Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.VERIFY_CHECK_SUM);
                                        otaFirmwareWrite.OTAVerifyCheckSumCmd(mCheckSumType);
                                        Log.d("BLEService", "执行验证检查操作");
                                    }
                                }
                            }
                        }

                    }
                    else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.VERIFY_CHECK_SUM)) {
                        String statusReceived;
                        if (extras.containsKey(Constants.EXTRA_VERIFY_CHECKSUM_STATUS)) {
                            statusReceived = extras.getString(Constants.EXTRA_VERIFY_CHECKSUM_STATUS);
                            if (statusReceived.equalsIgnoreCase("01")) {
                                /**
                                 * Verify Status Verified
                                 * Sending Next coommand
                                 */
                                //Getting the arrayID
                                otaFirmwareWrite.OTAExitBootloaderCmd(mCheckSumType);
                                Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.EXIT_BOOTLOADER);
                                Log.d("BLEService", "bootloader结束");
                            }
                        }

                    }
                    else if(sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.EXIT_BOOTLOADER)){
                        final BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mBluetoothDeviceAddress);
                        ParamUtils.mFileupgradeStarted = false;
                        unpairDevice(device);
                        saveDeviceAddress();
                        Log.d("BLEService", "ota固件升级成功");
                        if (secondFileUpdatedNeeded()) {
                            Log.d("BLEService", "堆栈升级成功完成。应用程序升级悬而未决");
                        }
                        else {
                            Log.d("BLEService", "ota固件升级成功");
                        }
                        ParamUtils.mFileupgradeStarted = false;
                        closeAllBLEConnect();
                        unpairDevice(device);
                    }
                    if (extras.containsKey(Constants.EXTRA_ERROR_OTA)) {
                        String errorMessage = extras.getString(Constants.EXTRA_ERROR_OTA);
                        Log.d("BLEService", errorMessage);
                    }
                }
                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    if (state == BluetoothDevice.BOND_BONDING) {
                        // Bonding...
                        Log.i("BLEService", "Bonding is in process....");
                    }
                    else if (state == BluetoothDevice.BOND_BONDED) {
                        Log.d("BLEService", "Paired");
                    }
                    else if (state == BluetoothDevice.BOND_NONE) {
                        Log.d("BLEService", "Unpaired");
                    }
                }
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;
        deviceHashMap=new HashMap<>();

        IntentFilter filter=new IntentFilter();
        filter.addAction(BootLoaderUtils.ACTION_OTA_STATUS);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);

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
                SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
            else if (command==ParamUtils.BLE_COMMAND_TEST) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_TEST, null, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_SETUSERID) {
                HashMap<String, String> params=new HashMap<>();
                params.put("userid", "test");
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
                params.put("model", intent.getExtras().getString("value"));
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_SETATTITUDEMODE, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_GAMESTART) {
                HashMap<String, String> params=new HashMap<>();
                params.put("duration", intent.getExtras().getString("value"));
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_GAMESTART, params, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_CLEANTEETH) {
                QueueUtils.getInstance().addTask(ParamUtils.BLE_COMMAND_CLEANTEETH, null, this);
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
                isBootloader = true;
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
            else if (command==ParamUtils.BLE_COMMAND_CPUID) {
                QueueUtils.getInstance().addReadTask(ParamUtils.UUID_SERVICE_DEVICEINFO, ParamUtils.UUID_SERVICE_DEVICEINFO_CPUID, this);
            }
            else if (command==ParamUtils.BLE_COMMAND_LOGOUT) {
                needBroadcast=false;
                if (callback!=null) {
                    callback.cancelScan();
                }
                closeAllBLEConnect();
            }
            else if (command==ParamUtils.BLE_COMMAND_CONNECT) {
                needBroadcast=false;
                if (callback!=null) {
                    callback.cancelScan();
                }
                closeAllBLEConnect();
                BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
                connectBLE(adapter.getRemoteDevice(intent.getExtras().getString("value")));
            }
            else if (command==ParamUtils.OTAEnterBootLoaderCmd) {
                prepareFileWriting(intent.getExtras().getString("value"));
                mProgressBarPosition = 1;
                initializeBondingIFnotBonded();
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
        if (adapter==null) {
            Log.d("BLEService", "取消扫描");

            if (blestate==BLEConnectModel.BLESTATE.STATE_SCAN) {
                blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                BLEConnectModel model=new BLEConnectModel();
                model.setBlestate(BLEConnectModel.BLESTATE.STATE_CANCELSCAN);
                EventBus.getDefault().post(model);
            }
            return;
        }
        callback=new MyLeScanCallback(10, adapter) {
            @Override
            public void scanCancelCallBack() {
                Log.d("BLEService", "取消扫描");

                blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                BLEConnectModel model=new BLEConnectModel();
                model.setBlestate(BLEConnectModel.BLESTATE.STATE_CANCELSCAN);
                EventBus.getDefault().post(model);

                callback=null;
            }

            @Override
            public void scanEndCallBack() {
                Log.d("BLEService", "扫描结束");

                blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;

                BLEConnectModel model=new BLEConnectModel();
                model.setBlestate(BLEConnectModel.BLESTATE.STATE_NOSCAN);
                EventBus.getDefault().post(model);

                callback=null;
            }

            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("BLEService", device.getAddress()+" "+device.getName()+" "+device.getUuids()+" "+rssi);
                com.renyu.iitebletest.bluetooth.BluetoothDevice device1=new com.renyu.iitebletest.bluetooth.BluetoothDevice();
                device1.setDevice(device);
                device1.setRssi(rssi);
                if (device.getName()!=null && device.getName().startsWith("iite")) {
                    if (name==null || name.equals("")) {
                        if (!deviceHashMap.containsKey(device.getName())) {
                            deviceHashMap.put(device.getName(), device1);
                            EventBus.getDefault().post(device1);
                        }
                    }
                    else {
                        if (device.getName().equals(name) || device.getName().substring(device.getName().indexOf("-")+1).equals(name)) {
                            if (!deviceHashMap.containsKey(device.getName())) {
                                deviceHashMap.put(device.getName(), device1);
                                EventBus.getDefault().post(device1);
                            }
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
        mBluetoothDeviceAddress = device.getAddress();
        closeAllBLEConnect();
        //超过30s还不能连接上，直接认为连接失败
        bleSubscription= Observable.timer(30, TimeUnit.SECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                closeAllBLEConnect();

                disConnect();
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
                BLEService.this.gatt=gatt;
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (isBootloader && status==0) {
                    isBootloader=false;
                    QueueUtils.getInstance().putOtaCommand();
                }
                QueueUtils.getInstance().release();
                BLEService.this.gatt=gatt;

                boolean isExitBootloaderCmd = false;
                synchronized (BLEService.class) {
                    isExitBootloaderCmd = m_otaExitBootloaderCmdInProgress;
                    if(m_otaExitBootloaderCmdInProgress)
                        m_otaExitBootloaderCmdInProgress = false;
                }
                if(isExitBootloaderCmd)
                    onOtaExitBootloaderComplete(status);
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
                BLEService.this.gatt=gatt;

                //ota的指令
                if (ParamUtils.UUID_SERVICE_OTA.toString().equals(characteristic.getUuid().toString())) {
                    Intent intentOTA = new Intent(ParamUtils.ACTION_OTA_DATA_AVAILABLE);
                    Bundle mBundle = new Bundle();
                    mBundle.putByteArray(Constants.EXTRA_BYTE_VALUE, characteristic.getValue());
                    mBundle.putString(Constants.EXTRA_BYTE_UUID_VALUE, characteristic.getUuid().toString());
                    intentOTA.putExtras(mBundle);
                    sendBroadcast(intentOTA);
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                BLEService.this.gatt=gatt;
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Log.d("BLEService", "BLE设备连接成功");
                        Log.d("BLEService", "BLE设备正在配置服务中");
                        gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.d("BLEService", "BLE设备连接断开");

                        bleSubscription.unsubscribe();

                        gatt.close();

                        disConnect();

                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                BLEService.this.gatt=gatt;
                if (status==BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLEService", "BLE设备配置服务成功");

                    if (gatt.getService(ParamUtils.UUID_SERVICE_MILI)==null) {
                        Log.d("BLEService", "虽然BLE设备配置服务成功，但是服务已经关闭");

                        closeAllBLEConnect();
                    }
                    else {
                        if (checkIsOTA()) {
                            //开启通知服务
                            BluetoothGattCharacteristic characteristic = gatt.getService(ParamUtils.UUID_SERVICE_OTASERVICE).getCharacteristic(ParamUtils.UUID_SERVICE_OTA);
                            if (enableNotification(characteristic, gatt, ParamUtils.UUID_DESCRIPTOR_OTA)) {
                                mOTACharacteristic=characteristic;

                                BLEConnectModel model=new BLEConnectModel();
                                model.setBlestate(BLEConnectModel.BLESTATE.STATE_OTA);
                                EventBus.getDefault().post(model);

                                blestate= BLEConnectModel.BLESTATE.STATE_OTA;

                                bleSubscription.unsubscribe();

                                Intent intent=new Intent();
                                intent.setAction("BLE_RETRY_DIS");
                                sendBroadcast(intent);
                            }
                            else {
                                closeAllBLEConnect();
                            }
                        }
                        else {
                            //开启通知服务
                            BluetoothGattCharacteristic characteristic = gatt.getService(ParamUtils.UUID_SERVICE_MILI).getCharacteristic(ParamUtils.UUID_SERVICE_READ);
                            if (enableNotification(characteristic, gatt, ParamUtils.UUID_DESCRIPTOR)) {
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
                }
                else {
                    Log.d("BLEService", "BLE设备配置服务失败");

                    closeAllBLEConnect();
                }
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);

                BLEService.this.gatt=gatt;

                BLECommandModel model=new BLECommandModel();
                model.setCommand(ParamUtils.BLE_COMMAND_RSSI);
                model.setValue(""+rssi);
                EventBus.getDefault().post(model);
            }
        });
    }

    private void disConnect() {
        BLEConnectModel model=new BLEConnectModel();
        model.setBlestate(BLEConnectModel.BLESTATE.STATE_DISCONNECTED);
        EventBus.getDefault().post(model);

        //在不需要重试的时候，主动断开重试
        if (needBroadcast) {
            Intent intent=new Intent();
            intent.setAction("BLE_RETRY");
            sendBroadcast(intent);
        }
        else {
            Intent intent=new Intent();
            intent.setAction("BLE_RETRY_DIS");
            sendBroadcast(intent);
        }
        needBroadcast=true;

        blestate= BLEConnectModel.BLESTATE.STATE_NOSCAN;
    }

    /**
     * 关闭所有gatt连接
     */
    private void closeAllBLEConnect() {
        if (gatt!=null) {
            gatt.disconnect();
            refreshDeviceCache(gatt);
            gatt=null;
        }
        else {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
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

    private boolean enableNotification(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt, UUID uuid) {
        boolean success = gatt.setCharacteristicNotification(characteristic, true);
        if(!success) {
            Log.d("BLEService", "设置通知失败");
            return false;
        }
        if (characteristic.getDescriptors().size()>0) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
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

    private void prepareFileWriting(String mCurrentFilePath) {
        Utils.setIntSharedPreference(this, Constants.PREF_PROGRAM_ROW_NO, 0);
        Utils.setIntSharedPreference(this, Constants.PREF_PROGRAM_ROW_START_POS, 0);
        if (mOTACharacteristic != null) {
            otaFirmwareWrite = new OTAFirmwareWrite(mOTACharacteristic);
        }
        final CustomFileReader customFileReader;
        customFileReader = new CustomFileReader(mCurrentFilePath);
        customFileReader.setFileReadStatusUpdater(this);
        String[] headerData = customFileReader.analyseFileHeader();
        mSiliconID = headerData[0];
        mSiliconRev = headerData[1];
        mCheckSumType = headerData[2];
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (HANDLER_FLAG) {
                    mTotalLines = customFileReader.getTotalLines();
                    mFlashRowList = customFileReader.readDataLines();
                }
            }
        }, 1000);
    }

    private void initializeBondingIFnotBonded() {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mBluetoothDeviceAddress);
        if (!getBondedState()) {
            pairDevice(device);

        }
    }

    private boolean getBondedState() {
        Boolean bonded;
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mBluetoothDeviceAddress);
        bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        return bonded;
    }

    //For Pairing
    private void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value, boolean isExitBootloaderCmd) {
        synchronized (BLEService.class) {
            writeOTABootLoaderCommand(characteristic, value);
            if (isExitBootloaderCmd) {
                m_otaExitBootloaderCmdInProgress = true;
            }
        }
    }

    public synchronized static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value) {
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), characteristic.getService().getUuid().toString());
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristic.getUuid().toString());
        String characteristicValue = Utils.ByteArraytoHex(value);
        if ( gatt == null) {
            return;
        }
        else {
            byte[] valueByte = value;
            characteristic.setValue(valueByte);
            int counter = 20;
            boolean status;
            do {
                status = gatt.writeCharacteristic(characteristic);
                if(!status) {
                    Log.v("CYSMART","writeCharacteristic() status: False");
                    try {
                        Thread.sleep(100,0);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } while (!status && (counter-- > 0));


            if(status) {
                String dataLog =
                        "[" + serviceName + "|" + characteristicName + "] " +
                                "Write request sent with value, " +

                                "[ " + characteristicValue + " ]";
                Log.i("CYSMART", dataLog);
                Log.v("CYSMART", dataLog);
            }
            else {
                Log.v("CYSMART", "writeOTABootLoaderCommand failed!");
            }
        }
    }

    private void writeProgrammableData(int rowPosition) {
        int startPosition = Utils.getIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_START_POS);
        Log.e("BLEService", "Row: " + rowPosition + "Start Pos: " + startPosition);
        OTAFlashRowModel modelData = mFlashRowList.get(rowPosition);
        int verifyDataLength = modelData.mDataLength - startPosition;
        if (checkProgramRowCommandToSend(verifyDataLength)) {
            long rowMSB = Long.parseLong(modelData.mRowNo.substring(0, 2), 16);
            long rowLSB = Long.parseLong(modelData.mRowNo.substring(2, 4), 16);
            int dataLength = modelData.mDataLength - startPosition;
            byte[] dataToSend = new byte[dataLength];
            for (int pos = 0; pos < dataLength; pos++) {
                if (startPosition < modelData.mData.length) {
                    byte data = modelData.mData[startPosition];
                    dataToSend[pos] = data;
                    startPosition++;
                } else {
                    break;
                }
            }
            otaFirmwareWrite.OTAProgramRowCmd(rowMSB, rowLSB, modelData.mArrayId, dataToSend, mCheckSumType);
            Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE,  ""+BootLoaderCommands.PROGRAM_ROW);
            Utils.setIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_START_POS, 0);
            Log.d("BLEService", "固件升级中");
        }
        else {
            int dataLength = BootLoaderCommands.MAX_DATA_SIZE;
            byte[] dataToSend = new byte[dataLength];
            for (int pos = 0; pos < dataLength; pos++) {
                if (startPosition < modelData.mData.length) {
                    byte data = modelData.mData[startPosition];
                    dataToSend[pos] = data;
                    startPosition++;
                }
                else {
                    break;
                }
            }
            otaFirmwareWrite.OTAProgramRowSendDataCmd(dataToSend, mCheckSumType);
            Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.SEND_DATA);
            Utils.setIntSharedPreference(BLEService.this, Constants.PREF_PROGRAM_ROW_START_POS, startPosition);
            Log.d("BLEService", "固件升级中");
        }
    }

    /**
     * Method to show progress bar
     *
     * @param fileLineNos
     * @param totalLines
     */

    private void showProgress(int fileStatus, float fileLineNos, float totalLines) {
        if (fileStatus == 1) {
            Log.i("BLEService", (int) fileLineNos+"  "+(int) totalLines+"  "+(int) ((fileLineNos / totalLines) * 100) + "%");
        }
        if (fileStatus == 2) {
            Log.d("BLEService", "结束");
        }
    }

    private boolean checkProgramRowCommandToSend(int totalSize) {
        if (totalSize <= BootLoaderCommands.MAX_DATA_SIZE) {
            return true;
        } else {
            return false;
        }
    }

    private void onOtaExitBootloaderComplete(int status) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE, new byte[]{(byte)status});
        Intent intentOTA = new Intent(ParamUtils.ACTION_OTA_DATA_AVAILABLE);
        intentOTA.putExtras(bundle);
        sendBroadcast(intentOTA);
    }

    private boolean secondFileUpdatedNeeded() {
        String secondFilePath = Utils.getStringSharedPreference(BLEService.this, Constants.PREF_OTA_FILE_TWO_PATH);
        Log.e("BLEService", "secondFilePath-->" + secondFilePath);
        return mBluetoothDeviceAddress.equalsIgnoreCase(saveDeviceAddress())
                && (!secondFilePath.equalsIgnoreCase("Default")
                && (!secondFilePath.equalsIgnoreCase("")));
    }

    /**
     * Returns saved device adress
     *
     * @return
     */
    private String saveDeviceAddress() {
        Utils.setStringSharedPreference(BLEService.this, Constants.PREF_DEV_ADDRESS, mBluetoothDeviceAddress);
        return Utils.getStringSharedPreference(BLEService.this, Constants.PREF_DEV_ADDRESS);
    }

    /**
     * 检查是否进入OTA模式
     */
    public boolean checkIsOTA() {
        List<BluetoothGattService> gattServices=gatt.getServices();
        for (BluetoothGattService gattService : gattServices) {
            Log.d("BLEService", gattService.getUuid().toString());
            if (gattService.getUuid().toString().equals(ParamUtils.UUID_SERVICE_OTASERVICE.toString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onFileReadProgressUpdate(int fileLine) {
        if (this.mTotalLines <= 0 || fileLine > 0) {

        }
        if (this.mTotalLines == fileLine && mOTACharacteristic != null) {
            Log.d("BLEService", "文件读取成功");
            Utils.setStringSharedPreference(BLEService.this, Constants.PREF_BOOTLOADER_STATE, "56");
            ParamUtils.mFileupgradeStarted = true;
            otaFirmwareWrite.OTAEnterBootLoaderCmd(mCheckSumType);
            Log.d("BLEService", "执行进入bootloader方法");
        }

    }
}
