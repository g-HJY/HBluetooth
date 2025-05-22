package com.hjy.bluetooth.utils;

import android.bluetooth.BluetoothGattCharacteristic;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.operator.abstra.Receiver;

import java.io.DataInputStream;

/**
 * author : HJY
 * date   : 2021/11/17
 * desc   :
 */
public class ReceiveHolder {

    public static void receiveBleReturnData(BluetoothGattCharacteristic characteristic) {
        byte[] result = characteristic.getValue();
        Receiver receiver = HBluetooth.getInstance().receiver();
        if (receiver != null && receiver.getReceiveCallBack() != null) {
            receiver.getReceiveCallBack().onReceived(null, result);
        }
    }

    public static void receiveClassicBluetoothReturnData(DataInputStream dis, byte[] result) {
        Receiver receiver = HBluetooth.getInstance().receiver();
        if (receiver != null && receiver.getReceiveCallBack() != null) {
            receiver.getReceiveCallBack().onReceived(dis, result);
        }
    }
}
