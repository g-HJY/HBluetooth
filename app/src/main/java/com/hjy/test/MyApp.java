package com.hjy.test;

import android.app.Application;

import com.hjy.bluetooth.HBluetooth;

/**
 * author : HJY
 * date   : 2021/11/12
 * desc   :
 */
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化 HBluetooth
        HBluetooth.init(this);
        HBluetooth.getInstance()
                .setConnectTimeOut(5000);
    }
}
