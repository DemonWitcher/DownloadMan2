// IDownloadCallback.aidl
package com.witcher.downloadman2lib;

import com.witcher.downloadman2lib.MessageSnapshot;

interface IDownloadCallback {

   oneway void callback(in MessageSnapshot messageSnapshot);
}
