package com.hjy.bluetooth.operator.abstra;

import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ConnectCallBack;

/**
 * Created by _H_JY on 2018/10/20.
 */
public abstract class Connector {

    public abstract void connect(BluetoothDevice device, ConnectCallBack connectCallBack);

    public abstract void connect(BluetoothDevice device, ConnectCallBack connectCallBack, BleNotifyCallBack notifyCallBack);

}
