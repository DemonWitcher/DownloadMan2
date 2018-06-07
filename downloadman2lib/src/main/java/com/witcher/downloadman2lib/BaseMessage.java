package com.witcher.downloadman2lib;

public class BaseMessage {
    public int tid;

    public BaseMessage(int tid) {
        this.tid = tid;
    }

    public static class PauseMessage extends BaseMessage{
        public long current;
        public long total;

        public PauseMessage(int tid, long current,long total) {
            super(tid);
            this.current = current;
            this.total = total;
        }
    }

    public static class ProgressMessage extends BaseMessage {
        public long total;
        public long current;

        public ProgressMessage(int tid, long total, long current) {
            super(tid);
            this.total = total;
            this.current = current;
        }
    }
    public static class CompleteMessage extends BaseMessage{
        public long total;

        public CompleteMessage(int tid, long total) {
            super(tid);
            this.total = total;
        }
    }
    public static class ErrorMessage extends BaseMessage {
        public int code;
        public String message;

        public ErrorMessage(int tid, int code, String message) {
            super(tid);
            this.code = code;
            this.message = message;
        }
    }
    public interface Type{
        int PROGRESS = 1;
        int COMPLETED = 2;
        int PAUSE = 3;
        int START = 4;
        int CONNECTED = 5;
        int DELETE = 6;
        int ERROR = 7;
    }
}
