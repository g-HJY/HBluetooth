package com.hjy.bluetooth.operator.impl;

import android.bluetooth.BluetoothGattDescriptor;

import com.hjy.bluetooth.inter.ReceiveCallBack;
import com.hjy.bluetooth.operator.abstra.Receiver;

/**
 * author : HJY
 * date   : 2021/11/12
 * desc   :
 */
public class BluetoothReceiver extends Receiver {

    private ReceiveCallBack receiveCallBack;
    private BluetoothGattDescriptor finalNotifyDescriptor;

    @Override
    public void setReceiveCallBack(ReceiveCallBack receiveCallBack) {
        this.receiveCallBack = receiveCallBack;
    }

    @Override
    public ReceiveCallBack getReceiveCallBack() {
        return this.receiveCallBack;
    }

    @Override
    public void resetCallBack() {
        receiveCallBack = null;
    }

    public void setFinalNotifyDescriptor(BluetoothGattDescriptor finalNotifyDescriptor) {
        this.finalNotifyDescriptor = finalNotifyDescriptor;
    }

    public BluetoothGattDescriptor getFinalNotifyDescriptor() {
        return finalNotifyDescriptor;
    }
}
