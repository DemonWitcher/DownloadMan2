package com.witcher.downloadman2lib.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class Range {

    @Id(autoincrement = true)
    private Long idkey;
    private int rid;
    private int tid;
    private long current;
    private long start;
    private long end;
    private int state;
    @Generated(hash = 959251110)
    public Range(Long idkey, int rid, int tid, long current, long start, long end,
            int state) {
        this.idkey = idkey;
        this.rid = rid;
        this.tid = tid;
        this.current = current;
        this.start = start;
        this.end = end;
        this.state = state;
    }
    @Generated(hash = 269891063)
    public Range() {
    }
    public Long getIdkey() {
        return this.idkey;
    }
    public void setIdkey(Long idkey) {
        this.idkey = idkey;
    }
    public int getRid() {
        return this.rid;
    }
    public void setRid(int rid) {
        this.rid = rid;
    }
    public int getTid() {
        return this.tid;
    }
    public void setTid(int tid) {
        this.tid = tid;
    }
    public long getCurrent() {
        return this.current;
    }
    public void setCurrent(long current) {
        this.current = current;
    }
    public long getStart() {
        return this.start;
    }
    public void setStart(long start) {
        this.start = start;
    }
    public long getEnd() {
        return this.end;
    }
    public void setEnd(long end) {
        this.end = end;
    }

    public synchronized int getState() {
        return this.state;
    }
    public synchronized void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Range{" +
                "idkey=" + idkey +
                ", tid=" + tid +
                ", current=" + current +
                ", start=" + start +
                ", end=" + end +
                ", state=" + state +
                '}';
    }
}
