package com.witcher.downloadman2lib.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.witcher.downloadman2lib.bean.Range;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "RANGE".
*/
public class RangeDao extends AbstractDao<Range, Long> {

    public static final String TABLENAME = "RANGE";

    /**
     * Properties of entity Range.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Idkey = new Property(0, Long.class, "idkey", true, "_id");
        public final static Property Rid = new Property(1, int.class, "rid", false, "RID");
        public final static Property Tid = new Property(2, int.class, "tid", false, "TID");
        public final static Property Current = new Property(3, long.class, "current", false, "CURRENT");
        public final static Property Start = new Property(4, long.class, "start", false, "START");
        public final static Property End = new Property(5, long.class, "end", false, "END");
        public final static Property State = new Property(6, int.class, "state", false, "STATE");
    }


    public RangeDao(DaoConfig config) {
        super(config);
    }
    
    public RangeDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"RANGE\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: idkey
                "\"RID\" INTEGER NOT NULL ," + // 1: rid
                "\"TID\" INTEGER NOT NULL ," + // 2: tid
                "\"CURRENT\" INTEGER NOT NULL ," + // 3: current
                "\"START\" INTEGER NOT NULL ," + // 4: start
                "\"END\" INTEGER NOT NULL ," + // 5: end
                "\"STATE\" INTEGER NOT NULL );"); // 6: state
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"RANGE\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Range entity) {
        stmt.clearBindings();
 
        Long idkey = entity.getIdkey();
        if (idkey != null) {
            stmt.bindLong(1, idkey);
        }
        stmt.bindLong(2, entity.getRid());
        stmt.bindLong(3, entity.getTid());
        stmt.bindLong(4, entity.getCurrent());
        stmt.bindLong(5, entity.getStart());
        stmt.bindLong(6, entity.getEnd());
        stmt.bindLong(7, entity.getState());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Range entity) {
        stmt.clearBindings();
 
        Long idkey = entity.getIdkey();
        if (idkey != null) {
            stmt.bindLong(1, idkey);
        }
        stmt.bindLong(2, entity.getRid());
        stmt.bindLong(3, entity.getTid());
        stmt.bindLong(4, entity.getCurrent());
        stmt.bindLong(5, entity.getStart());
        stmt.bindLong(6, entity.getEnd());
        stmt.bindLong(7, entity.getState());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public Range readEntity(Cursor cursor, int offset) {
        Range entity = new Range( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // idkey
            cursor.getInt(offset + 1), // rid
            cursor.getInt(offset + 2), // tid
            cursor.getLong(offset + 3), // current
            cursor.getLong(offset + 4), // start
            cursor.getLong(offset + 5), // end
            cursor.getInt(offset + 6) // state
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Range entity, int offset) {
        entity.setIdkey(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setRid(cursor.getInt(offset + 1));
        entity.setTid(cursor.getInt(offset + 2));
        entity.setCurrent(cursor.getLong(offset + 3));
        entity.setStart(cursor.getLong(offset + 4));
        entity.setEnd(cursor.getLong(offset + 5));
        entity.setState(cursor.getInt(offset + 6));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Range entity, long rowId) {
        entity.setIdkey(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Range entity) {
        if(entity != null) {
            return entity.getIdkey();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Range entity) {
        return entity.getIdkey() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
