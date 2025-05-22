package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.exception.BluetoothException;

/**
 * Created by _H_JY on 2018/10/24.
 */

public interface SendCallBack {

    void onSending(byte[] command);

    void onSendFailure(BluetoothException bleException);
}
