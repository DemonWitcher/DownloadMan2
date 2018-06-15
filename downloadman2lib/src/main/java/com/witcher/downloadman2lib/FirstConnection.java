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

public class FirstConnection implements Runnable {

    private String url;
    private String path;
    private int tid;
    private Task task;
    public volatile boolean isPause;
    private API api;
    private ThreadPoolExecutor executor;
    private DBManager dbManager;
    private Map<Integer, List<DownloadRunnable>> downloadMap;
    private Map<Integer, FirstConnection> connectionMap;
    private List<IDownloadCallback> callbackList;

    public FirstConnection(String url, String path, int tid, ThreadPoolExecutor executor, DBManager dbManager,
                           List<IDownloadCallback> callbackList, Map<Integer, List<DownloadRunnable>> downloadMap
            , Map<Integer, FirstConnection> connectionMap) {
        this.url = url;
        this.path = path;
        this.tid = tid;
        api = RetrofitProvider.getInstance().create(API.class);
        this.executor = executor;
        this.dbManager = dbManager;
        this.downloadMap = downloadMap;
        this.callbackList = callbackList;
        this.connectionMap = connectionMap;
        //最好对外提供task的total和current
    }

    public void pause() {
        isPause = true;
        List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
        if (downloadRunnableList != null) {
            for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                if (downloadRunnable.isPause) {
                    L.e("tid:" + tid + "  下载任务已经处于暂停状态了");
                } else {
                    downloadRunnable.pause();
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            MessageSnapshot messageSnapshot = new MessageSnapshot(tid, MessageType.CONNECTED);
            for (IDownloadCallback downloadCallback : callbackList) {
                if (isPause) {
                    L.w("准备线程给 连接回调 前暂停");
                    return;
                }
                L.w("给出连接中回调");
                downloadCallback.callback(messageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (isPause) {
            L.w("准备线程给 开始请求 前暂停");
            return;
        }
        Call<Void> call = api.getHttpHeader(url);
        try {
            L.w("准备线程 建立连接");
            Response<Void> response = call.execute();
            if (isPause) {
                L.w("准备线程给 读库分区 前暂停");
                return;
            }
            if (response.isSuccessful()) {
                L.w("准备线程 连接成功");
                long length = Long.valueOf(response.headers().get("Content-Length"));
                String etag = response.headers().get("etag");
                L.i("length:" + length);
                task = dbManager.selTask(tid);
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
        } finally {
            L.e("准备线程运行结束 isPause:" + isPause);
            //这里判断状态 完成 暂停 还是错误
            connectionMap.remove(tid);
            if (isPause) {
                //这里可以改一改 mgr里只暂停准备线程 准备线程暂停任务线程
                //这里给出暂停回调和进度
                downloadMap.remove(tid);
                callbackPause();
            } else {
                //准备线程不是暂停状态  检测工作线程状态
                checkResult();
            }
        }
    }

    private void callbackPause() {
        long current;
        long total = task.getTotal();
        Task task = dbManager.selTask(tid);
        if (task != null) {
            current = task.getCurrent();
        } else {
            L.e("库里缺失下载中的任务 返回了min_value作为current和total");
            current = Integer.MIN_VALUE;
        }
        MessageSnapshot.PauseMessageSnapshot pauseMessageSnapshot =
                new MessageSnapshot.PauseMessageSnapshot(tid, MessageType.PAUSE, total, current);
        try {
            for (IDownloadCallback downloadCallback : callbackList) {
                L.w("给出暂停回调");
                downloadCallback.callback(pauseMessageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void callbackCompleted() {
        MessageSnapshot.CompleteMessageSnapshot completeMessageSnapshot =
                new MessageSnapshot.CompleteMessageSnapshot(task.getTid(), MessageType.COMPLETED,
                        task.getTotal());
        try {
            for (IDownloadCallback downloadCallback : callbackList) {
                L.w("tid:" + tid + "  给出完成回调");
                downloadCallback.callback(completeMessageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void checkResult() {
        //全部都是完成 就应该是完成
        boolean completed = true;
        List<DownloadRunnable> runnableList = downloadMap.get(tid);
        if (runnableList == null) {
            return;
        }
        for (DownloadRunnable downloadRunnable : runnableList) {
            if (!downloadRunnable.isCompleted) {
                completed = false;
            }
        }
        downloadMap.remove(tid);
        if (completed) {
            task.setCurrent(task.getTotal());
            dbManager.updateTask(task);
            callbackCompleted();
        }
    }

    private void startDownloadRunnable(Task task) {
        List<Callable<Object>> subTasks = new ArrayList<>(task.getRanges().size());
        List<DownloadRunnable> runnableList = new ArrayList<>();
        for (int i = 0; i < task.getRanges().size(); ++i) {
            Range range = task.getRanges().get(i);
            if (range.getState() == State.COMPLETED) {
                L.i("rid:" + range.getIdkey() + "  已经完成 不加入线程池了");
            } else {
                range.setState(State.PREPARE);//这里判断range状态,如果是已经完成了的 就不修改状态并且加入线程池了
                DownloadRunnable downloadRunnable = new DownloadRunnable(range, task, dbManager, callbackList);
                runnableList.add(downloadRunnable);
                subTasks.add(Executors.callable(downloadRunnable));
            }
        }
        if (isPause) {
            L.w("准备线程给 下载任务存map 前暂停");
            return;
        }
        downloadMap.put(task.getTid(), runnableList);
        try {
            if (isPause) {
                L.w("准备线程给 启动下载任务 前暂停");
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
