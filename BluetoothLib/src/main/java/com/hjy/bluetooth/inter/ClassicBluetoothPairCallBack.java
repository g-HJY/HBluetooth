package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.exception.BluetoothException;

/**
 * author : HJY
 * date   : 2021/11/26
 * desc   :
 */
public interface ClassicBluetoothPairCallBack {

    void onPairSuccess();
    void onPairing();
    void onPairFailure(BluetoothException bluetoothException);
    void onPairRemoved();
}
