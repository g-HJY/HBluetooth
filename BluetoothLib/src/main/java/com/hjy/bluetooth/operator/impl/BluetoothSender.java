package com.hjy.bluetooth.operator.impl;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothSocket;

import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.SendCallBack;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.utils.LockStore;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by _H_JY on 2018/10/22.
 */
public class BluetoothSender extends Sender {

    private              BluetoothSocket             mSocket;
    private              BluetoothGatt               mGatt;
    private              BluetoothGattCharacteristic characteristic;
    private              BluetoothConnector          connector;
    private              ConnectCallBack             connectCallBack;
    private              int                         type;
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
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
                if(connectCallBack != null){
                    connectCallBack.onDisConnected();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mGatt != null) {
            //will go to onConnectionStateChange()，and call gatt.close() to release
            mGatt.disconnect();
        }

    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    @Override
    public void send(final byte[] command, final SendCallBack sendCallBack) {
        if (LockStore.getLock(LOCK_NAME)) {
            if (mSocket != null && type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                new Thread(new Runnable() {
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
                                sendCallBack.onSending();
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


                            if (sendCallBack != null) {
                                sendCallBack.onReceived(dis, result);
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            LockStore.releaseLock(LOCK_NAME);
                        }
                    }
                }).start();
            } else if (mGatt != null && characteristic != null && type == BluetoothDevice.DEVICE_TYPE_LE) {
                if (connector != null && sendCallBack != null) {
                    connector.setSendCallBack(sendCallBack);
                }
                characteristic.setValue(command);
                if (sendCallBack != null) {
                    sendCallBack.onSending();
                }
                mGatt.writeCharacteristic(characteristic);
                LockStore.releaseLock(LOCK_NAME);
            } else {
                LockStore.releaseLock(LOCK_NAME);
            }
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
