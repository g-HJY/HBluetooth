package com.hjy.hardwarehost.inter;

import com.hjy.hardwarehost.exception.BleException;

/**
 * author : HJY
 * date   : 2021/9/9
 * desc   :
 */
public interface BleMtuChangedCallback {

      void onSetMTUFailure(int realMtuSize,BleException bleException);

      void onMtuChanged();

}
