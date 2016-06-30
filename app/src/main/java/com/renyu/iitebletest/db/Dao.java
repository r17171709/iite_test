package com.renyu.iitebletest.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.renyu.iitebletest.model.BLECheckModel;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by renyu on 16/3/10.
 */
public class Dao extends OrmLiteSqliteOpenHelper {

    private static Dao dao;

    private final static String databaseName="iitetest.db";
    private final static int databaseVersion=2;

    public synchronized static Dao getInstance(Context context) {
        if (dao==null) {
            synchronized (Dao.class) {
                if (dao==null) {
                    dao=new Dao(context);
                }
            }
        }
        return dao;
    }

    public Dao(Context context) {
        super(context, databaseName, null, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, BLECheckModel.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, ConnectionSource connectionSource, int i, int i1) {
        if (i==1 && i1==2) {

        }
    }

    @Override
    public void close() {
        super.close();
        dao=null;
    }

    /**
     * 获取全部数据
     * @return
     */
    public List<BLECheckModel> getAllData() {
        try {
            return getDao(BLECheckModel.class).queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 添加数据
     * @param model
     */
    public void addData(BLECheckModel model) {
        try {
            getDao(BLECheckModel.class).create(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除数据
     * @param uniqueid
     */
    public void deleteData(String uniqueid) {
        try {
            DeleteBuilder deleteBuilder=getDao(BLECheckModel.class).deleteBuilder();
            deleteBuilder.where().eq("bd_sn", uniqueid);
            deleteBuilder.delete();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
