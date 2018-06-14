package com.witcher.downloadman2lib.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.witcher.downloadman2lib.L;
import com.witcher.downloadman2lib.bean.Range;
import com.witcher.downloadman2lib.bean.Task;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

public class DBManager {
    private DaoMaster.DevOpenHelper mHelper;
    private SQLiteDatabase mDb;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private Context mContext;

    public DBManager(Context context) {
        this.mContext = context;
        setDatabase();
    }

    private void setDatabase() {
        mHelper = new DaoMaster.DevOpenHelper(mContext, "downloadman2-db", null);
        mDb = mHelper.getWritableDatabase();
        mDaoMaster = new DaoMaster(mDb);
        mDaoSession = mDaoMaster.newSession();
    }

    public void deleteAll() {
        mDaoSession.getRangeDao().deleteAll();
        mDaoSession.getTaskDao().deleteAll();
    }

    public Task delete(int tid) {
        Task task = selTask(tid);
        if (task != null) {
            mDaoSession.getTaskDao().delete(task);
        }
        mDaoSession.getRangeDao().deleteInTx(selRange(tid));
        return task;
    }

    public void addTask(Task task) {
        mDaoSession.getTaskDao().insert(task);
    }

    public void addRange(Range range) {
        mDaoSession.getRangeDao().insert(range);
    }

    public void updateRange(Range range) {
        L.d("修改数据库 range " + range.toString());
        mDaoSession.getRangeDao().update(range);
    }

    public void updateTask(Task task) {
        mDaoSession.getTaskDao().update(task);
    }

    public List<Range> selRange(int tid) {
        QueryBuilder<Range> queryBuilder = mDaoSession.queryBuilder(Range.class);
        List<Range> list = queryBuilder.where(RangeDao.Properties.Tid.eq(tid)).list();
        for (Range range : list) {
            L.d("读取出来的range :" + range.toString());
        }
        return list;
    }

    public Task selTask(int tid) {
        QueryBuilder<Task> queryBuilder = mDaoSession.queryBuilder(Task.class);
        List<Task> list = queryBuilder.where(TaskDao.Properties.Tid.eq(tid)).list();
        if (list.size() > 0) {
            List<Range> rangeList = selRange(tid);
            Task task = list.get(0);
            long current = 0;
            for (Range range : rangeList) {
                current = range.getCurrent() + current;
            }
            task.setRanges(rangeList);
            task.setCurrent(current);
            return task;
        } else {
            return null;
        }
    }

    public List<Task> selAllTask() {
        List<Task> taskList = mDaoSession.getTaskDao().loadAll();
        List<Range> rangeList = mDaoSession.getRangeDao().loadAll();
        for (Task task : taskList) {
            long current = 0;
            for (Range range : rangeList) {
                if (range.getTid() == task.getTid()) {
                    current = range.getCurrent() + current;
                }
            }
            task.setCurrent(current);
        }
        return taskList;
    }

    public void close() {
        if (mDb != null && mDb.isOpen()) {
            mDb.close();
        }
        if (mHelper != null) {
            mHelper.close();
        }
    }

}
