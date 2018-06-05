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

    public DownloadRunnable(Range range, Task task, DBManager dbManager, List<IDownloadCallback> callbackList) {
        this.range = range;
        this.task = task;
        api = RetrofitProvider.getInstance().create(API.class);
        this.dbManager = dbManager;
        this.callbackList = callbackList;
    }

    public void pause() {
        L.i("rid:" + range.getIdkey() + " 暂停");
        dbManager.updateRange(range);
        isPause = true;
    }

    @Override
    public void run() {
        //建立连接 IO写入 插入数据库
        InputStream inputStream = null;
        RandomAccessFile raf = null;
        BufferedOutputStream outputStream = null;
        try {
            String strRange = "bytes=" + (range.getCurrent() + range.getStart()) + "-" + range.getEnd();
            L.i("rid:" + range.getIdkey() + ",strRange:" + strRange + ",current:" + range.getCurrent() + ",start:" + range.getStart());

            Call<ResponseBody> call = api.download(strRange, task.getUrl());
            Response<ResponseBody> response = call.execute();

            inputStream = response.body().byteStream();
            raf = new RandomAccessFile(new File(task.getPath()), "rw");
            outputStream = new BufferedOutputStream(new FileOutputStream(raf.getFD()));

            L.i("rid:" + range.getIdkey() + ",seek:" + (range.getStart() + range.getCurrent()));

            raf.seek(range.getStart() + range.getCurrent());

            byte[] bytes = new byte[BUFFER_SIZE];
            int byteCount;

            L.i("rid:" + range.getIdkey() + ",开始下载");
            range.setState(State.DOWNLOADING);
            for (IDownloadCallback downloadCallback : callbackList) {
                downloadCallback.onStart(task.getTid());
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
                    downloadCallback.onProgress(task.getTid(), allCurrent, task.getTotal());
                }
                if (currentTime - lastTime > 2000) {
                    lastTime = currentTime;
                    outputStream.flush();
                    dbManager.updateRange(range);
                    L.i("rid:" + range.getIdkey() + ",progress:" + range.getCurrent());
                }
                if (isPause) {
                    break;
                }
            }
            if (!isPause) {
                if (response.isSuccessful()) {
                    L.i("rid:" + range.getIdkey() + "下载成功");
                    range.setState(State.COMPLETED);
                    range.setCurrent(range.getEnd() - range.getStart());
                    dbManager.updateRange(range);
                    List<Range> rangeList = task.getRanges();
                    boolean isCompleted = true;
                    for (Range range : rangeList) {
                        if (range.getState() != State.COMPLETED) {
                            isCompleted = false;
                        }
                    }
                    if (isCompleted) {
                        for (IDownloadCallback downloadCallback : callbackList) {
                            downloadCallback.onCompleted(task.getTid(),task.getTotal());
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
        }
    }
}
