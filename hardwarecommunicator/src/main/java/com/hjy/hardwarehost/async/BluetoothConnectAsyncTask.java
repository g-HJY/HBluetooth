package com.hjy.hardwarehost.async;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.hjy.hardwarehost.HBluetooth;
import com.hjy.hardwarehost.operator.abstra.Sender;
import com.hjy.hardwarehost.constant.BluetoothState;
import com.hjy.hardwarehost.inter.ConnectCallBack;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by _H_JY on 2018/10/22.
 * Connected thread for classic bluetooth.
 */
public class BluetoothConnectAsyncTask extends WeakAsyncTask<Void, Void, Integer, Context> {

    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private ConnectCallBack connectCallBack;
    private Context mContext;
    private Sender sender;
    private Handler handler;

    public BluetoothConnectAsyncTask(Context context, BluetoothDevice bluetoothDevice, ConnectCallBack connectCallBack) {
        super(context);
        this.bluetoothDevice = bluetoothDevice;
        this.mContext = context;
        this.connectCallBack = connectCallBack;
        try {
            Method method = null;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
                method = this.bluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[]{int.class});
            } else {
                method = this.bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            }
            this.bluetoothSocket = (BluetoothSocket) method.invoke(this.bluetoothDevice, 1);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Integer doInBackground(Context context,Void... voids) {

        if (bluetoothSocket != null) {

            if(handler == null){
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (connectCallBack != null) {
                        connectCallBack.onConnecting();
                    }
                }
            });

            int maxTries = 3;
            for (int i = 0; i < maxTries; i++) {
                try {
                    bluetoothSocket.connect();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            if (bluetoothSocket.isConnected()) {
                sender = HBluetooth.getInstance(mContext).sender();
                if(sender != null){
                    sender.initChannel(bluetoothSocket, BluetoothDevice.DEVICE_TYPE_CLASSIC);
                }
                return BluetoothState.CONNECT_SUCCESS;
            }

        }

        return BluetoothState.CONNECT_FAIL;
    }

    @Override
    protected void onPostExecute(Context context,Integer result) {
        super.onPostExecute(result);
        if (this.connectCallBack != null) {
            if (result == BluetoothState.CONNECT_SUCCESS) {

                try {
                    mContext.unregisterReceiver(mReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                mContext.registerReceiver(mReceiver, filter);
                this.connectCallBack.onConnected(sender);
            } else {
                this.connectCallBack.onError(BluetoothState.CONNECT_FAIL, "连接失败");
            }
        }
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                if (connectCallBack != null) {
                    connectCallBack.onDisConnected();
                }
            }
        }
    };


}
