package com.witcher.downloadman2;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.witcher.downloadman2.DownloadAdapter.DownloadViewHolder;
import com.witcher.downloadman2lib.DownloadListener;
import com.witcher.downloadman2lib.DownloadMan;
import com.witcher.downloadman2lib.L;
import com.witcher.downloadman2lib.Util;
import com.witcher.downloadman2lib.bean.Range;
import com.witcher.downloadman2lib.bean.Task;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {

    private Context context;
    private DBManager dbManager;
    private List<Task> list;
    private SparseArray<Long> lastTimeMap = new SparseArray<>();
    private SparseArray<Long> lastCurrentMap = new SparseArray<>();

    DownloadAdapter(Context context, DBManager dbManager) {
        this.context = context;
        this.dbManager = dbManager;
        list = dbManager.selAllTask();
        int size = list.size();
        for (int i = size; i < 5; ++i) {
            Task task = new Task();
            task.setUrl(TEXT.url);
            task.setPath(TEXT.path + "test" + i + i + i + ".apk");
            list.add(task);
        }
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DownloadViewHolder(LayoutInflater.from(context).inflate(R.layout.item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        holder.mPbProgress.setMax(100);
        final Task task = list.get(position);
        if (task.getTid() != 0) {
            if (task.getState() == TaskState.DOWNLOADING) {
                holder.mTvState.setText("下载中");
                holder.mTvCurrent.setText(Util.formatSize(task.getCurrent()));
                holder.mTvTotal.setText(Util.formatSize(task.getTotal()));
                long lastCurrentLong = 0;
                Long lastCurrent = lastCurrentMap.get(task.getTid());
                if (lastCurrent != null) {
                    lastCurrentLong = lastCurrent;
                }
                long progress = task.getCurrent() - lastCurrentLong;
                L.i("onBindViewHolder 下载中 getCurrent:"+task.getCurrent() + ",lastCurrentLong:"+lastCurrentLong);
                holder.mTvSpeed.setText(Util.formatSize(progress * 2));
                holder.mPbProgress.setProgress(Util.formatPercentInt(task.getTotal(), task.getCurrent()));
            } else if (task.getState() == TaskState.COMPLETED) {
                holder.mTvState.setText("已完成");
                holder.mPbProgress.setProgress(100);
                holder.mTvCurrent.setText(Util.formatSize(task.getTotal()));
                holder.mTvTotal.setText(Util.formatSize(task.getTotal()));
            } else if (task.getState() == TaskState.PAUSE) {
                L.i("onBindViewHolder 暂停");
                holder.mTvState.setText("暂停中");
                holder.mTvCurrent.setText(Util.formatSize(task.getCurrent()));
                holder.mTvTotal.setText(Util.formatSize(task.getTotal()));
                holder.mPbProgress.setProgress(Util.formatPercentInt(task.getTotal(), task.getCurrent()));
            } else if (task.getState() == TaskState.ERROR) {
                holder.mTvState.setText("出错了");
            } else if (task.getState() == TaskState.DELETE) {
                holder.mTvState.setText("已删除");
            } else if (task.getState() == TaskState.PREPARE) {
                holder.mTvState.setText("连接中");
            } else {
                holder.mTvState.setText("没开始");
                holder.mPbProgress.setProgress(Util.formatPercentInt(task.getTotal(), task.getCurrent()));
            }
            holder.mTvId.setText(String.valueOf(task.getTid()));
            holder.mTvName.setText(task.getFileName());
        } else {
            holder.mTvName.setText(new File(task.getPath()).getName());
        }

        holder.mBtStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start(task);
            }
        });
        holder.mBtPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadMan.pause(task.getTid());
            }
        });
        holder.mBtDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadMan.delete(task.getTid());
            }
        });
        holder.mBtSel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Range> rangeList = dbManager.selRange(task.getTid());
                L.i("task :" + task.toString());
                for (Range range : rangeList) {
                    L.i("range :" + range.toString());
                }
            }
        });
    }

    private void start(final Task task) {
        int tid = DownloadMan.start(task.getUrl(), task.getPath(), new DownloadListener() {
            @Override
            public void onProgress(int tid, long current, long total) {
                task.setState(TaskState.DOWNLOADING);
                long currentTime = System.currentTimeMillis();
                long lastTimelong = 0;
                Long lastTime = lastTimeMap.get(tid);
                if (lastTime != null) {
                    lastTimelong = lastTime;
                }
                if (currentTime - lastTimelong > 500) {
                    long lastCurrent = task.getCurrent();
                    task.setCurrent(current);
                    task.setTotal(total);
                    notifyDataSetChanged();
                    L.w("onProgress tid:" + tid + ",current:" + current + ",total:" + total);
                    lastCurrentMap.put(tid, lastCurrent);
                    lastTimeMap.put(tid, currentTime);
                }
            }

            @Override
            public void onCompleted(int tid, long total) {
                L.w("onCompleted tid:" + tid + ",total:" + total);
                task.setState(TaskState.COMPLETED);
                task.setTotal(total);
                notifyDataSetChanged();
            }

            @Override
            public void onPause(int tid, long current, long total) {
                L.w("onPause tid:" + tid + ",current:" + current + ",total:" + total);
                task.setState(TaskState.PAUSE);
                task.setTotal(total);
                task.setCurrent(current);
                Task task1 = list.get(0);
                L.i("task1:" + task1.toString());
                notifyDataSetChanged();
            }

            @Override
            public void onDelete(int tid) {
                L.w("onDelete tid:" + tid);
                task.setState(TaskState.DELETE);
                notifyDataSetChanged();
            }

            @Override
            public void onStart(int tid) {

            }

            @Override
            public void onConnected(int tid) {
                L.w("onConnected tid:" + tid);
                task.setState(TaskState.PREPARE);
                notifyDataSetChanged();
            }

            @Override
            public void onError(int tid, Throwable throwable) {
                task.setState(TaskState.ERROR);
                notifyDataSetChanged();
            }
        });
        task.setTid(tid);
        L.i("任务ID:" + tid);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {

        private TextView mTvName, mTvId, mTvCurrent, mTvTotal, mTvSpeed, mTvState;
        private Button mBtStart, mBtPause, mBtDelete, mBtSel;
        private ProgressBar mPbProgress;

        DownloadViewHolder(View itemView) {
            super(itemView);
            mTvName = itemView.findViewById(R.id.tv_name);
            mTvId = itemView.findViewById(R.id.tv_id);
            mTvCurrent = itemView.findViewById(R.id.tv_current);
            mTvTotal = itemView.findViewById(R.id.tv_total);
            mTvSpeed = itemView.findViewById(R.id.tv_speed);
            mTvState = itemView.findViewById(R.id.tv_state);
            mBtStart = itemView.findViewById(R.id.bt_start);
            mBtPause = itemView.findViewById(R.id.bt_pause);
            mBtDelete = itemView.findViewById(R.id.bt_delete);
            mBtSel = itemView.findViewById(R.id.bt_sel);
            mPbProgress = itemView.findViewById(R.id.pb_progress);
        }
    }

}
