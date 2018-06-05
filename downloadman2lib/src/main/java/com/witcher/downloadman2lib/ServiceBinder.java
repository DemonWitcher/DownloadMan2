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

import com.witcher.downloadman2lib.BaseMessage.Type;
import com.witcher.downloadman2lib.BaseMessage.ProgressMessage;
import com.witcher.downloadman2lib.BaseMessage.ErrorMessage;
import com.witcher.downloadman2lib.BaseMessage.CompleteMessage;

import java.util.HashMap;
import java.util.Map;

public class ServiceBinder {

    private IDownloadService mIDownloadService;
    private boolean mIsBind;
    private Map<Integer, DownloadListener> listenerMap;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Type.PROGRESS: {
                    ProgressMessage progressMessage = (ProgressMessage) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(progressMessage.tid);
                    if (downloadListener != null) {
                        downloadListener.onProgress(progressMessage.tid, progressMessage.current, progressMessage.total);
                    }
                }
                break;
                case Type.COMPLETED: {
                    CompleteMessage completeMessage = (CompleteMessage) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(completeMessage.tid);
                    if (downloadListener != null) {
                        downloadListener.onCompleted(completeMessage.tid,completeMessage.total);
                    }
                }
                break;
                case Type.PAUSE: {
                    int tid = (int) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(tid);
                    if (downloadListener != null) {
                        downloadListener.onPause(tid);
                    }
                }
                break;
                case Type.START: {
                    int tid = (int) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(tid);
                    if (downloadListener != null) {
                        downloadListener.onStart(tid);
                    }
                }
                break;
                case Type.DELETE: {
                    int tid = (int) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(tid);
                    if (downloadListener != null) {
                        downloadListener.onDelete(tid);
                    }
                }
                break;
                case Type.CONNECTED: {
                    int tid = (int) msg.obj;
                    DownloadListener downloadListener = listenerMap.get(tid);
                    if (downloadListener != null) {
                        downloadListener.onConnected(tid);
                    }
                }
                break;
                case Type.ERROR: {
                    ErrorMessage errorMessage = (ErrorMessage) msg.obj;
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
        public void onProgress(int tid, long current, long total) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.PROGRESS, new ProgressMessage(tid, total, current)));
        }

        @Override
        public void onCompleted(int tid,long total) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.COMPLETED, new CompleteMessage(tid,total)));
        }

        @Override
        public void onPause(int tid) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.PAUSE, tid));
        }

        @Override
        public void onStart(int tid) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.START, tid));
        }

        @Override
        public void onConnected(int tid) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.CONNECTED, tid));
        }

        @Override
        public void onDelete(int tid) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.DELETE, tid));
        }

        @Override
        public void onError(int tid, int code, String message) throws RemoteException {
            handler.sendMessage(Message.obtain(handler, Type.ERROR, new ErrorMessage(tid, code, message)));
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIDownloadService = IDownloadService.Stub.asInterface(service);
            mIsBind = true;
            listenerMap = new HashMap<>();
            try {
                mIDownloadService.registerCallback(mIDownloadCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                mIDownloadService.unregisterCallback(mIDownloadCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mIDownloadService = null;
            mIsBind = false;
        }
    };

    public void bindService() {
        Context context = DownloadMan.getContext();
        Intent intent = new Intent(context, DownloadService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public boolean isBind() {
        return mIsBind;
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
                mIDownloadService.delete(tid);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
