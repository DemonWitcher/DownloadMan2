package com.witcher.downloadman2lib;

import com.witcher.downloadman2lib.IDownloadCallback;

interface IDownloadService {

    oneway void registerCallback(in IDownloadCallback callback);
    oneway void unregisterCallback(in IDownloadCallback callback);

    void start(String url,String path);
    void pause(int tid);
    void delete(int tid);

}
