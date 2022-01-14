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

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.inter.ScanCallBack;
import com.hjy.bluetooth.operator.abstra.Scanner;
import com.hjy.bluetooth.utils.ScanFilterUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class BluetoothScanner extends Scanner {

    private int scanType, continuousScanTimes;
    private              boolean               isScanning;
    private              Context               mContext;
    private              ScanCallBack          scanCallBack;
    private              BluetoothLeScanner    bluetoothLeScanner;
    private              List<BluetoothDevice> bluetoothDevices;
    private              BluetoothAdapter      bluetoothAdapter;
    private              Handler               handler;
    private              boolean               liveUpdateScannedDeviceName;
    private              long                  lastCheckPeriodScanNumLimitTime  = 0L;
    private static final int                   PERIOD_SCAN_NUM_LIMIT_DELAY_TIME = 30 * 1000;

    private BluetoothScanner() {
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
    public synchronized void scan(int scanType, int timeUse, ScanCallBack scanCallBack) {
        startScan(scanType, timeUse, scanCallBack);
    }


    private void startScan(int scanType, int timeUse, ScanCallBack scanCallBack) {
        //Important, If we're already discovering or scanning, stop it first!
        isScanning = false;
        stopScan();

        HBluetooth.BleConfig bleConfig = HBluetooth.getInstance().getBleConfig();
        if (bleConfig != null) {
            this.liveUpdateScannedDeviceName = bleConfig.isLiveUpdateScannedDeviceName();
        }

        this.scanType = scanType;
        this.scanCallBack = scanCallBack;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
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

        //Clear data before scan
        if (bluetoothDevices == null) {
            bluetoothDevices = new ArrayList<>();
        } else if (bluetoothDevices.size() > 0) {
            bluetoothDevices.clear();
        }

        if (this.scanCallBack != null) {
            this.scanCallBack.onScanStart();
        }

        if (this.scanType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            // Register for broadcasts when a device is discovered
            // Register for broadcasts when discovery has finished
            IntentFilter filter = new IntentFilter();
            filter.addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);

            if (!(isScanning = bluetoothAdapter.startDiscovery()) && this.scanCallBack != null) {
                this.scanCallBack.onError(3, "Start discovery fail,make sure you have Bluetooth enabled or open permissions");
            }

        } else if (this.scanType == BluetoothDevice.DEVICE_TYPE_LE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //After 5.0 use BluetoothLeScanner to scan
                //Because bluetoothAdapter.startLeScan deprecated
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                if (bluetoothLeScanner != null) {
                    isScanning = true;

                    //Since Android 7.0 does not allow 5 consecutive scans within 30s, otherwise any device cannot be scanned
                    //Once this limit is exceeded, a prompt is given
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        if (continuousScanTimes == 0) {
                            lastCheckPeriodScanNumLimitTime = System.currentTimeMillis();
                        }
                        continuousScanTimes++;
                        long checkPeriodScanNumTimeDiff = System.currentTimeMillis() - lastCheckPeriodScanNumLimitTime;
                        if (checkPeriodScanNumTimeDiff > PERIOD_SCAN_NUM_LIMIT_DELAY_TIME) {
                            continuousScanTimes = 0;
                            lastCheckPeriodScanNumLimitTime = System.currentTimeMillis();
                        } else if (this.scanCallBack != null && continuousScanTimes > 5) {
                            this.scanCallBack.onError(3, "Forbidden,please do not scan more than 5 times in 30s");
                        }
                    }

                    bluetoothLeScanner.startScan(mScanCallback);
                } else if (this.scanCallBack != null) {
                    this.scanCallBack.onError(3, "BluetoothLeScanner is null,make sure you have Bluetooth enabled or open permissions");
                }

            } else if (!(isScanning = bluetoothAdapter.startLeScan(mLeScanCallBack)) && this.scanCallBack != null) {
                this.scanCallBack.onError(3, "StartLeScan fail,make sure you have Bluetooth enabled or open permissions");
            }
        }

        //Auto stop scan when time out
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
    // The BroadcastReceiver that listens for discovered devices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // When discovery finds a device
            if (android.bluetooth.BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                android.bluetooth.BluetoothDevice device = intent
                        .getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE);

                //If there is filtering, filter the scanning results
                if (getFilter() != null && !ScanFilterUtils.isInFilter(device.getName(), getFilter())) {
                    return;
                }

                int rssi = intent.getShortExtra(android.bluetooth.BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // new device found
                BluetoothDevice bluetoothDevice = new BluetoothDevice();
                bluetoothDevice.setPaired(device.getBondState() == android.bluetooth.BluetoothDevice.BOND_BONDED);
                bluetoothDevice.setAddress(device.getAddress());
                bluetoothDevice.setName(device.getName());
                bluetoothDevice.setType(device.getType());
                bluetoothDevice.setRssi(rssi);


                if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
                    if (bluetoothDevices.contains(bluetoothDevice)) {
                        return;
                    }
                }
                bluetoothDevices.add(bluetoothDevice);

                if (scanCallBack != null) {
                    scanCallBack.onScanning(bluetoothDevices, bluetoothDevice);
                }

                // When discovery is finished, complete scan and call back
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
        public void onLeScan(android.bluetooth.BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            handleBleScanResult(bluetoothDevice, rssi, bytes);
        }
    };


    //Ble scan callback after 5.0
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            android.bluetooth.BluetoothDevice bluetoothDevice = result.getDevice();

            handleBleScanResult(bluetoothDevice, result.getRssi(),
                    result.getScanRecord() == null ? null : result.getScanRecord().getBytes());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            if (scanCallBack != null) {
                scanCallBack.onError(errorCode, "Scan failed!");
            }
        }
    };


    private void handleBleScanResult(android.bluetooth.BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        //If you need to get real-time device name,we can parse it from scanRecord.
        //Otherwise, you will get cached data, not real-time data
        String deviceName;
        if (liveUpdateScannedDeviceName) {
            deviceName = ScanFilterUtils.parseDeviceName(scanRecord);
        } else {
            deviceName = bluetoothDevice.getName();
        }

        //If there is filtering, filter the scanning results
        if (getFilter() != null && !ScanFilterUtils.isInFilter(deviceName, getFilter())) {
            return;
        }

        final BluetoothDevice device = new BluetoothDevice();
        device.setName(deviceName);
        device.setAddress(bluetoothDevice.getAddress());
        device.setType(BluetoothDevice.DEVICE_TYPE_LE);
        device.setPaired(bluetoothDevice.getBondState() == android.bluetooth.BluetoothDevice.BOND_BONDED);
        device.setRssi(rssi);
        device.setScanRecord(scanRecord);

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
                    scanCallBack.onScanning(bluetoothDevices, device);
                }
            });
        }
    }

    @Override
    public synchronized void stopScan() {
        if (scanType == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            unregisterReceiver();
        } else if (scanType == BluetoothDevice.DEVICE_TYPE_LE && bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(mScanCallback);
            } else {
                bluetoothAdapter.stopLeScan(mLeScanCallBack);
            }
        }

        if (isScanning) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (scanCallBack != null) {
                        scanCallBack.onScanFinished(bluetoothDevices);
                    }
                }
            });
            isScanning = false;
        }
    }

    @Override
    public void resetCallBack() {
        scanCallBack = null;
    }

    private void unregisterReceiver() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
