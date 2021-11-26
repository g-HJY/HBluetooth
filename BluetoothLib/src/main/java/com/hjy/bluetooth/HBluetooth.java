package com.hjy.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.IntDef;

import com.hjy.bluetooth.constant.ClassicBluetoothPairMode;
import com.hjy.bluetooth.constant.ValueLimit;
import com.hjy.bluetooth.entity.ScanFilter;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.BleMtuChangedCallback;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ClassicBluetoothPairCallBack;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    //Mark the user actively clicks the button to disconnect
    private boolean          isUserActiveDisconnect;
    private int              connectTimeOut;
    private int              reconnectTryTimes, reconnectInterval;
    private BleConfig              mBleConfig;
    private ClassicBluetoothConfig mClassicBluetoothConfig;

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

    @IntDef({ClassicBluetoothPairMode.JUST_WORK, ClassicBluetoothPairMode.PIN_CODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PairMode {
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


    /**
     * Only for classic bluetooth.
     * <p>
     * Perform unpaired through reflection
     *
     * @param bluetoothDevice
     * @return true or false
     */
    public boolean disBoundDevice(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return false;
        }
        try {
            Method removeBondMethod = BluetoothDevice.class.getMethod("removeBond");
            Boolean returnValue = (Boolean) removeBondMethod.invoke(bluetoothDevice);
            return returnValue.booleanValue();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
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

    public void connect(com.hjy.bluetooth.entity.BluetoothDevice bluetoothDevice, ClassicBluetoothPairCallBack classicBluetoothPairCallBack, ConnectCallBack connectCallBack) {
        if (connector != null) {
            connector.connect(bluetoothDevice, classicBluetoothPairCallBack, connectCallBack);
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

    public void readBleCharacteristic(String serviceUUID, String characteristicUUID, SendCallBack sendCallBack) {
        if (sender != null) {
            sender.readCharacteristic(serviceUUID, characteristicUUID, sendCallBack);
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
            throw new RuntimeException("You must call enableBluetooth() first.");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public HBluetooth setConnectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
        return this;
    }

    public HBluetooth openReconnect(int reconnectTryTimes, int reconnectInterval) {
        this.reconnectTryTimes = reconnectTryTimes > 6 ? 6 : reconnectTryTimes;
        this.reconnectInterval = reconnectInterval < 0 ? 0 : reconnectInterval;
        return this;
    }

    public int getReconnectTryTimes() {
        return reconnectTryTimes;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setUserActiveDisconnect(boolean userActiveDisconnect) {
        isUserActiveDisconnect = userActiveDisconnect;
    }

    public synchronized void releaseIgnoreActiveDisconnect() {
        cancelScan();
        destroyChannel();
        resetCallBack();
    }

    public boolean isUserActiveDisconnect() {
        return isUserActiveDisconnect;
    }

    public BleConfig getBleConfig() {
        return mBleConfig;
    }

    public void setBleConfig(BleConfig bleConfig) {
        mBleConfig = bleConfig;
    }

    public ClassicBluetoothConfig getClassicBluetoothConfig() {
        return mClassicBluetoothConfig;
    }

    public void setClassicBluetoothConfig(ClassicBluetoothConfig classicBluetoothConfig) {
        mClassicBluetoothConfig = classicBluetoothConfig;
    }

    /**
     * The config only for classic bluetooth
     */
    public static class ClassicBluetoothConfig {
        private int    pairMode = ClassicBluetoothPairMode.JUST_WORK;
        private String pinCode  = "1234";

        /**
         * Default mode:JUST_WORK.
         * JUST_WORK: Connect without pair
         * PIN_CODE: If the device is not bound, it will be paired before connecting
         *
         * @param pairMode
         * @return
         */
        public ClassicBluetoothConfig setPairMode(@PairMode int pairMode) {
            this.pairMode = pairMode;
            return this;
        }

        public int getPairMode() {
            return pairMode;
        }

        public String getPinCode() {
            return pinCode;
        }

        /**
         * Only the paired mode of PIN_CODE need to call this method
         *
         * @param pinCode
         * @return
         */
        public ClassicBluetoothConfig setPinCode(String pinCode) {
            this.pinCode = pinCode;
            return this;
        }
    }


    /**
     * The config only for ble
     */
    public static class BleConfig {
        private String serviceUUID, writeCharacteristicUUID, notifyCharacteristicUUID;
        private boolean useCharacteristicDescriptor;
        private int     mtuSize;
        private int     sendTimeInterval   = 20;
        private int     eachSplitPacketLen = 20;
        private int     notifyDelay        = 200;
        private boolean splitPacketToSendWhenCmdLenBeyond, autoConnect;
        private boolean               liveUpdateScannedDeviceName;
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
         * Default value is false
         * Whether to directly connect to the remote device (false) or to automatically connect as soon as the remote device becomes available (true).
         *
         * @param autoConnect
         * @return
         */
        public BleConfig autoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        /**
         * If set liveUpdateScannedDeviceName = true,Will get real-time Bluetooth device name
         * If you do not need real-time updates, please do not set
         *
         * @param liveUpdateScannedDeviceName
         * @return
         */
        public BleConfig liveUpdateScannedDeviceName(boolean liveUpdateScannedDeviceName) {
            this.liveUpdateScannedDeviceName = liveUpdateScannedDeviceName;
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
                throw new IllegalArgumentException("BleMtuChangedCallback can not be null !");
            }

            if (mtuSize > ValueLimit.DEFAULT_MAX_MTU) {
                callback.onSetMTUFailure(mtuSize, new BluetoothException("Required mtuSize should lower than 512 !"));
            }

            if (mtuSize < ValueLimit.DEFAULT_MTU) {
                callback.onSetMTUFailure(mtuSize, new BluetoothException("Required mtuSize should higher than 23 !"));
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

        public boolean isLiveUpdateScannedDeviceName() {
            return liveUpdateScannedDeviceName;
        }

        public boolean isAutoConnect() {
            return autoConnect;
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


    /**
     * Call this method when you need to disconnect
     */
    public synchronized void release() {
        isUserActiveDisconnect = true;
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
