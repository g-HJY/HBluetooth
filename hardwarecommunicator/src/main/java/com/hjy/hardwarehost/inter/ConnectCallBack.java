package com.hjy.hardwarehost.inter;

/**
 * Created by _H_JY on 2018/10/20.
 */
public interface ConnectCallBack {

    void onConnecting();

    void onConnected();

    void onDisConnecting();

    void onDisConnected();

    void onError(int errorType,String errorMsg);
}
