package com.hjy.hardwarehost.operator.impl;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import com.hjy.hardwarehost.operator.abstra.Scanner;
import com.hjy.hardwarehost.entity.BluetoothDevice;
import com.hjy.hardwarehost.inter.ScanCallBack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class BluetoothScanner extends Scanner {

    private int scanType;
    private Context mContext;
    private ScanCallBack scanCallBack;
    private List<BluetoothDevice> bluetoothDevices;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;

    public BluetoothScanner() {
    }


    public BluetoothScanner(Context context, BluetoothAdapter bluetoothAdapter) {
        this.mContext = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }


    @Override
    public synchronized void scan(int scanType, ScanCallBack scanCallBack) {
        this.scanType = scanType;
        this.scanCallBack = scanCallBack;

        if (android.os.Build.VERSION.SDK_INT < 18) {
            if (this.scanCallBack != null) {
                this.scanCallBack.onError(1, "只支持Android 4.3以上的系统版本");
            }
            return;
        }

        if (this.scanType == BluetoothDevice.DEVICE_TYPE_LE && !mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if (this.scanCallBack != null) {
                this.scanCallBack.onError(2, "您的设备不支持低功耗蓝牙功能");
            }
            return;
        }

        if (bluetoothDevices == null) {
            bluetoothDevices = new ArrayList<>();
        } else if (bluetoothDevices.size() > 0) {
            bluetoothDevices.clear();
        }


        if (this.scanCallBack != null) {
            this.scanCallBack.onScanStart();
        }

        if (this.scanType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {

            unregisterReceiver();

            // Register for broadcasts when a device is discovered
            IntentFilter filter = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(mReceiver, filter);
            // Register for broadcasts when discovery has finished
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);
            // If we're already discovering, stop it
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        } else if (this.scanType == BluetoothDevice.DEVICE_TYPE_LE) {
            bluetoothAdapter.startLeScan(mLeScanCallBack);
        }
    }

    //CLASSIC BLUETOOTH
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // When discovery finds a device
            if (android.bluetooth.BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                android.bluetooth.BluetoothDevice device = intent
                        .getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);
                // new device found
                BluetoothDevice bluetoothDevice = new BluetoothDevice();
                if (device.getBondState() != android.bluetooth.BluetoothDevice.BOND_BONDED) {
                    bluetoothDevice.setPaired(false);
                } else {
                    bluetoothDevice.setPaired(true);
                }

                bluetoothDevice.setAddress(device.getAddress());
                bluetoothDevice.setName(device.getName());
                bluetoothDevice.setType(device.getType());


                if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
                    if (bluetoothDevices.contains(bluetoothDevice)) {
                        return;
                    }
                }
                bluetoothDevices.add(bluetoothDevice);

                if (scanCallBack != null) {
                    scanCallBack.onScanning(bluetoothDevices,bluetoothDevice);
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (scanCallBack != null) {
                    scanCallBack.onScanFinished(bluetoothDevices);
                }
            }
        }
    };


    //Ble搜索回调
    private BluetoothAdapter.LeScanCallback mLeScanCallBack = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(android.bluetooth.BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

            BluetoothDevice device = new BluetoothDevice();
            device.setName(bluetoothDevice.getName());
            device.setAddress(bluetoothDevice.getAddress());
            device.setType(BluetoothDevice.DEVICE_TYPE_LE);
            device.setScanRecord(bytes);

            if (bluetoothDevices.contains(device)) {
                int index = bluetoothDevices.indexOf(device);
                bluetoothDevices.set(index,device);
            }else {
                bluetoothDevices.add(device);
            }


            if (scanCallBack != null) {
                if(handler == null){
                   handler = new Handler(Looper.getMainLooper());
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanCallBack.onScanFinished(bluetoothDevices);
                    }
                });
            }
        }
    };


    @Override
    public synchronized void stopScan() {
        if (scanType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            unregisterReceiver();
        } else if (scanType == BluetoothDevice.DEVICE_TYPE_LE) {
            bluetoothAdapter.stopLeScan(mLeScanCallBack);
        }
    }


    private void unregisterReceiver() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
