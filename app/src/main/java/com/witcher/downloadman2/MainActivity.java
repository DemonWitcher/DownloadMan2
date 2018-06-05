package com.witcher.downloadman2;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.witcher.downloadman2lib.DownloadListener;
import com.witcher.downloadman2lib.DownloadMan;
import com.witcher.downloadman2lib.L;
import com.witcher.downloadman2lib.Util;
import com.witcher.downloadman2lib.bean.Range;
import com.witcher.downloadman2lib.bean.Task;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
//数据库 IO AIDL 网络 线程池
    //接口

    String url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
    String path = Environment.getExternalStorageDirectory() + File.separator + "2" + File.separator + "test.apk";

    private TextView mTvName, mTvId, mTvCurrent, mTvTotal, mTvSpeed, mTvState;
    private Button mBtStart, mBtPause, mBtDelete, mBtSel;
    private Button mBtDeleteAllDB;
    private ProgressBar mPbProgress;
    private DBManager dbManager;

    private int tid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initData() {
        mPbProgress.setMax(100);
        dbManager = new DBManager(this);
        List<Task> tasks = dbManager.selAllTask();
        if (tasks.size() > 0) {
            Task task = tasks.get(0);
            tid = task.getTid();
            mTvName.setText(task.getFileName());
            mTvId.setText(String.valueOf(tid));
            mTvCurrent.setText(Util.formatSize(task.getCurrent()));
            mTvTotal.setText(Util.formatSize(task.getTotal()));
            if(task.getCurrent() == task.getTotal()){
                mTvState.setText("已完成");
                mPbProgress.setProgress(100);
            }else{
                mTvState.setText("暂停中");
                mPbProgress.setProgress(Util.formatPercentInt(task.getTotal(), task.getCurrent()));
            }
            L.i("查询出来的TID :" + tid);
        }
    }

    private void initView() {
        mTvName = findViewById(R.id.tv_name);
        mTvId = findViewById(R.id.tv_id);
        mTvCurrent = findViewById(R.id.tv_current);
        mTvTotal = findViewById(R.id.tv_total);
        mTvSpeed = findViewById(R.id.tv_speed);
        mTvState = findViewById(R.id.tv_state);
        mBtStart = findViewById(R.id.bt_start);
        mBtPause = findViewById(R.id.bt_pause);
        mBtDelete = findViewById(R.id.bt_delete);
        mBtSel = findViewById(R.id.bt_sel);
        mPbProgress = findViewById(R.id.pb_progress);

        mBtStart.setOnClickListener(this);
        mBtPause.setOnClickListener(this);
        mBtDelete.setOnClickListener(this);
        mBtSel.setOnClickListener(this);

        mBtDeleteAllDB = findViewById(R.id.bt_db_delete);
        mBtDeleteAllDB.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_start: {
                tid = DownloadMan.start(url, path, downloadListener);
                mTvId.setText(String.valueOf(tid));
                mTvName.setText(new File(path).getName());
                L.i("任务ID:" + tid);
            }
            break;
            case R.id.bt_pause: {
                DownloadMan.pause(tid);
            }
            break;
            case R.id.bt_delete: {
                DownloadMan.delete(tid);
            }
            break;
            case R.id.bt_sel: {
                Task task = dbManager.selTask(tid);
                List<Range> rangeList = dbManager.selRange(tid);
                if(task!=null){
                    L.i("task :"+ task.toString());
                }
                for(Range range:rangeList){
                    L.i("range :"+ range.toString());
                }
            }
            break;
            case R.id.bt_db_delete: {
                dbManager.deleteAll();
                File file = new File(path);
                file.delete();
                L.i("删除全部数据");
            }
            break;
        }
    }

    private long lastTime;
    private long lastCurrent;

    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onProgress(int tid, long current, long total) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime > 500) {
                mTvState.setText("下载中");
                L.w("onProgress tid:" + tid + ",current:" + current + ",total:" + total);
                mTvCurrent.setText(Util.formatSize(current));
                mTvTotal.setText(Util.formatSize(total));
                long progress = current - lastCurrent;
                mTvSpeed.setText(Util.formatSize(progress * 2));
                mPbProgress.setProgress(Util.formatPercentInt(total, current));
                lastCurrent = current;
                lastTime = currentTime;
            }
        }

        @Override
        public void onCompleted(int tid,long total) {
            L.w("onCompleted tid:" + tid);
            mTvState.setText("下载完成");
            mPbProgress.setProgress(100);
            Task task = dbManager.selTask(tid);
            mTvCurrent.setText(Util.formatSize(total));
            mTvTotal.setText(Util.formatSize(total));
        }

        @Override
        public void onPause(int tid) {
            L.w("onPause tid:" + tid);
            mTvState.setText("暂停中");
        }

        @Override
        public void onDelete(int tid) {
            L.w("onDelete tid:" + tid);
            mTvState.setText("已删除");
        }

        @Override
        public void onStart(int tid) {
            L.w("tname:"+Thread.currentThread().getName());
            L.w("onStart tid:" + tid);
        }

        @Override
        public void onConnected(int tid) {
            L.w("tname:"+Thread.currentThread().getName());
            L.w("onConnected tid:" + tid);
            mTvState.setText("连接中");
        }

        @Override
        public void onError(int tid, Throwable throwable) {

        }
    };
}
