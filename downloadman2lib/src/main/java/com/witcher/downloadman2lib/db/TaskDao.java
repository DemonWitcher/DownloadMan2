package com.witcher.downloadman2lib.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.witcher.downloadman2lib.bean.Task;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "TASK".
*/
public class TaskDao extends AbstractDao<Task, Long> {

    public static final String TABLENAME = "TASK";

    /**
     * Properties of entity Task.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Idkey = new Property(0, Long.class, "idkey", true, "_id");
        public final static Property Tid = new Property(1, int.class, "tid", false, "TID");
        public final static Property Url = new Property(2, String.class, "url", false, "URL");
        public final static Property Path = new Property(3, String.class, "path", false, "PATH");
        public final static Property FileName = new Property(4, String.class, "fileName", false, "FILE_NAME");
        public final static Property State = new Property(5, int.class, "state", false, "STATE");
        public final static Property ConnectionCount = new Property(6, int.class, "connectionCount", false, "CONNECTION_COUNT");
        public final static Property Total = new Property(7, long.class, "total", false, "TOTAL");
        public final static Property Current = new Property(8, long.class, "current", false, "CURRENT");
        public final static Property ETag = new Property(9, String.class, "eTag", false, "E_TAG");
    }


    public TaskDao(DaoConfig config) {
        super(config);
    }
    
    public TaskDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"TASK\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: idkey
                "\"TID\" INTEGER NOT NULL ," + // 1: tid
                "\"URL\" TEXT," + // 2: url
                "\"PATH\" TEXT," + // 3: path
                "\"FILE_NAME\" TEXT," + // 4: fileName
                "\"STATE\" INTEGER NOT NULL ," + // 5: state
                "\"CONNECTION_COUNT\" INTEGER NOT NULL ," + // 6: connectionCount
                "\"TOTAL\" INTEGER NOT NULL ," + // 7: total
                "\"CURRENT\" INTEGER NOT NULL ," + // 8: current
                "\"E_TAG\" TEXT);"); // 9: eTag
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"TASK\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Task entity) {
        stmt.clearBindings();
 
        Long idkey = entity.getIdkey();
        if (idkey != null) {
            stmt.bindLong(1, idkey);
        }
        stmt.bindLong(2, entity.getTid());
 
        String url = entity.getUrl();
        if (url != null) {
            stmt.bindString(3, url);
        }
 
        String path = entity.getPath();
        if (path != null) {
            stmt.bindString(4, path);
        }
 
        String fileName = entity.getFileName();
        if (fileName != null) {
            stmt.bindString(5, fileName);
        }
        stmt.bindLong(6, entity.getState());
        stmt.bindLong(7, entity.getConnectionCount());
        stmt.bindLong(8, entity.getTotal());
        stmt.bindLong(9, entity.getCurrent());
 
        String eTag = entity.getETag();
        if (eTag != null) {
            stmt.bindString(10, eTag);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Task entity) {
        stmt.clearBindings();
 
        Long idkey = entity.getIdkey();
        if (idkey != null) {
            stmt.bindLong(1, idkey);
        }
        stmt.bindLong(2, entity.getTid());
 
        String url = entity.getUrl();
        if (url != null) {
            stmt.bindString(3, url);
        }
 
        String path = entity.getPath();
        if (path != null) {
            stmt.bindString(4, path);
        }
 
        String fileName = entity.getFileName();
        if (fileName != null) {
            stmt.bindString(5, fileName);
        }
        stmt.bindLong(6, entity.getState());
        stmt.bindLong(7, entity.getConnectionCount());
        stmt.bindLong(8, entity.getTotal());
        stmt.bindLong(9, entity.getCurrent());
 
        String eTag = entity.getETag();
        if (eTag != null) {
            stmt.bindString(10, eTag);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public Task readEntity(Cursor cursor, int offset) {
        Task entity = new Task( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // idkey
            cursor.getInt(offset + 1), // tid
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // url
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // path
            cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4), // fileName
            cursor.getInt(offset + 5), // state
            cursor.getInt(offset + 6), // connectionCount
            cursor.getLong(offset + 7), // total
            cursor.getLong(offset + 8), // current
            cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9) // eTag
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Task entity, int offset) {
        entity.setIdkey(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setTid(cursor.getInt(offset + 1));
        entity.setUrl(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setPath(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setFileName(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setState(cursor.getInt(offset + 5));
        entity.setConnectionCount(cursor.getInt(offset + 6));
        entity.setTotal(cursor.getLong(offset + 7));
        entity.setCurrent(cursor.getLong(offset + 8));
        entity.setETag(cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Task entity, long rowId) {
        entity.setIdkey(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Task entity) {
        if(entity != null) {
            return entity.getIdkey();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Task entity) {
        return entity.getIdkey() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
