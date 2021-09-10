package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.exception.BleException;

/**
 * author : HJY
 * date   : 2021/9/9
 * desc   :
 */
public interface BleMtuChangedCallback {

      void onSetMTUFailure(int realMtuSize,BleException bleException);

      void onMtuChanged(int mtuSize);

}
