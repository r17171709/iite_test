package com.renyu.iitebletest.common;

import java.util.UUID;

/**
 * Created by renyu on 16/3/9.
 */
public class ParamUtils {

    public static String IDS="";

    //BLE相关
    //服务 UUID
    public static final UUID UUID_SERVICE_MILI=UUID.fromString("E53A96FD-BC51-4A3A-A397-4B759661B7CF");
    public static final UUID UUID_SERVICE_WRITE=UUID.fromString("0000cdd2-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_READ=UUID.fromString("0000cdd1-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DESCRIPTOR=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //电池服务 UUID
    public static final UUID UUID_SERVICE_BATTERY=UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_BATTERY_READ=UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    //设备信息服务 UUID
    public static final UUID UUID_SERVICE_DEVICEINFO=UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_DEVICEINFO_NAME=UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_DEVICEINFO_ID=UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_DEVICEINFO_VERSION=UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_DEVICEINFO_CPUID=UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    //ota UUID
    public static final UUID UUID_SERVICE_OTASERVICE=UUID.fromString("00060000-F8CE-11E4-ABF4-0002A5D5C51B");
    public static final UUID UUID_SERVICE_OTA=UUID.fromString("00060001-F8CE-11E4-ABF4-0002A5D5C51B");
    public static final UUID UUID_DESCRIPTOR_OTA=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //BLE触发扫描指定
    public static final int BLE_COMMAND_SCAN=100;
    //BLE触发写指令
    public static final int BLE_COMMAND_SEND=101;
    //BLE触发关闭设备指令
    public static final int BLE_COMMAND_CLOSE=102;
    //BLE触发读指令
    public static final int BLE_COMMAND_READ=103;
    //BLE保存最后address
    public static final int BLE_COMMAND_SAVE=104;
    //BLE获取BLE信息
    public static final int BLE_COMMAND_GETINFO=105;
    //BLE获取电池电量信息
    public static final int BLE_COMMAND_BATTERY=106;
    //BLE获取设备名称
    public static final int BLE_COMMAND_INFONAME=107;
    //BLE获取设备ID
    public static final int BLE_COMMAND_INFOID=108;
    //BLE获取设备版本
    public static final int BLE_COMMAND_INFOVERSION=109;
    //BLE芯片ID
    public static final int BLE_COMMAND_CPUID=113;
    //BLE解除绑定
    public static final int BLE_COMMAND_LOGOUT=110;
    //BLE读取rssi
    public static final int BLE_COMMAND_RSSI=111;
    //设定时间
    public static final int BLE_COMMAND_TIME=161;
    //读取时间
    public static final int BLE_COMMAND_GETTIME=162;
    //读取牙刷状态
    public static final int BLE_COMMAND_GETSTATE=163;
    //绑定牙刷
    public static final int BLE_COMMAND_BINDTEETH=241;
    //马达开关通知
    public static final int BLE_COMMAND_MOTORSWITCH=242;
    //设置userid
    public static final int BLE_COMMAND_SETUSERID=164;
    //读取userid
    public static final int BLE_COMMAND_GETUSERID=165;
    //恢复出厂设置
    public static final int BLE_COMMAND_FACTORY=166;
    //设置惯用手
    public static final int BLE_COMMAND_DOMINANT=168;
    //获取牙刷当前姿态
    public static final int BLE_COMMAND_ATTITUDE=177;
    //获取最近一次刷牙记录
    public static final int BLE_COMMAND_GETCURRENTTEETHINFO=178;
    //获取所有刷牙记录
    public static final int BLE_COMMAND_GETALLINFO=179;
    public static final int BLE_COMMAND_GETALLINFOTWO=243;
    //获取指定一条刷牙记录
    public static final int BLE_COMMAND_GETONEINFO=180;
    //获取当前姿态识别模式
    public static final int BLE_COMMAND_ATTITUDEMODE=181;
    //设定姿态识别模式
    public static final int BLE_COMMAND_SETATTITUDEMODE=182;
    //进入游戏模式
    public static final int BLE_COMMAND_GAMESTART=183;
    //游戏模式的暂停和恢复
    public static final int BLE_COMMAND_GAMECONTROL=184;
    //清空所有刷牙信息
    public static final int BLE_COMMAND_CLEANTEETH=185;
    //校准模式的进入和退出
    public static final int BLE_COMMAND_CALIBRATION=187;
    //设定led1
    public static final int BLE_COMMAND_SETLED1=193;
    //设定led2
    public static final int BLE_COMMAND_SETLED2=194;
    //设定motor
    public static final int BLE_COMMAND_SETMOTOR=195;
    //设备reset
    public static final int BLE_COMMAND_SETRESET=196;
    //获取uniqueid
    public static final int BLE_COMMAND_GETUID=197;
    //获取qrcode
    public static final int BLE_COMMAND_GETQRCODE=198;
    //进入固件升级模式
    public static final int BLE_COMMAND_UPDATE=199;
    //读取电池电压
    public static final int BLE_COMMAND_GETV=200;
    //BLE测试指令
    public static final int BLE_COMMAND_TEST=202;
    //连接指令
    public static final int BLE_COMMAND_CONNECT=203;
    //OTA
    public static final int OTAEnterBootLoaderCmd=300;
    public static boolean mFileupgradeStarted=false;

    public final static int RESULT_ENABLE_BT=1013;
    public final static int RESULT_QRCODE=1000;
    public final static int RESULT_SCANBLE=1001;
    public final static int RESULT_QRCODESN=1002;

    //当前已经执行数
    public static int currentUploadCount=0;
    //总上传数
    public static int totalUploadCount=0;


    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_DISCONNECTED_CAROUSEL =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_CAROUSEL";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.cysmart.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";
    public final static String ACTION_GATT_DISCONNECTED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_OTA";
    public final static String ACTION_GATT_CONNECT_OTA =
            "com.example.bluetooth.le.ACTION_GATT_CONNECT_OTA";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_OTA";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
}
