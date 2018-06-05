package com.witcher.downloadman2lib;

import android.os.RemoteException;

import com.witcher.downloadman2lib.bean.Range;
import com.witcher.downloadman2lib.bean.Task;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

public class DownloadManager {

    public static final int RANGER_NUMBER = 3;

    private ThreadPoolExecutor executor;
    private API api;
    private DBManager dbManager;
    private Map<Integer, List<DownloadRunnable>> downloadMap;
    private List<IDownloadCallback> callbackList;

    public DownloadManager() {
        downloadMap = new ConcurrentHashMap<>();
        api = RetrofitProvider.getInstance().create(API.class);
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

    //函数1
    public void start(String url, String path) {
        //4.查询本地是否存在
        //	5.第一次连接 拿长度,是否支持range,拿eTag
        //6.本地不存在  检查本地磁盘空间 创建文件 插入TASK 任务分区 插入RANGE,,走线程池 开始下载
        //		7.本地存在,eTag没过期,读取分区数据 检查本地文件
        //			如果文件存在 下载进度 开始下载
        //			如果文件不存在 走步骤8
        //		8.本地存在,eTag过期,删除本地文件,清理数据库,然后走步骤6
        int tid = Util.generateId(url, path);
        try {
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.onConnected(tid);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Call<Void> call = api.getHttpHeader(url);
        try {
            Response<Void> response = call.execute();
            if (response.isSuccessful()) {
                long length = Long.valueOf(response.headers().get("Content-Length"));
                String etag = response.headers().get("etag");
                L.i("length:" + length);
                Task task = dbManager.selTask(tid);
                File taskFile = new File(path);
                if (task == null) {
                    L.i("新任务");
                    task = new Task(url, path, taskFile.getName(), tid, RANGER_NUMBER,length);
                    prepare(length, taskFile, task);
                    startDownloadRunnable(task);
                } else {
                    if (taskFile.exists()) {
                        //继续下载 读取数据库分区数据 继续下载 这里后面在加上校验etag或者lastModify
                        L.i("继续下载");
                        List<Range> rangeList = dbManager.selRange(tid);
                        task.setRanges(rangeList);
                        startDownloadRunnable(task);
                    } else {
                        L.i("文件不存在 重新下载");
                        dbManager.delete(tid);
                        prepare(length, taskFile, task);
                        startDownloadRunnable(task);
                    }
                }
            } else {
                L.i("网络请求失败 message:" + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void createRange(Task task, long length) {
        ArrayList<Range> ranges = new ArrayList<>(RANGER_NUMBER);
        dbManager.addTask(task);
        long size = length / RANGER_NUMBER;
        for (int i = 0; i < RANGER_NUMBER; ++i) {
            Range range = new Range();
            range.setTid(task.getTid());
            range.setStart(i * size);
            if (i == RANGER_NUMBER - 1) {
                range.setEnd(length - 1);
            } else {
                range.setEnd((i + 1) * size - 1);
            }
            dbManager.addRange(range);
            L.i("range" + i + ",start:" + range.getStart() + ",end:" + range.getEnd());
            ranges.add(range);
        }
        task.setRanges(ranges);
    }

    private void startDownloadRunnable(Task task) {
        List<Callable<Object>> subTasks = new ArrayList<>(task.getRanges().size());
        List<DownloadRunnable> runnableList = new ArrayList<>();
        for (int i = 0; i < task.getRanges().size(); ++i) {
            Range range = task.getRanges().get(i);
            range.setState(State.PREPARE);
            DownloadRunnable downloadRunnable = new DownloadRunnable(range, task, dbManager, callbackList);
            runnableList.add(downloadRunnable);
            subTasks.add(Executors.callable(downloadRunnable));
        }
        downloadMap.put(task.getTid(), runnableList);
        try {
            executor.invokeAll(subTasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void prepare(long length, File taskFile, Task task) {
        if (length > Util.getFreeSpaceBytes(taskFile.getParent())) {
            //磁盘空间不足
        } else {
            RandomAccessFile raf = null;
            try {
                L.i("创建文件");
                if (taskFile.exists()) {
                    taskFile.delete();
                }
                raf = new RandomAccessFile(taskFile, "rw");
                raf.setLength(length);
                raf.close();
                createRange(task, length);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void pause(int tid) {
        List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
        if (downloadRunnableList != null) {
            for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                downloadRunnable.pause();
            }
        }
        try {
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.onPause(tid);
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
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.onDelete(tid);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
