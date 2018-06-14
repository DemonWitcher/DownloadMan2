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
        if(checkIsCompleted(tid)){
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
         */
    }

    public void pause(int tid) {
        if(checkIsCompleted(tid)){
            return;
        }
        try {
            long current = 0;
            long total = 0;
            FirstConnection firstConnection = connectionMap.get(tid);
            if (firstConnection != null) {
                connectionMap.remove(tid);
                firstConnection.pause();//下载线程暂停了 给内存里的数据 准备线程暂停了 给数据库里的数据
                long[] totalAndCurrent = firstConnection.getTotalAndCurrent();
                total = totalAndCurrent[0];
                current = totalAndCurrent[1];
            } else {
                List<DownloadRunnable> downloadRunnableList = downloadMap.get(tid);
                if (downloadRunnableList != null) {
                    downloadMap.remove(tid);
                    for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                        downloadRunnable.pause();
                        //在快速响应暂停和暂停时回调最新进度之间 如何取舍
                        //这里怎么能尽可能拿到最新的呢  内存里有的range 拿内存 完成了的range 拿数据库
                        //downloadRunnableList 比库里少的range 去库里拿进度
                    }
                    Task task = dbManager.selTask(tid);
                    if (task != null) {
                        for (Range range : task.getRanges()) {
                            boolean isContains = false;
                            for (DownloadRunnable downloadRunnable : downloadRunnableList) {
                                if (downloadRunnable.getRange().getIdkey().equals(range.getIdkey())) {
                                    current = current + downloadRunnable.getRange().getCurrent();
                                    isContains = true;
                                    break;
                                }
                            }
                            if (!isContains) {
                                current = current + range.getCurrent();
                            }
                        }
                        total = task.getTotal();
                    } else {
                        L.e("库里缺失下载中的任务 返回了min_value作为current和total");
                        current = Integer.MIN_VALUE;
                        total = Integer.MIN_VALUE;
                    }
                } else {//内存里没有这个任务 用户连续点了多次暂停就会走到这里 从数据库里读一下进度给用户吧
                    L.e("读不到任务组 被删除了");
                    Task task = dbManager.selTask(tid);
                    if (task != null) {
                        current = task.getCurrent();
                        total = task.getTotal();
                    } else {
                        L.e("暂停时库里和内存中都没任务 返回了min_value作为current和total");
                        current = Integer.MIN_VALUE;
                        total = Integer.MIN_VALUE;
                    }
                }
            }
            MessageSnapshot.PauseMessageSnapshot pauseMessageSnapshot =
                    new MessageSnapshot.PauseMessageSnapshot(tid, MessageType.PAUSE, total, current);
            for (IDownloadCallback downloadCallback : callbackList) {
                L.w("给出暂停回调");
                downloadCallback.callback(pauseMessageSnapshot);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
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
