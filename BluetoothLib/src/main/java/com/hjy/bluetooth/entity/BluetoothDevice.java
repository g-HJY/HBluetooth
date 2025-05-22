package com.hjy.bluetooth.entity;

import java.util.Arrays;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class BluetoothDevice {

    public static final int DEVICE_TYPE_CLASSIC = android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC;

    public static final int DEVICE_TYPE_LE = android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE;

    private String name;

    private int type;

    private String address;

    private boolean paired;

    private byte[] scanRecord;

    private int rssi;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isPaired() {
        return paired;
    }

    public void setPaired(boolean paired) {
        this.paired = paired;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(byte[] scanRecord) {
        this.scanRecord = scanRecord;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean equals(Object obj) {
        if (obj instanceof BluetoothDevice) {
            BluetoothDevice d = (BluetoothDevice) obj;
            return this.address.equals(d.address);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return "BluetoothDevice{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", address='" + address + '\'' +
                ", paired=" + paired +
                ", scanRecord=" + Arrays.toString(scanRecord) +
                ", rssi=" + rssi +
                '}';
    }
}
