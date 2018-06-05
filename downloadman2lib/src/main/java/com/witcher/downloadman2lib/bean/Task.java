package com.witcher.downloadman2lib.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

import java.util.List;

@Entity
public class Task {

    @Id(autoincrement = true)
    private Long idkey;

    private int tid;
    private String url;
    private String path;
    private String fileName;
    private int state;
    private int connectionCount;
    private long total;
    private long current;
    private String eTag;
    @Transient
    private List<Range> ranges;

    public Task(String url,String path,String fileName,int tid,int rangeNumber,long total){
        this.url = url;
        this.path = path;
        this.fileName = fileName;
        this.tid = tid;
        this.connectionCount = rangeNumber;
        this.total = total;
    }

    @Generated(hash = 833705904)
    public Task(Long idkey, int tid, String url, String path, String fileName,
            int state, int connectionCount, long total, long current, String eTag) {
        this.idkey = idkey;
        this.tid = tid;
        this.url = url;
        this.path = path;
        this.fileName = fileName;
        this.state = state;
        this.connectionCount = connectionCount;
        this.total = total;
        this.current = current;
        this.eTag = eTag;
    }

    @Generated(hash = 733837707)
    public Task() {
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(int connectionCount) {
        this.connectionCount = connectionCount;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public Long getIdkey() {
        return this.idkey;
    }

    public void setIdkey(Long idkey) {
        this.idkey = idkey;
    }

    public String getETag() {
        return this.eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    @Override
    public String toString() {
        return "Task{" +
                "tid=" + tid +
                '}';
    }
}
