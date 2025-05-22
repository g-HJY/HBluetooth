package com.hjy.bluetooth.operator.abstra;

import com.hjy.bluetooth.entity.ScanFilter;
import com.hjy.bluetooth.inter.ScanCallBack;

/**
 * Created by _H_JY on 2018/10/20.
 */
public abstract class Scanner {

    private ScanFilter filter;

    public abstract void scan(int scanType, ScanCallBack scanCallBack);

    public abstract void scan(int scanType, int timeUse, ScanCallBack scanCallBack);

    public abstract void stopScan();

    public abstract void resetCallBack();

    public ScanFilter getFilter() {
        return filter;
    }

    public void setFilter(ScanFilter filter) {
        this.filter = filter;
    }


}
