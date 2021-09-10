package com.hjy.bluetooth.inter;

import com.hjy.bluetooth.entity.BluetoothDevice;

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
