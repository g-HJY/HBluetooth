package com.hjy.bluetooth.operator.abstra;

import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ClassicBluetoothPairCallBack;
import com.hjy.bluetooth.inter.ConnectCallBack;

/**
 * Created by _H_JY on 2018/10/20.
 */
public abstract class Connector {

    private int retryTimes;

    public abstract void connect(BluetoothDevice device, ConnectCallBack connectCallBack);

    public abstract void connect(BluetoothDevice device, ClassicBluetoothPairCallBack classicBluetoothPairCallBack, ConnectCallBack connectCallBack);

    public abstract void connect(BluetoothDevice device, ConnectCallBack connectCallBack, BleNotifyCallBack notifyCallBack);

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }
}
