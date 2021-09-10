package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.operator.abstra.Sender;

/**
 * Created by _H_JY on 2018/10/20.
 */
public interface ConnectCallBack {

    void onConnecting();

    void onConnected(Sender sender);

    void onDisConnecting();

    void onDisConnected();

    void onError(int errorType,String errorMsg);
}
