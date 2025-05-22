package com.hjy.bluetooth.operator.impl;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.SendCallBack;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.utils.ArrayUtils;
import com.hjy.bluetooth.utils.BleNotifier;
import com.hjy.bluetooth.utils.LockStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by _H_JY on 2018/10/22.
 */
public class BluetoothSender extends Sender {

    private              Handler                     handler   = new Handler(Looper.getMainLooper());
    private              BluetoothSocket             mSocket;
    private              BluetoothGatt               mGatt;
    private              BluetoothGattCharacteristic characteristic;
    private              BluetoothConnector          connector;
    private              ConnectCallBack             connectCallBack;
    private              SendCallBack                sendCallBack;
    private              int                         type;
    private              HBluetooth.BleConfig        mBleConfig;
    private final static String                      LOCK_NAME = "SendCmdLock";


    public BluetoothSocket getSocket() {
        return mSocket;
    }

    public void setSocket(BluetoothSocket socket) {
        this.mSocket = socket;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.mGatt = gatt;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public BluetoothSender setConnector(BluetoothConnector connector) {
        this.connector = connector;
        return this;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    @Override
    public void discoverServices() {
        if (mGatt != null) {
            mGatt.discoverServices();
        }
    }

    @Override
    public <G> G initSenderHelper(G g) {
        if (g instanceof BluetoothGattCharacteristic) {
            return (G) (characteristic = (BluetoothGattCharacteristic) g);
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public synchronized void destroyChannel() {
        if (connector != null) {
            connector.cancelConnectAsyncTask();
        }
        //Classic bluetooth disconnect
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
                HBluetooth.getInstance().setConnected(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Ble disconnect
        if (mGatt != null) {
            //Close ble notification
            BleNotifier.closeNotification();
            //Will go to onConnectionStateChange()，and call gatt.close() to release
            mGatt.disconnect();
            refreshGattCache();
        }

    }

    @Override
    public void resetCallBack() {
        sendCallBack = null;
    }

    private synchronized void refreshGattCache() {
        try {
            final Method method = BluetoothGatt.class.getMethod("refresh");
            if (method != null && mGatt != null) {
                method.invoke(mGatt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    private void sendFailCallBack(final String failMsg) {
        if (sendCallBack != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                sendCallBack.onSendFailure(new BluetoothException(failMsg));
            } else {
                //Call back on UI thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallBack.onSendFailure(new BluetoothException(failMsg));
                    }
                });
            }
        }
    }

    private void sendingCallBack(final byte[] command) {
        if (sendCallBack != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                sendCallBack.onSending(command);
            } else {
                //Call back on UI thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendCallBack.onSending(command);
                    }
                });
            }
        }
    }

    @Override
    public void send(final byte[] command, final SendCallBack sendCallBack) {
        this.sendCallBack = sendCallBack;
        if (LockStore.getLock(LOCK_NAME)) {
            if (mSocket != null && type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                //Classic bluetooth send command
                AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Clean input stream before write.
                            InputStream is = mSocket.getInputStream();
                            int r = 1;
                            while (r > 0) {
                                r = is.available();
                                if (r > 0) {
                                    byte[] b = new byte[r];
                                    is.read(b, 0, r);
                                }
                            }

                            Thread.sleep(50);

                            sendingCallBack(command);

                            //Send command.
                            OutputStream os = mSocket.getOutputStream();
                            os.write(command);
                            os.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                            sendFailCallBack("Bluetooth socket write IOException."+e.getMessage());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            sendFailCallBack("Bluetooth socket write InterruptedException"+e.getMessage());
                        } finally {
                            LockStore.releaseLock(LOCK_NAME);
                        }
                    }
                });
            } else if (mGatt != null && characteristic != null && type == BluetoothDevice.DEVICE_TYPE_LE) {

                //When the packet length exceeds 20, it needs to be sent by subcontracting
                if (mBleConfig == null) {
                    mBleConfig = HBluetooth.getInstance().getBleConfig();
                }
                boolean splitPacketToSendWhenCmdLenBeyond = false;
                int sendTimeInterval = 0, splitLen = 20;
                String serviceUUID = null, writeUUID = null;
                if (mBleConfig != null) {
                    splitPacketToSendWhenCmdLenBeyond = mBleConfig.isSplitPacketToSendWhenCmdLenBeyond();
                    sendTimeInterval = mBleConfig.getSendTimeInterval();
                    splitLen = mBleConfig.getEachSplitPacketLen();
                    serviceUUID = mBleConfig.getServiceUUID();
                    writeUUID = mBleConfig.getWriteCharacteristicUUID();
                }


                if (splitPacketToSendWhenCmdLenBeyond && command.length > splitLen) {
                    //Split packet to send
                    Object[] objects = ArrayUtils.splitBytes(command, splitLen);
                    for (Object object : objects) {
                        byte[] onceCmd = (byte[]) object;
                        bleSendCommand(onceCmd, serviceUUID, writeUUID);
                        try {
                            Thread.sleep(sendTimeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    //If not set splitPacketWhenCmdLenBeyond=true on BleConfig,you need to set mtu when you want to send commands longer than 20
                    bleSendCommand(command, serviceUUID, writeUUID);
                }
                LockStore.releaseLock(LOCK_NAME);
            } else {
                LockStore.releaseLock(LOCK_NAME);
            }
        }

    }


    /**
     * Ble send command
     *
     * @param command
     * @param serviceUUID
     * @param writeUUID
     */
    private void bleSendCommand(byte[] command, String serviceUUID, String writeUUID) {
        //Instead, get the characteristic before sending the command every time
        BluetoothGattService service = mGatt.getService(UUID.fromString(serviceUUID));
        if (service != null) {
            characteristic = service.getCharacteristic(UUID.fromString(writeUUID));

            if (characteristic == null) {
                sendFailCallBack("The WriteCharacteristic is null, please check your writeUUID whether right");
                return;
            }

            //Check whether can write
            if ((characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                sendFailCallBack("This characteristic not support write");
                return;
            }

            characteristic.setValue(command);
            sendingCallBack(command);
            if (!mGatt.writeCharacteristic(characteristic)) {
                sendFailCallBack("Gatt writeCharacteristic fail, please check command or change the value of sendTimeInterval if you have set it");
            }
        } else {
            sendFailCallBack("Main bluetoothGattService is null,please check the serviceUUID whether right");
        }

    }

    @Override
    public <T> T initChannel(T o, int type, ConnectCallBack connectCallBack) {
        this.connectCallBack = connectCallBack;
        if (o instanceof BluetoothSocket) {
            this.type = BluetoothDevice.DEVICE_TYPE_CLASSIC;
            return (T) (mSocket = (BluetoothSocket) o);
        } else if (o instanceof BluetoothGatt) {
            this.type = BluetoothDevice.DEVICE_TYPE_LE;
            return (T) (mGatt = (BluetoothGatt) o);
        }
        return null;
    }


    @Override
    public void readCharacteristic(String serviceUUID, String characteristicUUID, SendCallBack sendCallBack) {
        this.sendCallBack = sendCallBack;
        if (mGatt != null) {
            BluetoothGattService service = mGatt.getService(UUID.fromString(serviceUUID));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));

                if (characteristic == null) {
                    sendFailCallBack("This Characteristic is null, please check the characteristicUUID whether right");
                    return;
                }

                //Check whether can read
                if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
                    sendFailCallBack("This characteristic not support read");
                    return;
                }

                if (!mGatt.readCharacteristic(characteristic)) {
                    sendFailCallBack("Gatt readCharacteristic fail");
                }

            } else {
                sendFailCallBack("BluetoothGattService is null,please check the serviceUUID whether right");
            }
        } else {
            sendFailCallBack("BluetoothGatt is null");
        }
    }

}
