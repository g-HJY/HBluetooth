package com.hjy.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.IntDef;

import com.hjy.bluetooth.constant.ValueLimit;
import com.hjy.bluetooth.exception.BleException;
import com.hjy.bluetooth.inter.BleMtuChangedCallback;
import com.hjy.bluetooth.inter.ScanCallBack;
import com.hjy.bluetooth.operator.abstra.Connector;
import com.hjy.bluetooth.operator.abstra.Scanner;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.operator.impl.BluetoothConnector;
import com.hjy.bluetooth.operator.impl.BluetoothScanner;
import com.hjy.bluetooth.operator.impl.BluetoothSender;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class HBluetooth {


    private static volatile HBluetooth mHBluetooth;

    private Context               mContext;
    private BluetoothAdapter      mAdapter;
    private Scanner               scanner;
    private Connector             connector;
    private Sender                sender;
    private int                   mtuSize;
    private BleMtuChangedCallback mBleMtuChangedCallback;
    private String                writeCharacteristicUUID;

    private HBluetooth(Context context) {
        this.mContext = context;
    }


    public static HBluetooth getInstance(Context context) {
        if (mHBluetooth == null) {
            synchronized (HBluetooth.class) {
                if (mHBluetooth == null) {
                    mHBluetooth = new HBluetooth(context.getApplicationContext());
                }
            }
        }
        return mHBluetooth;
    }


    @IntDef({BluetoothDevice.DEVICE_TYPE_CLASSIC, BluetoothDevice.DEVICE_TYPE_LE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BluetoothType {
    }

    /**
     * You must call it first after initialize this class.
     *
     * @return
     */
    public HBluetooth enableBluetooth() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mAdapter == null) {
            throw new RuntimeException("Bluetooth unsupported!");
        }

        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        }

        scanner = new BluetoothScanner(mContext, mAdapter);
        connector = new BluetoothConnector(mContext, mAdapter);
        sender = new BluetoothSender();

        return this;
    }


    public Set<BluetoothDevice> getBondedDevices() {
        return mAdapter == null ? null : mAdapter.getBondedDevices();
    }

    public void scan(@BluetoothType int scanType, ScanCallBack scanCallBack) {
        if (scanner != null) {
            scanner.scan(scanType, scanCallBack);
        }
    }


    public Scanner scanner() {
        if (mAdapter == null || !mAdapter.isEnabled()) {
            throw new RuntimeException("you must call enableBluetooth() first.");
        }
        return scanner;
    }


    public synchronized void cancelScan() {
        if (scanner != null) {
            scanner.stopScan();
        }
    }


    public synchronized void destroyChannel() {
        if (sender != null) {
            sender.destroyChannel();
        }
    }


    public Connector connector() {
        if (mAdapter == null || !mAdapter.isEnabled()) {
            throw new RuntimeException("you must call enableBluetooth() first.");
        }
        return connector;
    }


    public Sender sender() {
        if (mAdapter == null || !mAdapter.isEnabled()) {
            throw new RuntimeException("you must call enableBluetooth() first.");
        }
        return sender;
    }

    public String getWriteCharacteristicUUID() {
        return writeCharacteristicUUID;
    }

    public HBluetooth setWriteCharacteristicUUID(String writeCharacteristicUUID) {
        this.writeCharacteristicUUID = writeCharacteristicUUID;
        return this;
    }

    /**
     * set Mtu
     *
     * @param mtuSize
     * @param callback
     */
    public HBluetooth setMtu(int mtuSize,
                             BleMtuChangedCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
        }

        if (mtuSize > ValueLimit.DEFAULT_MAX_MTU) {
            callback.onSetMTUFailure(mtuSize, new BleException("requiredMtu should lower than 512 !"));
        }

        if (mtuSize < ValueLimit.DEFAULT_MTU) {
            callback.onSetMTUFailure(mtuSize, new BleException("requiredMtu should higher than 23 !"));
        }

        this.mtuSize = mtuSize;
        mBleMtuChangedCallback = callback;
        return this;
    }


    public BleMtuChangedCallback getBleMtuChangedCallback() {
        return mBleMtuChangedCallback;
    }


    public int getMtuSize() {
        return mtuSize;
    }


    public synchronized void release() {
        cancelScan();
        destroyChannel();
    }


}
