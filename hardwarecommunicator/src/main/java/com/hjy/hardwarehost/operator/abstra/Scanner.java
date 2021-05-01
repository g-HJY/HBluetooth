package com.hjy.hardwarehost.operator.abstra;

import com.hjy.hardwarehost.inter.ScanCallBack;

/**
 * Created by _H_JY on 2018/10/20.
 */
public abstract class Scanner {

    public abstract void scan(int scanType,ScanCallBack scanCallBack);

    public abstract void stopScan();

}
