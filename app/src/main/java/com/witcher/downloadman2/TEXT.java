package com.witcher.downloadman2;

import android.os.Environment;

import java.io.File;

public class TEXT {
    /**
     接口1.返回ID 开始下载(URL,PATH,listener)
     接口2.暂停(ID)
     接口3.删除任务(ID)
     接口4.返回任务 查询(ID)

     回调1.进度监听 ID,当前下载量,总下载量
     回调2.删除 ID
     回调3.暂停 ID
     回调4.错误 ID,Throwable
     回调5.完成 ID

     1.检查path ==null
     2.路径是文件还是文件夹 文件的话拿父目录文件夹
     3.检查是否存在,不存在就创建,创建失败就再检查一次
     4.查询本地是否存在
     5.第一次连接 拿长度,是否支持range,拿eTag
     6.本地不存在 检查本地磁盘空间 任务分区,存库,走线程池 开始下载
     7.本地存在,eTag没过期,读取分区数据 检查本地文件
     如果文件存在 下载进度 开始下载
     如果文件不存在 走步骤8
     8.本地存在,eTag过期,删除本地文件,清理数据库,然后走步骤6
     9.每次循环检查暂停标记 每2秒存一次下载进度

     暂停 修改下载线程标记
     */

    public static final String url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk";
    public static final String path = Environment.getExternalStorageDirectory() + File.separator + "2" + File.separator;

}
