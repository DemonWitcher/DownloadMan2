package com.witcher.downloadman2lib;

import android.os.RemoteException;

import com.witcher.downloadman2lib.bean.Range;
import com.witcher.downloadman2lib.bean.Task;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadRunnable implements Runnable {

    public static final int BUFFER_SIZE = 1024 * 8;

    private Range range;
    private Task task;
    private API api;
    private long lastTime;
    private volatile boolean isPause;
    private DBManager dbManager;
    private List<IDownloadCallback> callbackList;
    private Map<Integer, List<DownloadRunnable>> downloadMap;

    public DownloadRunnable(Range range, Task task, DBManager dbManager, List<IDownloadCallback> callbackList
    ,Map<Integer, List<DownloadRunnable>> downloadMap) {
        this.range = range;
        this.task = task;
        api = RetrofitProvider.getInstance().create(API.class);
        this.dbManager = dbManager;
        this.callbackList = callbackList;
        this.downloadMap = downloadMap;
    }

    public void pause() {
        L.i("rid:" + range.getIdkey() + " 暂停");
        isPause = true;
    }

    public Range getRange() {
        return range;
    }

    public Task getTask() {
        return task;
    }

    @Override
    public void run() {
        //建立连接 IO写入 插入数据库
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        BufferedOutputStream outputStream = null;
        try {
            String strRange = "bytes=" + (range.getCurrent() + range.getStart()) + "-" + range.getEnd();

            Call<ResponseBody> call = api.download(strRange, task.getUrl());
            Response<ResponseBody> response = call.execute();

            inputStream = response.body().byteStream();
            raf = new RandomAccessFile(new File(task.getPath()), "rw");
            outputStream = new BufferedOutputStream(new FileOutputStream(raf.getFD()));

            raf.seek(range.getStart() + range.getCurrent());

            byte[] bytes = new byte[BUFFER_SIZE];
            int byteCount;

            L.i("rid:" + range.getIdkey() + ",开始下载");
            range.setState(State.DOWNLOADING);
            MessageSnapshot startMessageSnapshot = new MessageSnapshot(task.getTid(),MessageType.START);
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.callback(startMessageSnapshot);
            }
            while (true) {
                byteCount = inputStream.read(bytes);
                if (byteCount == -1) {
                    outputStream.flush();
                    break;
                }
                outputStream.write(bytes, 0, byteCount);
                range.setCurrent(range.getCurrent() + byteCount);
                long currentTime = System.currentTimeMillis();
                for (IDownloadCallback downloadCallback : callbackList) {
                    long allCurrent = 0;
                    for (Range range : task.getRanges()) {
                        allCurrent = allCurrent + range.getCurrent();
                    }
                    MessageSnapshot.ProgressMessageSnapshot progressMessageSnapshot =
                            new MessageSnapshot.PauseMessageSnapshot(task.getTid(),MessageType.PROGRESS,
                                    task.getTotal(),allCurrent);
                    downloadCallback.callback(progressMessageSnapshot);
                }
                if (currentTime - lastTime > 2000) {
                    if(!isPause){
                        lastTime = currentTime;
                        outputStream.flush();
                        dbManager.updateRange(range);
                    }
                    L.i("rid:" + range.getIdkey() + ",progress:" + range.getCurrent());
                }
//                L.i("写入了一次 rid:"+range.getIdkey()+"progress:"+range.getCurrent());
                if (isPause) {
                    outputStream.flush();
                    range.setCurrent(range.getCurrent() - byteCount);
                    dbManager.updateRange(range);//暂停回调给完后 下载线程还能再写入一次数据
                    break;
                }
            }
            if (!isPause) {
                if (response.isSuccessful()) {
                    L.i("rid:" + range.getIdkey() + "下载成功");
                    range.setState(State.COMPLETED);
                    dbManager.updateRange(range);

                    List<Range> rangeList = task.getRanges();
                    boolean isCompleted = true;
                    for (Range range : rangeList) {
                        if (range.getState() != State.COMPLETED) {
                            isCompleted = false;
                        }
                    }
                    if (isCompleted) {
                        task.setCurrent(task.getTotal());
                        dbManager.updateTask(task);
                        MessageSnapshot.CompleteMessageSnapshot completeMessageSnapshot =
                                new MessageSnapshot.CompleteMessageSnapshot(task.getTid(),MessageType.COMPLETED,
                                        task.getTotal());
                        for (IDownloadCallback downloadCallback : callbackList) {
                            downloadCallback.callback(completeMessageSnapshot);
                        }
                    }
                } else {
                    L.i("rid:" + range.getIdkey() + "下载失败");
                }
            } else {
                L.i("rid:" + range.getIdkey() + "因为暂停结束");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            downloadMap.remove(task.getTid());
        }
    }
}
