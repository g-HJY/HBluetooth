package com.hjy.bluetooth.operator.impl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.SendCallBack;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.utils.ArrayUtils;
import com.hjy.bluetooth.utils.BleNotifier;
import com.hjy.bluetooth.utils.LockStore;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by _H_JY on 2018/10/22.
 */
public class BluetoothSender extends Sender {

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
                if (connectCallBack != null) {
                    connectCallBack.onDisConnected();
                    connectCallBack = null;
                }
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

                            if (sendCallBack != null) {
                                sendCallBack.onSending(command);
                            }

                            //Send command.
                            OutputStream os = mSocket.getOutputStream();
                            os.write(command);
                            os.flush();

                            //Get return's data.
                            DataInputStream dis = new DataInputStream(mSocket.getInputStream());
                            byte[] buffer = new byte[1024];
                            int size = mSocket.getInputStream().read(buffer);
                            byte[] result = new byte[size];
                            System.arraycopy(buffer, 0, result, 0, size);


                            BluetoothReceiver receiver = (BluetoothReceiver) HBluetooth.getInstance().receiver();
                            if (receiver != null && receiver.getReceiveCallBack() != null) {
                                receiver.getReceiveCallBack().onReceived(dis, result);
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                            if (sendCallBack != null) {
                                sendCallBack.onSendFailure(new BluetoothException("Bluetooth socket write IOException"));
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            if (sendCallBack != null) {
                                sendCallBack.onSendFailure(new BluetoothException("Bluetooth socket write InterruptedException"));
                            }
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
                        bleSendCommand(onceCmd, serviceUUID, writeUUID, sendCallBack);
                        try {
                            Thread.sleep(sendTimeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    //If not set splitPacketWhenCmdLenBeyond=true on BleConfig,you need to set mtu when you want to send commands longer than 20
                    bleSendCommand(command, serviceUUID, writeUUID, sendCallBack);
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
     * @param sendCallBack
     */
    private void bleSendCommand(byte[] command, String serviceUUID, String writeUUID, SendCallBack sendCallBack) {
        //Instead, get the characteristic before sending the command every time
        BluetoothGattService service = mGatt.getService(UUID.fromString(serviceUUID));
        if (service != null) {
            characteristic = service.getCharacteristic(UUID.fromString(writeUUID));

            //Check whether can write
            if (characteristic == null
                    || (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
                if (sendCallBack != null)
                    sendCallBack.onSendFailure(new BluetoothException("This characteristic not support write"));
                return;
            }

            characteristic.setValue(command);
            if (sendCallBack != null) {
                sendCallBack.onSending(command);
            }
            if (!mGatt.writeCharacteristic(characteristic) && sendCallBack != null) {
                sendCallBack.onSendFailure(new BluetoothException("Gatt writeCharacteristic fail, please check command or change the value of sendTimeInterval if you have set it"));
            }
        } else if (sendCallBack != null) {
            sendCallBack.onSendFailure(new BluetoothException("Main bluetoothGattService is null,please check the serviceUUID whether right"));
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

}
