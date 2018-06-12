package com.witcher.downloadman2lib;

import android.os.RemoteException;

import com.witcher.downloadman2lib.bean.Task;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadManager {

    public static final int RANGER_NUMBER = 3;

    private ThreadPoolExecutor executor;
    private DBManager dbManager;
    private Map<Integer, List<DownloadRunnable>> downloadMap;
    private Map<Integer,FirstConnection> connectionMap;
    private List<IDownloadCallback> callbackList;

    public DownloadManager() {
        downloadMap = new ConcurrentHashMap<>();
        connectionMap = new ConcurrentHashMap<>();
        executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                15, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        dbManager = new DBManager(DownloadMan.getContext());
        callbackList = new ArrayList<>();
    }

    public void registerCallback(IDownloadCallback callback) {
        callbackList.add(callback);
    }

    public void unregisterCallback(IDownloadCallback callback) {
        callbackList.remove(callback);
    }

    public void onDestroy() {
        executor.shutdown();
        dbManager.close();
    }

    public void start(String url, String path) {
        int tid = Util.generateId(url, path);
        FirstConnection firstConnection = new FirstConnection(url,path,tid,executor,dbManager,callbackList,downloadMap);
        connectionMap.put(tid,firstConnection);
        executor.execute(firstConnection);
        //AIDL接口线程 ->准备线程->下载线程
        //现在连接中的状态 无法暂停
        //完成了的任务得从map里remove出去
    }

    public void pause(int tid) {
        try {
            long current = 0;
            long total = 0;
            FirstConnection firstConnection = connectionMap.get(tid);
            if(firstConnection!=null){
                firstConnection.pause();//下载线程暂停了 给内存里的数据 准备线程暂停了 给数据库里的数据
                long[] totalAndCurrent = firstConnection.getTotalAndCurrent();
                total = totalAndCurrent[0];
                current = totalAndCurrent[1];
            }
            List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
            if (downloadRunnableList != null) {
                for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                    downloadRunnable.pause();
                    current = downloadRunnable.getRange().getCurrent() + current;
                    L.i("暂停了 rid:" + downloadRunnable.getRange().getIdkey() + " current:" + downloadRunnable.getRange().getCurrent());
                    total = downloadRunnable.getTask().getTotal();
                }
            }
            MessageSnapshot.PauseMessageSnapshot pauseMessageSnapshot =
                    new MessageSnapshot.PauseMessageSnapshot(tid,MessageType.PAUSE,total,current);
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.callback(pauseMessageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void delete(int tid) {
        List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
        if (downloadRunnableList != null) {
            for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                downloadRunnable.pause();
            }
        }

        Task task = dbManager.delete(tid);
        if (task != null) {
            File file = new File(task.getPath());
            if (file.exists()) {
                file.delete();
            }
        }
        try {
            MessageSnapshot messageSnapshot = new MessageSnapshot(tid,MessageType.DELETE);
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.callback(messageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
