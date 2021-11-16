package com.hjy.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.IntDef;

import com.hjy.bluetooth.constant.ValueLimit;
import com.hjy.bluetooth.entity.ScanFilter;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.BleMtuChangedCallback;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.ReceiveCallBack;
import com.hjy.bluetooth.inter.ScanCallBack;
import com.hjy.bluetooth.inter.SendCallBack;
import com.hjy.bluetooth.operator.abstra.Connector;
import com.hjy.bluetooth.operator.abstra.Receiver;
import com.hjy.bluetooth.operator.abstra.Scanner;
import com.hjy.bluetooth.operator.abstra.Sender;
import com.hjy.bluetooth.operator.impl.BluetoothConnector;
import com.hjy.bluetooth.operator.impl.BluetoothReceiver;
import com.hjy.bluetooth.operator.impl.BluetoothScanner;
import com.hjy.bluetooth.operator.impl.BluetoothSender;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Created by _H_JY on 2018/10/20.
 */
public class HBluetooth {


    private static volatile HBluetooth mHBluetooth;

    private Context          mContext;
    private BluetoothAdapter mAdapter;
    private Scanner          scanner;
    private Connector        connector;
    private Sender           sender;
    private Receiver         receiver;
    private boolean          isConnected;
    private BleConfig        mBleConfig;

    private HBluetooth(Context context) {
        this.mContext = context;
    }

    public static HBluetooth getInstance() {
        if (mHBluetooth == null) {
            synchronized (HBluetooth.class) {
                if (mHBluetooth == null) {
                    mHBluetooth = new HBluetooth(getContext());
                }
            }
        }
        return mHBluetooth;
    }


    @IntDef({BluetoothDevice.DEVICE_TYPE_CLASSIC, BluetoothDevice.DEVICE_TYPE_LE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BluetoothType {
    }

    /**
     * You must call it first after initialize this class.
     *
     * @return
     */
    public HBluetooth enableBluetooth() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mAdapter == null) {
            throw new RuntimeException("Bluetooth unsupported!");
        }

        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        }

        scanner = new BluetoothScanner(mContext, mAdapter);
        connector = new BluetoothConnector(mContext, mAdapter);
        sender = new BluetoothSender();
        receiver = new BluetoothReceiver();

        return this;
    }


    public Set<BluetoothDevice> getBondedDevices() {
        return mAdapter == null ? null : mAdapter.getBondedDevices();
    }

    public void scan(@BluetoothType int scanType, ScanCallBack scanCallBack) {
        if (scanner != null) {
            scanner.scan(scanType, scanCallBack);
        }
    }

    public void scan(@BluetoothType int scanType, int timeUse, ScanCallBack scanCallBack) {
        if (scanner != null) {
            scanner.scan(scanType, timeUse, scanCallBack);
        }
    }

    /**
     * @param scanType
     * @param filter       Accurate or fuzzy matching scanning according to the device name
     * @param scanCallBack
     */
    public void scan(@BluetoothType int scanType, ScanFilter filter, ScanCallBack scanCallBack) {
        if (scanner != null) {
            scanner.setFilter(filter);
            scanner.scan(scanType, scanCallBack);
        }
    }

    /**
     * @param scanType
     * @param timeUse
     * @param filter       Accurate or fuzzy matching scanning according to the device name
     * @param scanCallBack
     */
    public void scan(@BluetoothType int scanType, int timeUse, ScanFilter filter, ScanCallBack scanCallBack) {
        if (scanner != null) {
            scanner.setFilter(filter);
            scanner.scan(scanType, timeUse, scanCallBack);
        }
    }

    public Scanner scanner() {
        checkIfEnableBluetoothFirst();
        return scanner;
    }

    public synchronized void cancelScan() {
        if (scanner != null) {
            scanner.stopScan();
        }
    }


    public synchronized void destroyChannel() {
        if (sender != null) {
            sender.destroyChannel();
        }
    }

    private void resetCallBack() {
        if (sender != null) {
            sender.resetCallBack();
        }
        if (scanner != null) {
            scanner.resetCallBack();
        }
    }


    public void connect(com.hjy.bluetooth.entity.BluetoothDevice bluetoothDevice, ConnectCallBack connectCallBack) {
        if (connector != null) {
            connector.connect(bluetoothDevice, connectCallBack);
        }
    }

    public void connect(com.hjy.bluetooth.entity.BluetoothDevice bluetoothDevice, ConnectCallBack connectCallBack, BleNotifyCallBack bleNotifyCallBack) {
        if (connector != null) {
            connector.connect(bluetoothDevice, connectCallBack, bleNotifyCallBack);
        }
    }

    public Connector connector() {
        checkIfEnableBluetoothFirst();
        return connector;
    }

    public void send(byte[] cmd, SendCallBack sendCallBack) {
        if (sender != null) {
            sender.send(cmd, sendCallBack);
        }
    }

    public Sender sender() {
        checkIfEnableBluetoothFirst();
        return sender;
    }

    public Receiver setReceiver(ReceiveCallBack receiveCallBack) {
        if (receiver != null) {
            receiver.setReceiveCallBack(receiveCallBack);
        }
        return receiver;
    }

    public Receiver receiver() {
        checkIfEnableBluetoothFirst();
        return receiver;
    }

    private void checkIfEnableBluetoothFirst() {
        if (mAdapter == null || !mAdapter.isEnabled()) {
            throw new RuntimeException("you must call enableBluetooth() first.");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }


    public BleConfig getBleConfig() {
        return mBleConfig;
    }

    public void setBleConfig(BleConfig bleConfig) {
        mBleConfig = bleConfig;
    }


    /**
     * The config only for ble
     */
    public static class BleConfig {
        private String serviceUUID, writeCharacteristicUUID, notifyCharacteristicUUID;
        private boolean               useCharacteristicDescriptor;
        private int                   mtuSize;
        private int                   sendTimeInterval   = 20;
        private int                   eachSplitPacketLen = 20;
        private int                   notifyDelay        = 200;
        private boolean               splitPacketToSendWhenCmdLenBeyond;
        private BleMtuChangedCallback mBleMtuChangedCallback;

        public BleConfig withServiceUUID(String serviceUUID) {
            this.serviceUUID = serviceUUID;
            return this;
        }

        public BleConfig withWriteCharacteristicUUID(String writeCharacteristicUUID) {
            this.writeCharacteristicUUID = writeCharacteristicUUID;
            return this;
        }

        public BleConfig withNotifyCharacteristicUUID(String notifyCharacteristicUUID) {
            this.notifyCharacteristicUUID = notifyCharacteristicUUID;
            return this;
        }

        public BleConfig useCharacteristicDescriptor(boolean useCharacteristicDescriptor) {
            this.useCharacteristicDescriptor = useCharacteristicDescriptor;
            return this;
        }

        /**
         * default value = 200ms
         *
         * @param millisecond
         * @return
         */
        public BleConfig notifyDelay(int millisecond) {
            this.notifyDelay = millisecond;
            return this;
        }

        /**
         * @param splitPacketToSendWhenCmdLenBeyond default value = false
         * @param sendTimeInterval                  unit is ms,default value = 20ms
         *                                          The time interval of subcontracting sending shall not be less than 20ms to avoid sending failure
         *                                          The default length of each subcontract is 20 bytes
         * @return
         */
        public BleConfig splitPacketToSendWhenCmdLenBeyond(boolean splitPacketToSendWhenCmdLenBeyond, int sendTimeInterval) {
            this.splitPacketToSendWhenCmdLenBeyond = splitPacketToSendWhenCmdLenBeyond;
            this.sendTimeInterval = sendTimeInterval;
            return this;
        }

        public BleConfig splitPacketToSendWhenCmdLenBeyond(boolean splitPacketToSendWhenCmdLenBeyond, int sendTimeInterval, int eachSplitPacketLen) {
            this.splitPacketToSendWhenCmdLenBeyond = splitPacketToSendWhenCmdLenBeyond;
            this.sendTimeInterval = sendTimeInterval;
            this.eachSplitPacketLen = eachSplitPacketLen;
            return this;
        }

        /**
         * @param splitPacketToSendWhenCmdLenBeyond default value = false
         *                                          sendTimeInterval's unit is ms,default value = 20ms
         *                                          The default length of each subcontract is 20 bytes
         * @return
         */
        public BleConfig splitPacketToSendWhenCmdLenBeyond(boolean splitPacketToSendWhenCmdLenBeyond) {
            this.splitPacketToSendWhenCmdLenBeyond = splitPacketToSendWhenCmdLenBeyond;
            return this;
        }

        /**
         * set Mtu
         *
         * @param mtuSize
         * @param callback
         */
        public BleConfig setMtu(int mtuSize, BleMtuChangedCallback callback) {
            if (callback == null) {
                throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
            }

            if (mtuSize > ValueLimit.DEFAULT_MAX_MTU) {
                callback.onSetMTUFailure(mtuSize, new BluetoothException("requiredMtu should lower than 512 !"));
            }

            if (mtuSize < ValueLimit.DEFAULT_MTU) {
                callback.onSetMTUFailure(mtuSize, new BluetoothException("requiredMtu should higher than 23 !"));
            }

            this.mtuSize = mtuSize;
            mBleMtuChangedCallback = callback;
            return this;
        }

        public boolean isSplitPacketToSendWhenCmdLenBeyond() {
            return splitPacketToSendWhenCmdLenBeyond;
        }

        public int getSendTimeInterval() {
            return sendTimeInterval;
        }

        public int getEachSplitPacketLen() {
            return eachSplitPacketLen;
        }

        public String getServiceUUID() {
            return serviceUUID;
        }

        public String getWriteCharacteristicUUID() {
            return writeCharacteristicUUID;
        }

        public String getNotifyCharacteristicUUID() {
            return notifyCharacteristicUUID;
        }

        public boolean isUseCharacteristicDescriptor() {
            return useCharacteristicDescriptor;
        }

        public int getMtuSize() {
            return mtuSize;
        }

        public int getNotifyDelay() {
            return notifyDelay;
        }

        public BleMtuChangedCallback getBleMtuChangedCallback() {
            return mBleMtuChangedCallback;
        }
    }


    public synchronized void release() {
        cancelScan();
        destroyChannel();
        resetCallBack();
    }


    private static Context APPLICATION_CONTEXT;

    /**
     * 初始化context，如果由于不同机型导致反射获取context失败可以在Application调用此方法
     */
    public static void init(Context context) {
        APPLICATION_CONTEXT = context;
    }

    /**
     * 反射获取 application context
     */
    public static Context getContext() {
        if (null == APPLICATION_CONTEXT) {
            try {
                Application application = (Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null, (Object[]) null);
                if (application != null) {
                    APPLICATION_CONTEXT = application;
                    return application;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Application application = (Application) Class.forName("android.app.AppGlobals")
                        .getMethod("getInitialApplication")
                        .invoke(null, (Object[]) null);
                if (application != null) {
                    APPLICATION_CONTEXT = application;
                    return application;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            throw new IllegalStateException("ContextHolder is not initialed, it is recommend to init with application context.");
        }
        return APPLICATION_CONTEXT;
    }


}
