package com.hjy.hardwarehost.inter;

import com.hjy.hardwarehost.entity.BluetoothDevice;

import java.util.List;

/**
 * Created by _H_JY on 2018/10/20.
 */
public interface ScanCallBack {

    void onScanStart();

    void onScanning(List<BluetoothDevice> scannedDevices,BluetoothDevice currentScannedDevice);

    void onError(int errorType,String errorMsg);

    void onScanFinished(List<BluetoothDevice> bluetoothDevices);

}
