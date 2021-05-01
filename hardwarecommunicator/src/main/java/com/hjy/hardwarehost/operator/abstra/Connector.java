package com.hjy.hardwarehost.operator.abstra;

import com.hjy.hardwarehost.entity.BluetoothDevice;
import com.hjy.hardwarehost.inter.ConnectCallBack;

/**
 * Created by _H_JY on 2018/10/20.
 */
public abstract class Connector {

    public abstract void connect(BluetoothDevice device, ConnectCallBack connectCallBack);

}
