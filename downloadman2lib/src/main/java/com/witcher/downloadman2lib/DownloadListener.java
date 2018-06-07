package com.witcher.downloadman2lib;

public interface DownloadListener {

    void onProgress(int tid,long current,long total);
    void onCompleted(int tid,long total);
    void onPause(int tid,long current,long total);
    void onDelete(int tid);
    void onStart(int tid);
    void onConnected(int tid);
    void onError(int tid,Throwable throwable);

}
