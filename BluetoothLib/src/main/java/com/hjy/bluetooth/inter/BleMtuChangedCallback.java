package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.exception.BluetoothException;

/**
 * author : HJY
 * date   : 2021/9/9
 * desc   :
 */
public interface BleMtuChangedCallback {

      void onSetMTUFailure(int realMtuSize, BluetoothException bleException);

      void onMtuChanged(int mtuSize);

}
