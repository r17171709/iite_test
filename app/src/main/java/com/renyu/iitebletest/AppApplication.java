package com.renyu.iitebletest;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Clevo on 2016/7/28.
 */
public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        copyAssetsFile("20160725V1.2.1FOR1.6.cyacd", new File(Environment.getExternalStorageDirectory().getPath()+File.separator+"20160725V1.2.1FOR1.6.cyacd").getPath(), this);
        copyAssetsFile("20160725V1.2.1FOR1.7.cyacd", new File(Environment.getExternalStorageDirectory().getPath()+File.separator+"20160725V1.2.1FOR1.7.cyacd").getPath(), this);
    }

    /**
     * 通过assets复制文件
     * @param oldName
     * @param newPath
     * @param context
     */
    private static void copyAssetsFile(String oldName, String newPath, Context context) {
        File file=new File(newPath);
        if (new File(newPath).exists()) {
            file.delete();
        }
        AssetManager manager=context.getAssets();
        try {
            int byteread=0;
            InputStream inStream=manager.open(oldName);
            FileOutputStream fs=new FileOutputStream(newPath);
            byte[] buffer=new byte[1444];
            while ((byteread = inStream.read(buffer))!=-1) {
                fs.write(buffer, 0, byteread);
            }
            inStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
