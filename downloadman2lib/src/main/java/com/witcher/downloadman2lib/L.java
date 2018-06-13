package com.witcher.downloadman2lib;

import android.util.Log;

public class L {
    public static void i(String content){
        Log.i("witcher",getThreadName() + content);
    }
    public static void d(String content){
        Log.d("witcher",getThreadName() + content);
    }
    public static void w(String content){
        Log.w("witcher",getThreadName() + content);
    }
    public static void e(String content){
        Log.e("witcher",getThreadName() + content);
    }

    private static String getThreadName(){
        return "当前线程:" + Thread.currentThread().getName()+"  ";
    }
}
