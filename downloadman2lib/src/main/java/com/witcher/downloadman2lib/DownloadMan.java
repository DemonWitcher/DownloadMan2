package com.witcher.downloadman2lib;

import android.content.Context;
import android.text.TextUtils;

import com.witcher.downloadman2lib.bean.Task;

import java.io.File;

public class DownloadMan {

    private static Context context;
    private static ServiceBinder serviceBinder;

    /**
     * 使用application 别用activity之类的 会内存泄露
     * 现在没有懒加载 先放到application里执行这个 或者初始页之类的
     *
     * @param context application
     */
    public static void init(Context context) {
        DownloadMan.context = context;
        if (serviceBinder == null) {
            serviceBinder = new ServiceBinder();
        }
        if (!serviceBinder.isBind()) {
            serviceBinder.bindService();
        }
    }

    public static Context getContext() {
        return context;
    }

    public static int start(String url, String path, DownloadListener downloadListener) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(path) || downloadListener == null) {
            throw new IllegalArgumentException("url == null || path == null || downloadListener == null");
        }
        File file = new File(path);
        if (file.isDirectory()) {
            throw new IllegalArgumentException("path isDirectory need fileName");
        }
        L.i("url:" + url);
        L.i("path:" + path);
        File directory = file.getParentFile();
        if (!directory.exists()) {
            if (!directory.mkdirs() && !directory.exists()) {
                throw new RuntimeException("mkdirs fail");
            }
        }
        int tid = Util.generateId(url, path);
        if(serviceBinder!=null){
            serviceBinder.start(tid,url,path,downloadListener);
        }
        L.i("tid:" + tid);
        return tid;
    }

    public static void pause(int tid) {
        if(serviceBinder!=null){
            serviceBinder.pause(tid);
        }
    }

    public static void delete(int tid) {
        if(serviceBinder!=null){
            serviceBinder.delete(tid);
        }
    }

    public static Task sel(int tid) {
        return null;
    }
}
