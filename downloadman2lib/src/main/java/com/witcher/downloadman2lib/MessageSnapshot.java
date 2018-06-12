package com.witcher.downloadman2lib;

import android.os.Parcel;
import android.os.Parcelable;

public class MessageSnapshot implements Parcelable {

    public int tid;
    public int type;

    public MessageSnapshot(int tid,int type) {
        this.tid = tid;
        this.type = type;
    }

    public static class PauseMessageSnapshot extends ProgressMessageSnapshot{

        public PauseMessageSnapshot(int tid,int type, long total,long current) {
            super(tid,type,total,current);
        }
        protected PauseMessageSnapshot(Parcel in) {
            super(in);
        }
    }

    public static class ProgressMessageSnapshot extends CompleteMessageSnapshot {
        public long current;

        public ProgressMessageSnapshot(int tid,int type, long total, long current) {
            super(tid,type,total);
            this.current = current;
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(current);
        }

        @Override
        public void readFromParcel(Parcel dest) {
            super.readFromParcel(dest);
            current = dest.readLong();
        }

        protected ProgressMessageSnapshot(Parcel in) {
            super(in);
            current = in.readLong();
        }
    }
    public static class CompleteMessageSnapshot extends MessageSnapshot{
        public long total;

        public CompleteMessageSnapshot(int tid,int type, long total) {
            super(tid,type);
            this.total = total;
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(total);
        }

        @Override
        public void readFromParcel(Parcel dest) {
            super.readFromParcel(dest);
            total = dest.readLong();
        }

        protected CompleteMessageSnapshot(Parcel in) {
            super(in);
            total = in.readLong();
        }
    }
    public static class ErrorMessageSnapshot extends MessageSnapshot {
        public int code;
        public String message;

        public ErrorMessageSnapshot(int tid,int type, int code, String message) {
            super(tid,type);
            this.code = code;
            this.message = message;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(code);
            dest.writeString(message);
        }

        @Override
        public void readFromParcel(Parcel dest) {
            super.readFromParcel(dest);
            code = dest.readInt();
            message = dest.readString();
        }

        protected ErrorMessageSnapshot(Parcel in) {
            super(in);
            code = in.readInt();
            message = in.readString();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type);
        dest.writeInt(this.tid);
        dest.writeInt(this.type);
    }

    public void readFromParcel(Parcel dest) {
        tid = dest.readInt();
        type = dest.readInt();
    }

    protected MessageSnapshot(Parcel in) {
        this.tid = in.readInt();
        this.type = in.readInt();
    }

    public static final Parcelable.Creator<MessageSnapshot> CREATOR = new Parcelable.Creator<MessageSnapshot>() {
        @Override
        public MessageSnapshot createFromParcel(Parcel source) {
            int type = source.readInt();
            switch (type){
                case MessageType.PROGRESS:{
                    return new ProgressMessageSnapshot(source);
                }
                case MessageType.COMPLETED:{
                    return new CompleteMessageSnapshot(source);
                }
                case MessageType.PAUSE:{
                    return new PauseMessageSnapshot(source);
                }
                case MessageType.START:{
                    return new MessageSnapshot(source);
                }
                case MessageType.CONNECTED:{
                    return new MessageSnapshot(source);
                }
                case MessageType.DELETE:{
                    return new MessageSnapshot(source);
                }
                case MessageType.ERROR:{
                    return new ErrorMessageSnapshot(source);
                }
            }
            return new MessageSnapshot(source);
        }

        @Override
        public MessageSnapshot[] newArray(int size) {
            return new MessageSnapshot[size];
        }
    };
}
