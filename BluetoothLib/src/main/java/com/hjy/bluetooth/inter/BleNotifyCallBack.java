package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.exception.BleException;

/**
 * author : HJY
 * date   : 2021/11/10
 * desc   :
 */
public interface BleNotifyCallBack {

    void onNotifySuccess();

    void onNotifyFailure(BleException bleException);

}
