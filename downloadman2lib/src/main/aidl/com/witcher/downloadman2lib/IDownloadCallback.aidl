// IDownloadCallback.aidl
package com.witcher.downloadman2lib;

interface IDownloadCallback {

    void onProgress(int tid,long current,long total);
    void onCompleted(int tid,long total);
    void onPause(int tid,long current,long total);
    void onDelete(int tid);
    void onStart(int tid);
    void onConnected(int tid);
    void onError(int tid,int code,String message);
}
