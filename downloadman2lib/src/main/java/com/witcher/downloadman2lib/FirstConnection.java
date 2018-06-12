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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import retrofit2.Call;
import retrofit2.Response;

public class FirstConnection implements Runnable{

    private String url;
    private String path;
    private int tid;
    private volatile long length;
    private volatile boolean isPause;
    private API api;
    private ThreadPoolExecutor executor;
    private DBManager dbManager;
    private Map<Integer, List<DownloadRunnable>> downloadMap;
    private List<IDownloadCallback> callbackList;

    public FirstConnection(String url, String path, int tid, ThreadPoolExecutor executor, DBManager dbManager,
                           List<IDownloadCallback> callbackList,Map<Integer, List<DownloadRunnable>> downloadMap) {
        this.url = url;
        this.path = path;
        this.tid = tid;
        api = RetrofitProvider.getInstance().create(API.class);
        this.executor = executor;
        this.dbManager = dbManager;
        this.downloadMap = downloadMap;
        this.callbackList = callbackList;
        //最好对外提供task的total和current
    }

    public void pause(){
        isPause = true;
    }

    public long[] getTotalAndCurrent(){
        long[] totalAndCurrent = new long[2];
        Task task = dbManager.selTask(tid);
        if(length!=0){
            totalAndCurrent[0] = length;
        }else if(task!=null){
            totalAndCurrent[0] = task.getTotal();
        }else{
            totalAndCurrent[0] = 0L;
        }
        if(task == null){
            totalAndCurrent[1] = 0L;
        }else{
            totalAndCurrent[1] = task.getCurrent();
        }
        return totalAndCurrent;
    }

    @Override
    public void run() {
        try {
            MessageSnapshot messageSnapshot = new MessageSnapshot(tid,MessageType.CONNECTED);
            for (IDownloadCallback downloadCallback : callbackList) {
//                try {
//                    L.i("连接中睡眠开始");
//                    Thread.sleep(5000);
//                    L.i("连接中睡眠结束");
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                if(isPause){
                    return;
                }
                downloadCallback.callback(messageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if(isPause){
            return;
        }
        Call<Void> call = api.getHttpHeader(url);
        try {
            Response<Void> response = call.execute();
            if(isPause){
                return;
            }
            if (response.isSuccessful()) {
                length = Long.valueOf(response.headers().get("Content-Length"));
                String etag = response.headers().get("etag");
                L.i("length:" + length);
                Task task = dbManager.selTask(tid);
                File taskFile = new File(path);
                if (task == null) {
                    L.i("新任务");
                    task = new Task(url, path, taskFile.getName(), tid, DownloadManager.RANGER_NUMBER, length);
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
            if(isPause){
                return;
            }
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

    private void createRange(Task task, long length) {
        ArrayList<Range> ranges = new ArrayList<>(DownloadManager.RANGER_NUMBER);
        dbManager.addTask(task);
        long size = length / DownloadManager.RANGER_NUMBER;
        for (int i = 0; i < DownloadManager.RANGER_NUMBER; ++i) {
            Range range = new Range();
            range.setTid(task.getTid());
            range.setStart(i * size);
            if (i == DownloadManager.RANGER_NUMBER - 1) {
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
}
