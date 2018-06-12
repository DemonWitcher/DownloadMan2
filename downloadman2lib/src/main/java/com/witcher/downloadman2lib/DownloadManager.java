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
        FirstConnection firstConnection = new FirstConnection(url,path,tid,executor,dbManager,callbackList,downloadMap,connectionMap);
        connectionMap.put(tid,firstConnection);
        executor.execute(firstConnection);
        //AIDL接口线程 ->准备线程->下载线程
        //开始下载
        /*
            主进程UI->AIDL接口线程->创建准备线程->加入map->执行准备线程->连接中回调->第一次连接->
            检测数据库和本地文件->创建下载线程->加入map->执行下载线程->网络连接->IO|数据库|回调->数据库->完成回调
         */
        //暂停
        /*
            主进程UI->AIDL接口线程->读map暂停准备线程->读map暂停下载线程|下载线程记录进度->暂停回调
         */
        //删除
        /*
            主进程UI->AIDL接口线程->读map暂停准备线程|删除map->读map暂停下载线程|删除map->数据库|IO->删除回调
         */
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
            }else{
                List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
                if (downloadRunnableList != null) {
                    for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                        downloadRunnable.pause();
                        current = downloadRunnable.getRange().getCurrent() + current;
                        L.i("暂停了 rid:" + downloadRunnable.getRange().getIdkey() + " current:" + downloadRunnable.getRange().getCurrent());
                        total = downloadRunnable.getTask().getTotal();
                    }
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
        FirstConnection firstConnection = connectionMap.get(tid);
        if(firstConnection!=null){
            firstConnection.pause();//下载线程暂停了 给内存里的数据 准备线程暂停了 给数据库里的数据
        }
        connectionMap.remove(tid);
        List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
        if (downloadRunnableList != null) {
            for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                downloadRunnable.pause();
            }
        }
        downloadMap.remove(tid);

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
