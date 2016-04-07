package com.renyu.iitebletest.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.renyu.iitebletest.common.ParamUtils;
import com.renyu.iitebletest.model.BLECommandModel;
import com.renyu.iitebletest.model.BLEConnectModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by renyu on 16/2/14.
 */
public class QueueUtils {

    static QueueUtils instance;

    private QueueUtils() {
        executorNOService= Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        executorReadService=Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        rotateThread=new Thread() {
            @Override
            public void run() {
                super.run();
                Looper.prepare();

                rotateThreadHandler=new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (msg.what==0x110) {
                            executorNOService.execute((Runnable) msg.obj);
                        }
                        else if (msg.what==0x112) {
                            executorReadService.execute((Runnable) msg.obj);
                        }
                        else if (msg.what==0x111) {
                            taskSemaphore.release();
                        }
                    }
                };

                Looper.loop();
            }
        };
        rotateThread.start();
        taskSemaphore=new Semaphore(1);
        receiverCommandMaps=new HashMap<>();
    }

    public static QueueUtils getInstance() {
        if (instance==null) {
            synchronized (QueueUtils.class) {
                if (instance==null) {
                    instance=new QueueUtils();
                }
            }
        }
        return instance;
    }

    //通知线程池
    ExecutorService executorNOService;
    //读取线程池
    ExecutorService executorReadService;
    //默认加载数量
    static final int DEFAULT_THREAD_COUNT=1;
    //后台轮训线程
    Thread rotateThread;
    Handler rotateThreadHandler;
    Semaphore taskSemaphore;
    //当前执行发送的Subscription
    Subscription currentSendSubscription;
    //收到的数据列
    HashMap<String, LinkedList<byte[]>> receiverCommandMaps;

    public void addTask(int command, HashMap<String, String> params, Context context) {
        String values=BLEUtils.getBLECommand(command, params);
        BLEDataUtils ble_send = new BLEDataUtils(values, command);
        byte[][] bytes_send = ble_send.getDivided_data();
        for (byte[] bytes : bytes_send) {
            addTask(bytes, context);
        }
    }

    private void addTask(final byte[] command, final Context context) {
        addTask(new Runnable() {
            @Override
            public void run() {
                if (BLEService.blestate== BLEConnectModel.BLESTATE.STATE_CONNECTED) {
                    try {
                        taskSemaphore.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Observable.just(command).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] bytes) {
                            //指令发送
                            Log.d("QueueUtils", "发送指令");

                            Intent intent=new Intent(context, BLEService.class);
                            Bundle bundle=new Bundle();
                            bundle.putInt("command", ParamUtils.BLE_COMMAND_SEND);
                            bundle.putByteArray("value", bytes);
                            intent.putExtras(bundle);
                            context.startService(intent);
                        }
                    });
                    currentSendSubscription=Observable.just(command).delay(5, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() {
                        @Override
                        public void call(byte[] bytes) {
                            Log.d("QueueUtils", "command:" + new String(bytes) +"执行失败");
                            release();
                        }
                    });
                }

            }
        });
    }

    private void addTask(Runnable runnable) {
        Message m=new Message();
        m.obj=runnable;
        m.what=0x110;
        rotateThreadHandler.sendMessage(m);
    }

    public void addReadTask(final UUID serviceUUID, final UUID characUUID, final Context context) {
        if (BLEService.blestate== BLEConnectModel.BLESTATE.STATE_CONNECTED) {
            addReadTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        taskSemaphore.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Observable.just("").observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            //发送读取指令
                            Log.d("QueueUtils", "发送读取指令");

                            Intent intent=new Intent(context, BLEService.class);
                            Bundle bundle=new Bundle();
                            bundle.putInt("command", ParamUtils.BLE_COMMAND_READ);
                            bundle.putSerializable("serviceUUID", serviceUUID);
                            bundle.putSerializable("characUUID", characUUID);
                            intent.putExtras(bundle);
                            context.startService(intent);
                        }
                    });
                    currentSendSubscription=Observable.just("").delay(5, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            Log.d("QueueUtils", "command:" + characUUID.toString() +"执行失败");
                            release();
                        }
                    });
                }
            });
        }
    }

    private void addReadTask(Runnable runnable) {
        Message m=new Message();
        m.obj=runnable;
        m.what=0x112;
        rotateThreadHandler.sendMessage(m);
    }

    public void release() {
        if (currentSendSubscription!=null) {
            currentSendSubscription.unsubscribe();
            currentSendSubscription=null;
            rotateThreadHandler.sendEmptyMessage(0x111);
        }
    }

    public synchronized void putCommand(byte[] bytes) {
        int command=BLEUtils.convert2To10(BLEUtils.sign2nosign(bytes[2]));
        //最后一条数据
        if (bytes[0]==bytes[1]) {
            //判断指令是否在集合中，如果不在则忽略本条指令
            if (receiverCommandMaps.containsKey(""+command)) {
                LinkedList<byte[]> list=receiverCommandMaps.get(""+command);
                list.add(bytes);
                //如果集合中数量跟指令条数一直，则进一步判断，否则则忽略本条指令
                if (list.size()==bytes[1]) {
                    byte[][] bytes1=new byte[list.size()][];
                    for (int i=0;i<bytes1.length;i++) {
                        bytes1[i]=list.get(i);
                    }
                    BLEDataUtils utils=new BLEDataUtils(bytes1);
                    String result=utils.getOrigin_str();
                    Log.d("QueueUtils", result);
                    //符合json数据格式，则认为是有效指令，并将其转发
                    try {
                        new JSONObject(result);

                        BLECommandModel model=new BLECommandModel();
                        model.setCommand(command);
                        model.setValue(result);
                        EventBus.getDefault().post(model);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        //清除指令
                        receiverCommandMaps.remove(""+command);
                    }
                }
            }
        }
        else {
            LinkedList<byte[]> list;
            if (receiverCommandMaps.containsKey(""+command)) {
                list=receiverCommandMaps.get(""+command);
            }
            else {
                list=new LinkedList<>();
            }
            list.add(bytes);
            receiverCommandMaps.put(""+command, list);
        }
    }

    public void putReadCommand(byte[] bytes, UUID uuid) {
        BLECommandModel model=new BLECommandModel();
        if (uuid.toString().equals(ParamUtils.UUID_SERVICE_BATTERY_READ.toString())) {
            model.setCommand(ParamUtils.BLE_COMMAND_BATTERY);
            model.setValue(""+bytes[0]);
        }
        else if (uuid.toString().equals(ParamUtils.UUID_SERVICE_DEVICEINFO_NAME.toString())) {
            model.setCommand(ParamUtils.BLE_COMMAND_INFONAME);
            model.setValue(new String(bytes));
        }
        else if (uuid.toString().equals(ParamUtils.UUID_SERVICE_DEVICEINFO_ID.toString())) {
            model.setCommand(ParamUtils.BLE_COMMAND_INFOID);
            model.setValue(new String(bytes));
        }
        else if (uuid.toString().equals(ParamUtils.UUID_SERVICE_DEVICEINFO_VERSION.toString())) {
            model.setCommand(ParamUtils.BLE_COMMAND_INFOVERSION);
            model.setValue(new String(bytes));
        }
        else if (uuid.toString().equals(ParamUtils.UUID_SERVICE_DEVICEINFO_CPUID.toString())) {
            model.setCommand(ParamUtils.BLE_COMMAND_CPUID);
            model.setValue(new String(bytes));
        }
        EventBus.getDefault().post(model);
    }

    public void putOtaCommand() {
        BLECommandModel model=new BLECommandModel();
        model.setCommand(ParamUtils.BLE_COMMAND_UPDATE);
        EventBus.getDefault().post(model);
    }
}
