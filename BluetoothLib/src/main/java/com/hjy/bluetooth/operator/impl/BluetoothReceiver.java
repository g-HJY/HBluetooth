package com.hjy.bluetooth.operator.impl;

import com.hjy.bluetooth.inter.ReceiveCallBack;
import com.hjy.bluetooth.operator.abstra.Receiver;

/**
 * author : HJY
 * date   : 2021/11/12
 * desc   :
 */
public class BluetoothReceiver extends Receiver {

    private ReceiveCallBack receiveCallBack;

    @Override
    public void setReceiveCallBack(ReceiveCallBack receiveCallBack) {
        this.receiveCallBack = receiveCallBack;
    }

    @Override
    public ReceiveCallBack getReceiveCallBack() {
        return this.receiveCallBack;
    }
}
