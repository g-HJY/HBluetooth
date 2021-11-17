package com.hjy.bluetooth.utils;

import android.text.TextUtils;

import com.hjy.bluetooth.entity.ScanFilter;

/**
 * author : HJY
 * date   : 2021/11/15
 * desc   :
 */
public class ScanFilterUtils {

    public static boolean isInFilter(String deviceName, ScanFilter filter) {

        if (filter.getNames() == null) {
            return true;
        }

        boolean isAllFilterNameEmpty = true;
        for (String name : filter.getNames()) {
            if (!TextUtils.isEmpty(name)) {
                isAllFilterNameEmpty = false;
                break;
            }
        }

        if (isAllFilterNameEmpty) {
            return true;
        }


        if (TextUtils.isEmpty(deviceName)) {
            return false;
        }


        String[] names = filter.getNames();
        if (names != null && names.length > 0) {
            if (filter.isFuzzyMatching()) {
                for (String name : names) {
                    if (deviceName.contains(name)) {
                        return true;
                    }
                }
            } else {
                for (String name : names) {
                    if (name.equals(deviceName)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
