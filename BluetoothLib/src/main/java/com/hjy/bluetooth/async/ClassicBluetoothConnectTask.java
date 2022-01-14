package com.hjy.bluetooth.async;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.constant.BluetoothState;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.operator.abstra.Receiver;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.operator.impl.BluetoothReceiver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by _H_JY on 2018/10/22.
 * Connected thread for classic bluetooth.
 */
public class ClassicBluetoothConnectTask extends WeakAsyncTask<Void, Void, Integer, Context> {

    private              BluetoothSocket      bluetoothSocket;
    private              BluetoothDevice      bluetoothDevice;
    private              ConnectCallBack      connectCallBack;
    private              Context              mContext;
    private              Sender               sender;
    private              Handler              handler;
    private              Map<String, Boolean> timeOutDeviceMap;
    private              String               failMsg;
    private              long                 lastCheckReconnectTime    = 0L;
    //The interval between two reconnection detection shall not be less than 2000ms
    private static final int                  FAST_RECONNECT_DELAY_TIME = 2000;

    public ClassicBluetoothConnectTask(Context context, Handler handler, Map<String, Boolean> timeOutDeviceMap, BluetoothDevice bluetoothDevice, ConnectCallBack connectCallBack) {
        super(context);
        this.bluetoothDevice = bluetoothDevice;
        this.mContext = context;
        this.handler = handler;
        this.timeOutDeviceMap = timeOutDeviceMap;
        this.connectCallBack = connectCallBack;

        try {
            //If the socket is not released before connecting, release it first
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }

            Method method;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                //Establish an insecure Bluetooth connection. If pairing is not performed, the pairing box will not pop up
                method = this.bluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[]{int.class});
            } else {
                method = this.bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            }

            this.bluetoothSocket = (BluetoothSocket) method.invoke(this.bluetoothDevice, 1);

        } catch (NoSuchMethodException e) {
            failMsg = e.getMessage();
        } catch (IllegalAccessException e) {
            failMsg = e.getMessage();
        } catch (InvocationTargetException e) {
            failMsg = e.getMessage();
        } catch (IOException e) {
            failMsg = e.getMessage();
        }

    }


    @Override
    protected Integer doInBackground(Context context, Void... voids) {

        if (bluetoothSocket != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (connectCallBack != null) {
                        connectCallBack.onConnecting();
                    }
                }
            });

            //If the connection timeout is set, enable timeout detection
            final HBluetooth hBluetooth = HBluetooth.getInstance();
            int connectTimeOut = hBluetooth.getConnectTimeOut();
            Runnable timeOutRunnable = null;
            if (connectTimeOut > 0) {
                timeOutDeviceMap.put(bluetoothDevice.getAddress(), false);
                timeOutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!hBluetooth.isConnected()) {
                            timeOutDeviceMap.put(bluetoothDevice.getAddress(), true);
                            hBluetooth.releaseIgnoreActiveDisconnect();
                            if (connectCallBack != null) {
                                connectCallBack.onError(BluetoothState.CONNECT_TIMEOUT, "Connect failed! Reason: Time out");
                            }
                        }
                    }
                };
                handler.postDelayed(timeOutRunnable, connectTimeOut);
            }


            int maxTries = 3;
            for (int i = 0; i < maxTries; i++) {
                try {
                    bluetoothSocket.connect();
                    break;
                } catch (IOException e) {
                    failMsg = e.getMessage();
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            //Remove the listener of time out
            if (timeOutRunnable != null) {
                handler.removeCallbacks(timeOutRunnable);
            }

            if (!timeOutDeviceMap.get(bluetoothDevice.getAddress())) {
                hBluetooth.setConnected(bluetoothSocket.isConnected());
                if (bluetoothSocket.isConnected()) {
                    sender = hBluetooth.sender();
                    if (sender != null) {
                        sender.initChannel(bluetoothSocket, BluetoothDevice.DEVICE_TYPE_CLASSIC, connectCallBack);
                    }
                    return BluetoothState.CONNECT_SUCCESS;
                }
            } else {
                hBluetooth.setConnected(false);
                timeOutDeviceMap.remove(bluetoothDevice.getAddress());
                return BluetoothState.CONNECT_TIMEOUT;
            }
        }

        return BluetoothState.CONNECT_FAIL;
    }

    @Override
    protected void onPostExecute(Context context, Integer result) {
        if (this.connectCallBack != null) {
            if (result == BluetoothState.CONNECT_SUCCESS) {
                closeDisconnectionBroadcastReceiver();
                //Register to disconnect broadcast listening
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                mContext.registerReceiver(mReceiver, filter);

                //After connected, reset the parameters related to reconnection
                HBluetooth hBluetooth = HBluetooth.getInstance();
                hBluetooth.setUserActiveDisconnect(false);
                if (hBluetooth.connector() != null) {
                    hBluetooth.connector().setRetryTimes(0);
                }
                if (hBluetooth.receiver() != null) {
                    BluetoothReceiver bluetoothReceiver = (BluetoothReceiver) hBluetooth.receiver();
                    bluetoothReceiver.openClassicBluetoothReceiveThread(bluetoothSocket);
                }
                this.connectCallBack.onConnected(sender);
            } else if (result == BluetoothState.CONNECT_FAIL) {
                this.connectCallBack.onError(BluetoothState.CONNECT_FAIL, "Connect failed! Reason:" + failMsg);
                checkClassicBluetoothReconnect();
            } else {
                //If it is a passive disconnection and the reconnection mechanism is enabled, reconnect when disconnected
                checkClassicBluetoothReconnect();
            }
        }
    }


    private void closeDisconnectionBroadcastReceiver() {
        try {
            mContext.unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                HBluetooth.getInstance().setConnected(false);
                //Close the receiver
                Receiver receiver = HBluetooth.getInstance().receiver();
                if (receiver != null) {
                    BluetoothReceiver bluetoothReceiver = (BluetoothReceiver) receiver;
                    bluetoothReceiver.closeClassicBluetoothReceiveThread();
                }
                if (connectCallBack != null) {
                    connectCallBack.onDisConnected();
                }

                closeDisconnectionBroadcastReceiver();

                //If it is a passive disconnection and the reconnection mechanism is enabled, reconnect when disconnected
                checkClassicBluetoothReconnect();
            }
        }
    };

    /**
     * Reconnect if the reconnection is supported
     */
    private void checkClassicBluetoothReconnect() {
        if (System.currentTimeMillis() - lastCheckReconnectTime >= FAST_RECONNECT_DELAY_TIME) {
            lastCheckReconnectTime = System.currentTimeMillis();
            final HBluetooth hBluetooth = HBluetooth.getInstance();
            int reconnectTimes = hBluetooth.getReconnectTryTimes();
            if (hBluetooth.connector() != null) {
                int retryTimes = hBluetooth.connector().getRetryTimes();
                if (reconnectTimes > 0 && !hBluetooth.isUserActiveDisconnect() && retryTimes < reconnectTimes) {
                    hBluetooth.connector().setRetryTimes(++retryTimes);
                    //Log.e("mylog", "Try reconnecting->" + retryTimes);
                    if (timeOutDeviceMap.containsKey(bluetoothDevice.getAddress())) {
                        timeOutDeviceMap.remove(bluetoothDevice.getAddress());
                    }
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (bluetoothDevice != null) {
                                hBluetooth.connector().connect(adapt(bluetoothDevice), connectCallBack);
                            }
                        }
                    }, hBluetooth.getReconnectInterval());
                }
            }
        }
    }


    private com.hjy.bluetooth.entity.BluetoothDevice adapt(BluetoothDevice bluetoothDevice) {
        com.hjy.bluetooth.entity.BluetoothDevice device = new com.hjy.bluetooth.entity.BluetoothDevice();
        device.setName(bluetoothDevice.getName());
        device.setType(device.getType());
        device.setPaired(bluetoothDevice.getBondState() == android.bluetooth.BluetoothDevice.BOND_BONDED);
        device.setAddress(bluetoothDevice.getAddress());
        return device;
    }


}
