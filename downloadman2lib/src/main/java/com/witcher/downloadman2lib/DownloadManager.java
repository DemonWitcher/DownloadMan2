package com.witcher.downloadman2lib;

import android.os.RemoteException;

import com.witcher.downloadman2lib.bean.Range;
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
    private Map<Integer, FirstConnection> connectionMap;
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
//        executor.shutdown();
        dbManager.close();
    }

    public void start(String url, String path) {
        int tid = Util.generateId(url, path);
        if (checkIsCompleted(tid)) {
            return;
        }
        //已经完成了的 就不参与暂停和开始了
        if (connectionMap.containsKey(tid)) {
            L.e("任务已经在连接中了");
        } else {
            if (downloadMap.containsKey(tid)) {
                L.e("任务已经在下载中了");
            } else {
                FirstConnection firstConnection = new FirstConnection(url, path, tid, executor, dbManager, callbackList, downloadMap, connectionMap);
                connectionMap.put(tid, firstConnection);
                executor.execute(firstConnection);
            }
        }
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

        //一个线程出错了 另外几个怎么办
        //一个线程完成了 暂停 再开始时候 其它的继续下载 这个怎么办

        /*
            暂停了 但3个下载线程还没停住 又开始了新任务 新建了3个放map里了 然后刚才的3个停住了 把map里的删了
            新建的3个 就不能再暂停了  连接中的也是一个道理
            假如改成立刻删除 工作线程就不会误删其它线程
            那任务完成后怎么删除呢  改成判断状态 是暂停就不删除 不是暂停就删除
         */
        //加一下接口控制 已经开始 再点开始 就什么都不做
        //已经暂停 再点暂停 也什么都不做   删除同理

        /*
            下载中  其中1个线程完成了 删了map 然后暂停 内存找不到 就无法暂停
            需要改成 全部都完成 然后才删除
            这时候 再开始 也读不到内存 所以多开了一个下载

            研究一下改成再invokeAll后面判断状态
         */

        /*
            如果已经完成了一个线程 然后暂停再开始 就只有2个下载任务再执行 此时再点暂停 回调的数据就会缺失那个
            已经完成了的任务
         */
        /*
            据观察 B站是异步的暂停 = = 我也改成异步的吧
            假如改成异步的 ,我开了下载 然后暂停 暂停回调触发前 我又开下载 然后触发了新下载连接中回调
            然后又触发了老下载暂停回调 怎么算
            给暂停回调之前判断一下是否有新下载产生 就是map里面有没有这个任务 有就不给回调了
            OR 或者 处于暂停中的状态时 不允许再开新下载
            应该UI也给控制 点了暂停 暂停完成前 就不应该允许他进行继续下载的操作 保持一个马上暂停的状态 像B站一样

         */
    }

    public void pause(int tid) {
        if (checkIsCompleted(tid)) {
            return;
        }
        //暂停中的时候不可以再暂停
        FirstConnection firstConnection = connectionMap.get(tid);
        if (firstConnection != null) {
            if(firstConnection.isPause){
                L.e("tid:"+tid+"  连接任务已经处于暂停状态了");
            }else{
                firstConnection.pause();
            }
        } else {
            List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
            if (downloadRunnableList != null) {
                for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                    if(downloadRunnable.isPause){
                        L.e("tid:"+tid+"  下载任务已经处于暂停状态了");
                    }else{
                        downloadRunnable.pause();
                    }
                }
            }
        }
    }

    public void delete(int tid) {
        FirstConnection firstConnection = connectionMap.get(tid);
        if (firstConnection != null) {
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
            MessageSnapshot messageSnapshot = new MessageSnapshot(tid, MessageType.DELETE);
            for (IDownloadCallback downloadCallback : callbackList) {
                L.w("给出删除回调");
                downloadCallback.callback(messageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private boolean checkIsCompleted(int tid) {
        Task task = dbManager.selTask(tid);
        if (task != null) {
            boolean isCompleted = true;
            for (Range range : task.getRanges()) {
                if (range.getState() != State.COMPLETED) {
                    isCompleted = false;
                }
            }
            if (isCompleted) {
                L.e("tid:" + tid + "已经完成了");
                return true;
            }
        }
        return false;
    }

}
