package com.hjy.bluetooth.operator.abstra;

import com.hjy.bluetooth.inter.ReceiveCallBack;


/**
 * author : HJY
 * date   : 2021/11/12
 * desc   :
 */
public abstract class Receiver {
    public abstract void setReceiveCallBack(ReceiveCallBack receiveCallBack);
    public abstract ReceiveCallBack getReceiveCallBack();
}
