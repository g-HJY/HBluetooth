package com.hjy.bluetooth.operator.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;

import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.inter.ScanCallBack;
import com.hjy.bluetooth.operator.abstra.Scanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class BluetoothScanner extends Scanner {

    private int                   scanType;
    private boolean isScanning;
    private Context               mContext;
    private ScanCallBack          scanCallBack;
    private BluetoothLeScanner    bluetoothLeScanner;
    private List<BluetoothDevice> bluetoothDevices;
    private BluetoothAdapter      bluetoothAdapter;
    private Handler               handler;

    public BluetoothScanner() {
    }


    public BluetoothScanner(Context context, BluetoothAdapter bluetoothAdapter) {
        this.mContext = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }


    @Override
    public synchronized void scan(int scanType, ScanCallBack scanCallBack) {
        startScan(scanType, 0, scanCallBack);
    }

    @Override
    public void scan(int scanType, int timeUse, ScanCallBack scanCallBack) {
        startScan(scanType, timeUse, scanCallBack);
    }


    private void startScan(int scanType, int timeUse, ScanCallBack scanCallBack) {
        this.scanType = scanType;
        this.scanCallBack = scanCallBack;

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (this.scanCallBack != null) {
                this.scanCallBack.onError(1, "Only system versions above Android 4.3 are supported.");
            }
            return;
        }

        if (this.scanType == BluetoothDevice.DEVICE_TYPE_LE && !mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if (this.scanCallBack != null) {
                this.scanCallBack.onError(2, "Your device does not support low-power Bluetooth.");
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
            // Register for broadcasts when discovery has finished
            IntentFilter filter = new IntentFilter();
            filter.addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);

            // If we're already discovering, stop it
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            isScanning = true;
            bluetoothAdapter.startDiscovery();
        } else if (this.scanType == BluetoothDevice.DEVICE_TYPE_LE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //After 5.0 use BluetoothLeScanner to scan
                //Because bluetoothAdapter.startLeScan deprecated
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                isScanning = true;
                bluetoothLeScanner.startScan(mScanCallback);
            } else {
                isScanning = true;
                bluetoothAdapter.startLeScan(mLeScanCallBack);
            }
        }

        //Auto stop when time out
        if (timeUse != 0) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, timeUse);
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
                    scanCallBack.onScanning(bluetoothDevices, bluetoothDevice);
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (scanCallBack != null) {
                    scanCallBack.onScanFinished(bluetoothDevices);
                }
                isScanning = false;
            }
        }
    };


    //Ble scan callback before 5.0
    private BluetoothAdapter.LeScanCallback mLeScanCallBack = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(android.bluetooth.BluetoothDevice bluetoothDevice, int i, byte[] bytes) {

            final BluetoothDevice device = new BluetoothDevice();
            device.setName(bluetoothDevice.getName());
            device.setAddress(bluetoothDevice.getAddress());
            device.setType(BluetoothDevice.DEVICE_TYPE_LE);
            device.setScanRecord(bytes);

            if (bluetoothDevices.contains(device)) {
                int index = bluetoothDevices.indexOf(device);
                bluetoothDevices.set(index, device);
            } else {
                bluetoothDevices.add(device);
            }


            if (scanCallBack != null) {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanCallBack.onScanning(bluetoothDevices,device);
                    }
                });
            }
        }
    };


    //Ble scan callback after 5.0
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            android.bluetooth.BluetoothDevice bluetoothDevice = result.getDevice();
            final BluetoothDevice device = new BluetoothDevice();
            device.setName(bluetoothDevice.getName());
            device.setAddress(bluetoothDevice.getAddress());
            device.setType(BluetoothDevice.DEVICE_TYPE_LE);
            if (result.getScanRecord() != null) {
                device.setScanRecord(result.getScanRecord().getBytes());
            }

            if (bluetoothDevices.contains(device)) {
                int index = bluetoothDevices.indexOf(device);
                bluetoothDevices.set(index, device);
            } else {
                bluetoothDevices.add(device);
            }


            if (scanCallBack != null) {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanCallBack.onScanning(bluetoothDevices,device);
                    }
                });
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            if (scanCallBack != null) {
                scanCallBack.onError(errorCode, "Scan Failed!");
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(mScanCallback);
            } else {
                bluetoothAdapter.stopLeScan(mLeScanCallBack);
            }
        }

        if(isScanning){
            if (scanCallBack != null) {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        scanCallBack.onScanFinished(bluetoothDevices);
                    }
                });
            }
            isScanning = false;
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
