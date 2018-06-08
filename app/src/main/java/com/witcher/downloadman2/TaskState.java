package com.witcher.downloadman2;

public interface TaskState {
    int PREPARE = 1;
    int DOWNLOADING = 2;
    int COMPLETED = 3;
    int PAUSE = 4;
    int ERROR = 5;
    int DELETE = 6;
}
