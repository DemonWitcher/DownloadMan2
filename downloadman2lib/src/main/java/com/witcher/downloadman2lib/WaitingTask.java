package com.witcher.downloadman2lib;

public class WaitingTask {

    public String url;
    public String path;
    public int tid;
    public DownloadListener downloadListener;

    public WaitingTask(String url, String path, int tid, DownloadListener downloadListener) {
        this.url = url;
        this.path = path;
        this.tid = tid;
        this.downloadListener = downloadListener;
    }
}
