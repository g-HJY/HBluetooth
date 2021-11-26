package com.hjy.bluetooth.operator.impl;

import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothSocket;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.inter.ReceiveCallBack;
import com.hjy.bluetooth.operator.abstra.Receiver;
import com.hjy.bluetooth.utils.ReceiveHolder;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author : HJY
 * date   : 2021/11/12
 * desc   :
 */
public class BluetoothReceiver extends Receiver {

    private ReceiveCallBack receiveCallBack;
    private BluetoothGattDescriptor finalNotifyDescriptor;
    private ExecutorService executorService;

    @Override
    public void setReceiveCallBack(ReceiveCallBack receiveCallBack) {
        this.receiveCallBack = receiveCallBack;
    }

    @Override
    public ReceiveCallBack getReceiveCallBack() {
        return this.receiveCallBack;
    }

    public void setFinalNotifyDescriptor(BluetoothGattDescriptor finalNotifyDescriptor) {
        this.finalNotifyDescriptor = finalNotifyDescriptor;
    }

    public BluetoothGattDescriptor getFinalNotifyDescriptor() {
        return finalNotifyDescriptor;
    }

    public void openClassicBluetoothReceiveThread(final BluetoothSocket bluetoothSocket){
        if (executorService == null){
            executorService =  Executors.newSingleThreadExecutor();
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (HBluetooth.getInstance().isConnected()){
                    try {
                        //Get return's data of classic bluetooth.
                        DataInputStream dis = new DataInputStream(bluetoothSocket.getInputStream());
                        byte[] buffer = new byte[1024];
                        int size = bluetoothSocket.getInputStream().read(buffer);
                        byte[] result = new byte[size];
                        System.arraycopy(buffer, 0, result, 0, size);

                        ReceiveHolder.receiveClassicBluetoothReturnData(dis, result);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    public void closeClassicBluetoothReceiveThread(){
        if(executorService != null && !executorService.isShutdown()){
            executorService.shutdown();
            executorService = null;
        }
    }

}
