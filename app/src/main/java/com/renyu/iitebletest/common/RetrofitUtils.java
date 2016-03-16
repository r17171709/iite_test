package com.renyu.iitebletest.common;

import android.content.Context;
import android.util.Log;

import com.renyu.iitebletest.db.Dao;
import com.renyu.iitebletest.impl.UploadImpl;
import com.renyu.iitebletest.model.ResponseModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by renyu on 16/3/16.
 */
public class RetrofitUtils {

    private static RetrofitUtils retrofitUtils;

    Retrofit retrofit;

    private RetrofitUtils() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d("CodeHubServiceManager", message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder().addInterceptor(loggingInterceptor)
                .connectTimeout(20, TimeUnit.SECONDS).build();
        retrofit=new Retrofit.Builder().baseUrl("http://appapi.iite.cc:8080/").client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create()).addCallAdapterFactory(RxJavaCallAdapterFactory.create()).build();
    }

    public synchronized static RetrofitUtils getInstance() {
        if (retrofitUtils==null) {
            synchronized (RetrofitUtils.class) {
                if (retrofitUtils==null) {
                    retrofitUtils=new RetrofitUtils();
                }
            }
        }
        return retrofitUtils;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    /**
     * 上传数据
     * @param uniqueid
     * @param uid
     * @param battery
     * @param rssi
     * @param command
     * @param allResult
     */
    public void upload(final String uniqueid, String uid, String time, String battery, String rssi, boolean command, boolean allResult, final Context context) {
        UploadImpl upload=retrofit.create(UploadImpl.class);
        HashMap<String, String> params=new HashMap<>();
        params.put("bd_sn", uniqueid);
        params.put("qc_id_a", uid);
        SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        params.put("qc_date_a", dateFormat.format(Long.parseLong(time)));
        params.put("bd_old", battery);
        params.put("qc_result_a", allResult?"pass":"fail");
        params.put("bd_swith", command?"pass":"fail");
        params.put("bd_rssi", rssi);

        upload.upload(params).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<ResponseModel>() {
            @Override
            public void onCompleted() {
                ParamUtils.currentUploadCount++;
            }

            @Override
            public void onError(Throwable e) {
                ParamUtils.currentUploadCount++;
            }

            @Override
            public void onNext(ResponseModel responseModel) {
                Log.d("CheckActivity", responseModel.getData() + " " + responseModel.getResult());
                if (responseModel.getResult() == 1) {
                    Dao.getInstance(context).deleteData(uniqueid);
                }
            }
        });
    }
}
