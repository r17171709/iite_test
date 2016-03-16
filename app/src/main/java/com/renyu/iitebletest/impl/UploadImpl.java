package com.renyu.iitebletest.impl;

import com.renyu.iitebletest.model.ResponseModel;

import java.util.Map;

import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

/**
 * Created by renyu on 16/3/16.
 */
public interface UploadImpl {

    @FormUrlEncoded
    @POST("product_tool/tool/stagequan")
    Observable<ResponseModel> upload(@FieldMap Map<String, String> params);
}
