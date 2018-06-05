package com.witcher.downloadman2;

import android.app.Application;

import com.witcher.downloadman2lib.DownloadMan;

public class MyApp extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        DownloadMan.init(this);
    }
}
