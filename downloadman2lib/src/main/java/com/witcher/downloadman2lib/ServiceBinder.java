package com.witcher.downloadman2lib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.SparseArray;

import com.witcher.downloadman2lib.MessageSnapshot.CompleteMessageSnapshot;
import com.witcher.downloadman2lib.MessageSnapshot.ErrorMessageSnapshot;
import com.witcher.downloadman2lib.MessageSnapshot.PauseMessageSnapshot;
import com.witcher.downloadman2lib.MessageSnapshot.ProgressMessageSnapshot;
import com.witcher.downloadman2lib.bean.Task;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.File;
import java.util.ArrayList;

public class ServiceBinder {

    private IDownloadService mIDownloadService;
    private boolean mIsBind;
    private SparseArray<DownloadListener> listenerMap = new SparseArray<>();

    private boolean mIsBinding;//懒加载 是否正处于启动进程中
    private ArrayList<WaitingTask> waitingTaskArrayList = new ArrayList<>(1);

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MessageSnapshot messageSnapshot = (MessageSnapshot) msg.obj;
            switch (messageSnapshot.type) {
                case MessageType.PROGRESS: {
                    ProgressMessageSnapshot progressMessage = (ProgressMessageSnapshot) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(progressMessage.tid);
                    if (downloadListener != null) {
                        downloadListener.onProgress(progressMessage.tid, progressMessage.current, progressMessage.total);
                    }
                }
                break;
                case MessageType.COMPLETED: {
                    CompleteMessageSnapshot completeMessage = (CompleteMessageSnapshot) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(completeMessage.tid);
                    if (downloadListener != null) {
                        downloadListener.onCompleted(completeMessage.tid, completeMessage.total);
                    }
                }
                break;
                case MessageType.PAUSE: {
                    PauseMessageSnapshot pauseMessage = (PauseMessageSnapshot) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(pauseMessage.tid);
                    if (downloadListener != null) {
                        downloadListener.onPause(pauseMessage.tid, pauseMessage.current, pauseMessage.total);
                    }
                }
                break;
                case MessageType.START: {
                    DownloadListener downloadListener = listenerMap.get(messageSnapshot.tid);
                    if (downloadListener != null) {
                        downloadListener.onStart(messageSnapshot.tid);
                    }
                }
                break;
                case MessageType.DELETE: {
                    DownloadListener downloadListener = listenerMap.get(messageSnapshot.tid);
                    if (downloadListener != null) {
                        downloadListener.onDelete(messageSnapshot.tid);
                    }
                }
                break;
                case MessageType.CONNECTED: {
                    DownloadListener downloadListener = listenerMap.get(messageSnapshot.tid);
                    if (downloadListener != null) {
                        downloadListener.onConnected(messageSnapshot.tid);
                    }
                }
                break;
                case MessageType.ERROR: {
                    ErrorMessageSnapshot errorMessage = (ErrorMessageSnapshot) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(errorMessage.tid);
                    if (downloadListener != null) {
//                      downloadListener.onError(tid);
                    }
                }
                break;
            }
        }
    };

    private IDownloadCallback.Stub mIDownloadCallback = new IDownloadCallback.Stub() {
        @Override
        public void callback(MessageSnapshot messageSnapshot) throws RemoteException {
            handler.sendMessage(handler.obtainMessage(0, messageSnapshot));
        }

    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            L.i("onServiceConnected");
            mIDownloadService = IDownloadService.Stub.asInterface(service);
            mIsBind = true;
            try {
                mIDownloadService.registerCallback(mIDownloadCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            mIsBinding = false;

            for (WaitingTask waitingTask : waitingTaskArrayList) {
                L.i("执行懒加载之前的开始任务");
                start(waitingTask.tid, waitingTask.url, waitingTask.path, waitingTask.downloadListener);
            }
            waitingTaskArrayList.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            L.i("onServiceDisconnected");
            if (mIDownloadService != null && mIsBind) {
                try {
                    mIDownloadService.unregisterCallback(mIDownloadCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mIDownloadService = null;
            mIsBind = false;
        }
    };

    public void bindService(int tid, String url, String path, DownloadListener downloadListener) {
        if (mIsBinding) {
            return;
        }
        mIsBinding = true;
        addWaitingTask(new WaitingTask(url, path, tid, downloadListener));
        L.i("bindService");
        Context context = DownloadMan.getContext();
        Intent intent = new Intent(context, DownloadService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void addWaitingTask(WaitingTask waitingTask) {
        waitingTaskArrayList.add(waitingTask);
    }

    public boolean isBind() {
        return mIsBind;
    }

    public boolean isBinding() {
        return mIsBinding;
    }

    public void start(int tid, String url, String path, DownloadListener downloadListener) {
        if (mIDownloadService != null && isBind()) {
            try {
                listenerMap.put(tid, downloadListener);
                mIDownloadService.start(url, path);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void pause(int tid) {
        if (mIDownloadService != null && isBind()) {
            try {
                mIDownloadService.pause(tid);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void delete(int tid) {
        if (mIDownloadService != null && isBind()) {
            try {
                L.i("通过 远程 进程删除任务");
                mIDownloadService.delete(tid);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            L.i("通过 本地 进程删除任务");
            DBManager dbManager = new DBManager(DownloadMan.getContext());
            Task task = dbManager.delete(tid);
            dbManager.close();
            if (task != null) {
                File file = new File(task.getPath());
                if (file.exists()) {
                    file.delete();
                }
            }
            handler.sendMessage(handler.obtainMessage(0, new MessageSnapshot(tid, MessageType.DELETE)));
        }
    }
}
