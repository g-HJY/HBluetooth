package com.hjy.bluetooth.operator.impl;

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

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.async.BluetoothConnectAsyncTask;
import com.hjy.bluetooth.constant.BluetoothState;
import com.hjy.bluetooth.exception.BleException;
import com.hjy.bluetooth.inter.BleMtuChangedCallback;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.SendCallBack;
import com.hjy.bluetooth.operator.abstra.Connector;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.utils.BleNotifier;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class BluetoothConnector extends Connector {

    private Context                   mContext;
    private BluetoothAdapter          bluetoothAdapter;
    private BluetoothConnectAsyncTask connectAsyncTask;
    private ConnectCallBack           connectCallBack;
    private BleNotifyCallBack         bleNotifyCallBack;
    private SendCallBack              sendCallBack;


    private BluetoothConnector() {
    }

    public BluetoothConnector(Context context, BluetoothAdapter bluetoothAdapter) {
        this.mContext = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }


    @Override
    public synchronized void connect(com.hjy.bluetooth.entity.BluetoothDevice device, final ConnectCallBack connectCallBack) {
        this.connectCallBack = connectCallBack;
        cancelConnectAsyncTask();
        HBluetooth hBluetooth = HBluetooth.getInstance(mContext);
        hBluetooth.destroyChannel();
        hBluetooth.cancelScan();

        final BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) { //Classic Bluetooth Type.
            if (remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) { //If no paired,register a broadcast to paired.
                /*Add automatic pairing*/
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
                                /*Paired successfully，interrupt broadcast*/
                                abortBroadcast();
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (connectCallBack != null) {
                                    connectCallBack.onError(BluetoothState.PAIRED_FAILED, "Automatic pairing failed, please pair manually.");
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

    @Override
    public void connect(com.hjy.bluetooth.entity.BluetoothDevice device, ConnectCallBack connectCallBack, BleNotifyCallBack notifyCallBack) {
        this.bleNotifyCallBack = notifyCallBack;
        connect(device, connectCallBack);
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
                HBluetooth hBluetooth = HBluetooth.getInstance(mContext);
                hBluetooth.setConnected(true);
                Sender sender = hBluetooth.sender();
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
                HBluetooth.getInstance(mContext).setConnected(false);
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

            if (status == BluetoothGatt.GATT_SUCCESS) {
                HBluetooth hBluetooth = HBluetooth.getInstance(mContext);
                HBluetooth.BleConfig bleConfig = hBluetooth.getBleConfig();
                int mtuSize = 0;
                String mainServiceUUID = null, writeCharacteristicUUID = null, notifyUUID = null;
                if (bleConfig != null) {
                    mtuSize = bleConfig.getMtuSize();
                    mainServiceUUID = bleConfig.getServiceUUID();
                    writeCharacteristicUUID = bleConfig.getWriteCharacteristicUUID();
                    notifyUUID = bleConfig.getNotifyCharacteristicUUID();
                }
                //At the software level, MTU setting is supported only when Android API version > = 21 (Android 5.0).
                //At the hardware level, only modules with Bluetooth 4.2 and above can support the setting of MTU.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (mtuSize > 23 && mtuSize < 512) {
                        gatt.requestMtu(mtuSize);
                    }
                }

                if (TextUtils.isEmpty(writeCharacteristicUUID)) {
                    writeCharacteristicUUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
                }

                if (!TextUtils.isEmpty(mainServiceUUID)) {
                    BluetoothGattService service = gatt.getService(UUID.fromString(mainServiceUUID));
                    if (service != null) {
                        BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(UUID.fromString(writeCharacteristicUUID));
                        if (writeCharacteristic != null) {
                            hBluetooth.sender().initSenderHelper(writeCharacteristic);
                        } else {
                            if (bleNotifyCallBack != null) {
                                bleNotifyCallBack.onNotifyFailure(new BleException("WriteCharacteristic is null,please check the writeCharacteristicUUID whether right"));
                            }
                        }
                        BleNotifier.openNotification(mContext, gatt, service, notifyUUID, writeCharacteristic, bleNotifyCallBack);
                    } else {
                        if (bleNotifyCallBack != null) {
                            bleNotifyCallBack.onNotifyFailure(new BleException("Main bluetoothGattService is null,please check the serviceUUID whether right"));
                        }
                    }
                } else {
                    List<BluetoothGattService> services = gatt.getServices();
                    if (services != null && services.size() > 0) {
                        for (int i = 0; i < services.size(); i++) {
                            List<BluetoothGattCharacteristic> characteristics = services.get(i).getCharacteristics();
                            if (characteristics != null && characteristics.size() > 0) {
                                for (int k = 0; k < characteristics.size(); k++) {
                                    BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(k);
                                    if (writeCharacteristicUUID.equals(bluetoothGattCharacteristic.getUuid().toString())) {
                                        HBluetooth.getInstance(mContext).sender().initSenderHelper(bluetoothGattCharacteristic);
                                        BleNotifier.openNotification(mContext, gatt, services.get(i), notifyUUID, bluetoothGattCharacteristic, bleNotifyCallBack);
                                    }
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
            HBluetooth.BleConfig bleConfig = HBluetooth.getInstance(mContext).getBleConfig();
            int mtuSize = 0;
            BleMtuChangedCallback callback = null;
            if (bleConfig != null) {
                mtuSize = bleConfig.getMtuSize();
                callback = bleConfig.getBleMtuChangedCallback();
            }

            if (callback != null) {
                if (BluetoothGatt.GATT_SUCCESS == status && mtuSize == mtu) {
                    callback.onMtuChanged(mtu);
                } else {
                    callback.onSetMTUFailure(mtu, new BleException("MTU change failed!"));
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
