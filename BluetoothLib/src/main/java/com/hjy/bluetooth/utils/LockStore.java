package com.hjy.bluetooth.utils;


import android.text.TextUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 公用内存锁仓库.实现非阻塞型的同步锁，区别于synchronized
 */

public final class LockStore {

    // volatile保证所有线程看到的锁相同
    private static volatile Map<String, Date> locks = new HashMap<String, Date>();

    private LockStore() {

    }

    /**
     * 根据锁名获取锁
     *
     * @param lockName 锁名
     * @return 是否锁定成功
     */
    public synchronized static Boolean getLock(String lockName) {
        Boolean locked = false;

        if (TextUtils.isEmpty(lockName)) {
            throw new RuntimeException("Lock name can't be empty");
        }

        Date lockDate = locks.get(lockName);
        if (lockDate == null) {
            locks.put(lockName, new Date());
            locked = true;
        }

        return locked;
    }

    /**
     * 根据锁名释放锁
     *
     * @param lockName 锁名
     */
    public synchronized static void releaseLock(String lockName) {
        if (TextUtils.isEmpty(lockName)) {
            throw new RuntimeException("Lock name can't be empty");
        }

        Date lockDate = locks.get(lockName);
        if (lockDate != null) {
            locks.remove(lockName);
        }
    }

    /**
     * 获取上次成功锁定的时间
     *
     * @param lockName 锁名
     * @return 如果还没有锁定返回NULL
     */
    public synchronized static Date getLockDate(String lockName) {
        if (TextUtils.isEmpty(lockName)) {
            throw new RuntimeException("Lock name can't be empty");
        }

        Date lockDate = locks.get(lockName);

        return lockDate;
    }

}

