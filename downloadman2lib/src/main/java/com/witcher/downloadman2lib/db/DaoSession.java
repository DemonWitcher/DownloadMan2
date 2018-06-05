package com.witcher.downloadman2lib.db;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.witcher.downloadman2lib.bean.Range;
import com.witcher.downloadman2lib.bean.Task;

import com.witcher.downloadman2lib.db.RangeDao;
import com.witcher.downloadman2lib.db.TaskDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig rangeDaoConfig;
    private final DaoConfig taskDaoConfig;

    private final RangeDao rangeDao;
    private final TaskDao taskDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        rangeDaoConfig = daoConfigMap.get(RangeDao.class).clone();
        rangeDaoConfig.initIdentityScope(type);

        taskDaoConfig = daoConfigMap.get(TaskDao.class).clone();
        taskDaoConfig.initIdentityScope(type);

        rangeDao = new RangeDao(rangeDaoConfig, this);
        taskDao = new TaskDao(taskDaoConfig, this);

        registerDao(Range.class, rangeDao);
        registerDao(Task.class, taskDao);
    }
    
    public void clear() {
        rangeDaoConfig.clearIdentityScope();
        taskDaoConfig.clearIdentityScope();
    }

    public RangeDao getRangeDao() {
        return rangeDao;
    }

    public TaskDao getTaskDao() {
        return taskDao;
    }

}
