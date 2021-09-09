package com.hjy.hardwarehost.operator.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.hjy.hardwarehost.HBluetooth;
import com.hjy.hardwarehost.async.BluetoothConnectAsyncTask;
import com.hjy.hardwarehost.constant.BluetoothState;
import com.hjy.hardwarehost.exception.BleException;
import com.hjy.hardwarehost.inter.BleMtuChangedCallback;
import com.hjy.hardwarehost.inter.ConnectCallBack;
import com.hjy.hardwarehost.inter.SendCallBack;
import com.hjy.hardwarehost.operator.abstra.Connector;
import com.hjy.hardwarehost.operator.abstra.Sender;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class BluetoothConnector extends Connector {

    private Context                   mContext;
    private BluetoothAdapter          bluetoothAdapter;
    private BluetoothConnectAsyncTask connectAsyncTask;
    private ConnectCallBack           connectCallBack;
    private SendCallBack              sendCallBack;


    public BluetoothConnector() {
    }

    public BluetoothConnector(Context context, BluetoothAdapter bluetoothAdapter) {
        this.mContext = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }


    @Override
    public synchronized void connect(com.hjy.hardwarehost.entity.BluetoothDevice device, final ConnectCallBack connectCallBack) {
        this.connectCallBack = connectCallBack;
        cancelConnectAsyncTask();
        HBluetooth hBluetooth = HBluetooth.getInstance(mContext);
        hBluetooth.destroyChannel();
        hBluetooth.cancelScan();

        final BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) { //Classic Bluetooth Type.
            if (remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) { //If no paired,register a broadcast to paired.
                /*增加自动配对功能*/
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {

                            try {
                                byte[] pin = (byte[]) BluetoothDevice.class.getMethod("convertPinToBytes", String.class).invoke(BluetoothDevice.class, "1234");
                                Method m = remoteDevice.getClass().getMethod("setPin", byte[].class);
                                m.invoke(remoteDevice, pin);
                                remoteDevice.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(remoteDevice, true);
                                System.out.println("PAIRED !");
                                //context.unregisterReceiver(this);
                                /*配对成功，中断广播的继续传递*/
                                abortBroadcast();
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (connectCallBack != null) {
                                    connectCallBack.onError(BluetoothState.PAIRED_FAILED, "自动配对失败，请手动配对");
                                }
                            }
                        }
                    }
                }, filter);
            }

            connectAsyncTask = new BluetoothConnectAsyncTask(mContext, remoteDevice, this.connectCallBack);
            connectAsyncTask.execute();

        } else if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) { //BLE Type.

            remoteDevice.connectGatt(mContext, false, bluetoothGattCallback);
        }
    }


    public void setSendCallBack(SendCallBack sendCallBack) {
        this.sendCallBack = sendCallBack;
    }

    protected void cancelConnectAsyncTask() {
        if (connectAsyncTask != null && connectAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            connectAsyncTask.cancel(true);
            connectAsyncTask = null;
        }
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Sender sender = HBluetooth.getInstance(mContext).sender();
                if (sender != null) {
                    BluetoothSender bluetoothSender = (BluetoothSender) sender;
                    bluetoothSender.setConnector(BluetoothConnector.this).initChannel(gatt, BluetoothDevice.DEVICE_TYPE_LE, connectCallBack);
                    bluetoothSender.discoverServices();
                }

                if (connectCallBack != null) {
                    connectCallBack.onConnected(sender);
                }

            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                if (connectCallBack != null) {
                    connectCallBack.onConnecting();
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (gatt != null) {
                    gatt.close();
                }
                if (connectCallBack != null) {
                    connectCallBack.onDisConnected();
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                if (connectCallBack != null) {
                    connectCallBack.onDisConnecting();
                }

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            //软件层面，Android API版本>=21（Android 5.0），才支持设置MTU。
            //硬件层面，蓝牙4.2及以上的模块，才支持设置MTU。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int mtuSize = HBluetooth.getInstance(mContext).getMtuSize();
                if (mtuSize > 23 && mtuSize < 512) {
                    gatt.requestMtu(mtuSize);
                }
            }

            String writeCharacteristicUUID = HBluetooth.getInstance(mContext).getWriteCharacteristicUUID();
            if (TextUtils.isEmpty(writeCharacteristicUUID)) {
                writeCharacteristicUUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                if (services != null && services.size() > 0) {
                    for (int i = 0; i < services.size(); i++) {
                        List<BluetoothGattCharacteristic> characteristics = services.get(i).getCharacteristics();
                        if (characteristics != null && characteristics.size() > 0) {
                            for (int k = 0; k < characteristics.size(); k++) {
                                BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(k);
                                if (writeCharacteristicUUID.equals(bluetoothGattCharacteristic.getUuid().toString())) {
                                    HBluetooth.getInstance(mContext).sender().initSenderHelper(bluetoothGattCharacteristic);
                                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            int mtuSize = HBluetooth.getInstance(mContext).getMtuSize();
            BleMtuChangedCallback callback = HBluetooth.getInstance(mContext).getBleMtuChangedCallback();
            if (callback != null) {
                if (BluetoothGatt.GATT_SUCCESS == status && mtuSize == mtu) {
                    callback.onMtuChanged();
                } else {
                    callback.onSetMTUFailure(mtu, new BleException("MTU change fail!"));
                }
            }


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] result = characteristic.getValue();
            if (sendCallBack != null) {
                sendCallBack.onReceived(null, result);
            }
        }
    };


}
