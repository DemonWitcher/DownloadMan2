package com.witcher.downloadman2lib;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class DownloadService extends Service{

    private DownloadManager mDownloadManager;

    private IDownloadService.Stub binder = new IDownloadService.Stub() {
        @Override
        public void registerCallback(IDownloadCallback callback) throws RemoteException {
            if(mDownloadManager!=null){
                mDownloadManager.registerCallback(callback);
            }
        }

        @Override
        public void unregisterCallback(IDownloadCallback callback) throws RemoteException {
            if(mDownloadManager!=null){
                mDownloadManager.unregisterCallback(callback);
            }
        }

        @Override
        public void start(String url, String path) throws RemoteException {
            L.i("start tName:"+Thread.currentThread().getName());
            if(mDownloadManager!=null){
                mDownloadManager.start(url,path);
            }
        }

        @Override
        public void pause(int tid) throws RemoteException {
            if(mDownloadManager!=null){
                mDownloadManager.pause(tid);
            }
        }

        @Override
        public void delete(int tid) throws RemoteException {
            if(mDownloadManager!=null){
                mDownloadManager.delete(tid);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if(mDownloadManager == null){
            mDownloadManager = new DownloadManager();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mDownloadManager!=null){
            mDownloadManager.onDestroy();
        }
    }
}
