package com.renyu.iitebletest.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by renyu on 16/3/10.
 */
@DatabaseTable (tableName = "blecheck")
public class BLECheckModel {

    //设备唯一ID
    @DatabaseField (columnName = "bd_sn")
    String bd_sn;
    //操作工ID
    @DatabaseField (columnName = "qc_id_a")
    String qc_id_a;
    //检验日期， 格式为:yyyy-mm-dd hh:mm:ss 格式字符串
    @DatabaseField (columnName = "qc_date_a")
    String qc_date_a;
    //信号强度
    @DatabaseField (columnName = "bd_rssi")
    String bd_rssi;
    //按键测试
    @DatabaseField (columnName = "bd_swith")
    String bd_swith;
    //老化测试
    @DatabaseField (columnName = "bd_old")
    String bd_old;
    //检测结果
    @DatabaseField (columnName = "qc_result_a")
    String qc_result_a;
    //cpu版本号
    @DatabaseField (columnName = "cpuid")
    String cpuid;
    @DatabaseField (columnName = "iite_sn")
    String iite_sn;

    public String getBd_sn() {
        return bd_sn;
    }

    public void setBd_sn(String bd_sn) {
        this.bd_sn = bd_sn;
    }

    public String getQc_id_a() {
        return qc_id_a;
    }

    public void setQc_id_a(String qc_id_a) {
        this.qc_id_a = qc_id_a;
    }

    public String getQc_date_a() {
        return qc_date_a;
    }

    public void setQc_date_a(String qc_date_a) {
        this.qc_date_a = qc_date_a;
    }

    public String getBd_rssi() {
        return bd_rssi;
    }

    public void setBd_rssi(String bd_rssi) {
        this.bd_rssi = bd_rssi;
    }

    public String getBd_swith() {
        return bd_swith;
    }

    public void setBd_swith(String bd_swith) {
        this.bd_swith = bd_swith;
    }

    public String getBd_old() {
        return bd_old;
    }

    public void setBd_old(String bd_old) {
        this.bd_old = bd_old;
    }

    public String getQc_result_a() {
        return qc_result_a;
    }

    public void setQc_result_a(String qc_result_a) {
        this.qc_result_a = qc_result_a;
    }

    public String getCpuid() {
        return cpuid;
    }

    public void setCpuid(String cpuid) {
        this.cpuid = cpuid;
    }

    public String getIite_sn() {
        return iite_sn;
    }

    public void setIite_sn(String iite_sn) {
        this.iite_sn = iite_sn;
    }
}
