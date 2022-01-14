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
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.async.ClassicBluetoothConnectTask;
import com.hjy.bluetooth.constant.BluetoothState;
import com.hjy.bluetooth.constant.ClassicBluetoothPairMode;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.BleMtuChangedCallback;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ClassicBluetoothPairCallBack;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.operator.abstra.Connector;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.utils.BleNotifier;
import com.hjy.bluetooth.utils.ReceiveHolder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

/**
 * Created by _H_JY on 2018/10/20.
 */

public class BluetoothConnector extends Connector {

    private              Context                                  mContext;
    private              BluetoothAdapter                         bluetoothAdapter;
    private              ClassicBluetoothConnectTask              connectAsyncTask;
    private              ConnectCallBack                          connectCallBack;
    private              BleNotifyCallBack                        bleNotifyCallBack;
    private              ClassicBluetoothPairCallBack             pairCallBack;
    private              Handler                                  handler;
    private              Map<String, Boolean>                     timeOutDeviceMap;
    private              com.hjy.bluetooth.entity.BluetoothDevice device;
    private              long                                     lastCheckReconnectTime    = 0L;
    //The interval between two reconnection detection shall not be less than 2000ms
    private static final int                                      FAST_RECONNECT_DELAY_TIME = 2000;

    private BluetoothConnector() {
    }

    public BluetoothConnector(Context context, BluetoothAdapter bluetoothAdapter) {
        this.mContext = context;
        this.bluetoothAdapter = bluetoothAdapter;
    }


    @Override
    public synchronized void connect(com.hjy.bluetooth.entity.BluetoothDevice device, final ConnectCallBack connectCallBack) {
        this.connectCallBack = connectCallBack;
        this.device = device;
        cancelConnectAsyncTask();
        final HBluetooth hBluetooth = HBluetooth.getInstance();
        hBluetooth.destroyChannel();
        hBluetooth.cancelScan();

        final BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(device.getAddress());

        if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) { //Classic Bluetooth Type.
            int pairMode = ClassicBluetoothPairMode.JUST_WORK;
            String pinCode = "1234";
            HBluetooth.ClassicBluetoothConfig config = hBluetooth.getClassicBluetoothConfig();
            if (config != null) {
                pairMode = config.getPairMode();
                pinCode = config.getPinCode();
            }

            if (pairMode == ClassicBluetoothPairMode.PIN_CODE &&
                    remoteDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                //If no paired,register a broadcast to paired.
                //Add automatic pairing
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

                final String finalPinCode = pinCode;
                mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
                            try {
                                abortBroadcast();
                                byte[] pin = (byte[]) BluetoothDevice.class.getMethod("convertPinToBytes", String.class).invoke(BluetoothDevice.class, finalPinCode);
                                Method m = remoteDevice.getClass().getMethod("setPin", byte[].class);
                                m.invoke(remoteDevice, pin);
                                remoteDevice.getClass().getMethod("setPairingConfirmation", boolean.class).invoke(remoteDevice, true);
                                System.out.println("PAIRED !");

                                //Already bound,start connection thread
                                initializeRelatedNullVariable();
                                connectAsyncTask = new ClassicBluetoothConnectTask(mContext, handler,
                                        timeOutDeviceMap, remoteDevice, connectCallBack);
                                connectAsyncTask.execute();
                            } catch (Exception e) {
                                if (pairCallBack != null) {
                                    pairCallBack.onPairFailure(new BluetoothException("Pair failure," + (e.getMessage() == null ? e.getCause() : e.getMessage())));
                                }
                                if (connectCallBack != null) {
                                    connectCallBack.onError(BluetoothState.PAIRED_FAILED, "Automatic pairing failed, please pair manually.");
                                }
                            }
                        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction()) && pairCallBack != null) {
                            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            int bondSate = bluetoothDevice.getBondState();
                            switch (bondSate) {
                                case BluetoothDevice.BOND_NONE:
                                    pairCallBack.onPairRemoved();
                                    break;

                                case BluetoothDevice.BOND_BONDING:
                                    pairCallBack.onPairing();
                                    break;

                                case BluetoothDevice.BOND_BONDED:
                                    pairCallBack.onPairSuccess();
                                    break;
                            }
                        }
                    }
                }, filter);

                //When it is lower than Android API 19, the pairing method is a hidden method, so it can only be implemented through the reflection method.
                try {
                    Method creMethod = BluetoothDevice.class.getMethod("createBond");
                    creMethod.invoke(remoteDevice);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

            } else {
                //start connection thread
                initializeRelatedNullVariable();
                connectAsyncTask = new ClassicBluetoothConnectTask(mContext, handler,
                        timeOutDeviceMap, remoteDevice, this.connectCallBack);
                connectAsyncTask.execute();
            }


        } else if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) { //BLE Type.
            //Get related config of connection
            HBluetooth.BleConfig bleConfig = hBluetooth.getBleConfig();
            boolean autoConnect = false;
            if (bleConfig != null) {
                autoConnect = bleConfig.isAutoConnect();
            }

            //If the connection timeout is set, enable timeout detection
            int connectTimeOut = hBluetooth.getConnectTimeOut();
            if (connectTimeOut > 0) {
                //You have set connectTimeOut and value is right
                initializeRelatedNullVariable();
                timeOutDeviceMap.put(remoteDevice.getAddress(), false);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!hBluetooth.isConnected() && !hBluetooth.isUserActiveDisconnect()) {
                            timeOutDeviceMap.put(remoteDevice.getAddress(), true);
                            hBluetooth.releaseIgnoreActiveDisconnect();
                            if (connectCallBack != null) {
                                connectCallBack.onError(BluetoothState.CONNECT_TIMEOUT, "Connect time out");
                            }
                        }
                    }
                }, connectTimeOut);
            }

            //Ble connection,for systems above 6.0, the transmission mode adopts transport_ LE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                remoteDevice.connectGatt(mContext, autoConnect, bluetoothGattCallback, TRANSPORT_LE);
            } else {
                remoteDevice.connectGatt(mContext, autoConnect, bluetoothGattCallback);
            }
        }
    }

    private void initializeRelatedNullVariable() {
        if (timeOutDeviceMap == null) {
            timeOutDeviceMap = new HashMap<>();
        }
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
    }

    @Override
    public void connect(com.hjy.bluetooth.entity.BluetoothDevice device, ConnectCallBack connectCallBack, BleNotifyCallBack notifyCallBack) {
        this.bleNotifyCallBack = notifyCallBack;
        connect(device, connectCallBack);
    }

    public void connect(com.hjy.bluetooth.entity.BluetoothDevice device, ClassicBluetoothPairCallBack classicBluetoothPairCallBack, ConnectCallBack connectCallBack) {
        this.pairCallBack = classicBluetoothPairCallBack;
        connect(device, connectCallBack);
    }


    protected void cancelConnectAsyncTask() {
        if (connectAsyncTask != null && connectAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            connectAsyncTask.cancel(true);
            connectAsyncTask = null;
        }
    }


    /**
     * Reconnect if the reconnection is supported
     *
     * @param gatt
     */
    private void checkBleReconnect(BluetoothGatt gatt) {
        if (System.currentTimeMillis() - lastCheckReconnectTime >= FAST_RECONNECT_DELAY_TIME) {
            lastCheckReconnectTime = System.currentTimeMillis();
            HBluetooth hBluetooth = HBluetooth.getInstance();
            int reconnectTimes = hBluetooth.getReconnectTryTimes();
            int retryTimes = getRetryTimes();
            if (reconnectTimes > 0 && !hBluetooth.isUserActiveDisconnect() && retryTimes < reconnectTimes) {
                setRetryTimes(++retryTimes);
                //Log.e("mylog", "Try reconnecting->" + retryTimes);
                initializeRelatedNullVariable();
                if (timeOutDeviceMap.containsKey(gatt.getDevice().getAddress())) {
                    timeOutDeviceMap.remove(gatt.getDevice().getAddress());
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (device != null) {
                            connect(device, connectCallBack, bleNotifyCallBack);
                        }
                    }
                }, hBluetooth.getReconnectInterval());
            } else {
                //Clear listener
                connectCallBack = null;
                bleNotifyCallBack = null;
            }
        }
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            //Only devices connected without timeout need callback processing, because the timeout has been handled separately
            if (timeOutDeviceMap == null || !timeOutDeviceMap.get(gatt.getDevice().getAddress())) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    HBluetooth hBluetooth = HBluetooth.getInstance();
                    hBluetooth.setConnected(true);
                    hBluetooth.setUserActiveDisconnect(false);
                    setRetryTimes(0);
                    Sender sender = hBluetooth.sender();
                    if (sender != null) {
                        BluetoothSender bluetoothSender = (BluetoothSender) sender;
                        bluetoothSender.setConnector(BluetoothConnector.this).initChannel(gatt, BluetoothDevice.DEVICE_TYPE_LE, connectCallBack);
                        bluetoothSender.discoverServices();
                    }

                    if (connectCallBack != null) {
                        connectCallBack.onConnected(sender);
                    }

                } else if (newState == BluetoothProfile.STATE_CONNECTING && connectCallBack != null) {
                    connectCallBack.onConnecting();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    HBluetooth.getInstance().setConnected(false);
                    gatt.close();

                    if (connectCallBack != null) {
                        connectCallBack.onDisConnected();
                    }

                    //If it is a passive disconnection and the reconnection mechanism is enabled, reconnect when disconnected
                    checkBleReconnect(gatt);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTING && connectCallBack != null) {
                    connectCallBack.onDisConnecting();
                }
            } else {
                //Remove the time out record of this device
                timeOutDeviceMap.remove(gatt.getDevice().getAddress());
                checkBleReconnect(gatt);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                HBluetooth hBluetooth = HBluetooth.getInstance();
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mtuSize > 23 && mtuSize < 512) {
                    if (!gatt.requestMtu(mtuSize) && bleConfig.getBleMtuChangedCallback() != null) {
                        bleConfig.getBleMtuChangedCallback().onSetMTUFailure(-1, new BluetoothException("Gatt requestMtu failed"));
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
                                bleNotifyCallBack.onNotifyFailure(new BluetoothException("WriteCharacteristic is null,please check the writeCharacteristicUUID whether right"));
                            }
                        }
                        BleNotifier.openNotification(gatt, service, notifyUUID, writeCharacteristic, bleNotifyCallBack);
                    } else {
                        if (bleNotifyCallBack != null) {
                            bleNotifyCallBack.onNotifyFailure(new BluetoothException("Main bluetoothGattService is null,please check the serviceUUID whether right"));
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
                                        hBluetooth.sender().initSenderHelper(bluetoothGattCharacteristic);
                                        BleNotifier.openNotification(gatt, services.get(i), notifyUUID, bluetoothGattCharacteristic, bleNotifyCallBack);
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
            HBluetooth.BleConfig bleConfig = HBluetooth.getInstance().getBleConfig();
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
                    callback.onSetMTUFailure(mtu, new BluetoothException("MTU change warning! Real size of MTU is " + mtu));
                }
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                ReceiveHolder.receiveBleReturnData(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            ReceiveHolder.receiveBleReturnData(characteristic);
        }
    };

}
