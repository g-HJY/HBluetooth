package com.hjy.bluetooth.entity;

/**
 * author : HJY
 * date   : 2021/11/15
 * desc   :
 */
public class ScanFilter {

    private boolean fuzzyMatching;

    private String[] names;

    private ScanFilter(){}

    public ScanFilter(boolean fuzzyMatching, String... names) {
        this.fuzzyMatching = fuzzyMatching;
        this.names = names;
    }

    public boolean isFuzzyMatching() {
        return fuzzyMatching;
    }

    public String[] getNames() {
        return names;
    }
}
