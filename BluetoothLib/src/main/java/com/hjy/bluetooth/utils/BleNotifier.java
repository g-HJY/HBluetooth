package com.hjy.bluetooth.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.operator.abstra.Receiver;
import com.hjy.bluetooth.operator.impl.BluetoothReceiver;

import java.util.UUID;

/**
 * author : HJY
 * date   : 2021/11/11
 * desc   :
 */
public class BleNotifier {


    /**
     * Open the ble notification
     *
     * @param gatt
     * @param service
     * @param notifyUUID
     * @param bluetoothGattCharacteristic
     * @param bleNotifyCallBack
     */
    public static void openNotification(final BluetoothGatt gatt, final BluetoothGattService service, final String notifyUUID,
                                        final BluetoothGattCharacteristic bluetoothGattCharacteristic, final BleNotifyCallBack bleNotifyCallBack) {
        int notifyDelay = 200;
        HBluetooth.BleConfig bleConfig = HBluetooth.getInstance().getBleConfig();
        if(bleConfig != null){
            notifyDelay = bleConfig.getNotifyDelay();
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //Turn on the Android terminal to receive notifications
                BluetoothGattCharacteristic finalNotifyCharacteristic = null;

                boolean notifySuccess;
                String failureMsg;
                if (!TextUtils.isEmpty(notifyUUID)) {
                    BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(UUID.fromString(notifyUUID));
                    if (notifyCharacteristic != null) {
                        notifySuccess = gatt.setCharacteristicNotification(finalNotifyCharacteristic = notifyCharacteristic, true);
                        failureMsg = "Gatt setCharacteristicNotification fail";
                    } else {
                        notifySuccess = false;
                        failureMsg = "NotificationCharacteristic is null,please check the notifyUUID whether right";
                    }
                } else {
                    notifySuccess = gatt.setCharacteristicNotification(finalNotifyCharacteristic = bluetoothGattCharacteristic, true);
                    failureMsg = "Gatt setCharacteristicNotification fail";
                }

                if (!notifySuccess) {
                    if (bleNotifyCallBack != null) {
                        bleNotifyCallBack.onNotifyFailure(new BluetoothException(failureMsg));
                    }
                    return;
                }

                //Write the data switch of on notification to the descriptor attribute of characteristic,
                //so that when the hardware data changes,
                //it can actively send data to the mobile phone
                BluetoothGattDescriptor descriptor = null;
                boolean useCharacteristicDescriptor = false;
                HBluetooth.BleConfig bleConfig = HBluetooth.getInstance().getBleConfig();
                if (bleConfig != null) {
                    useCharacteristicDescriptor = bleConfig.isUseCharacteristicDescriptor();
                }
                if (finalNotifyCharacteristic != null) {
                    if (useCharacteristicDescriptor) {
                        descriptor = finalNotifyCharacteristic.getDescriptor(finalNotifyCharacteristic.getUuid());
                    } else {
                        descriptor = finalNotifyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    }
                }

                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    notifySuccess = gatt.writeDescriptor(descriptor);
                    failureMsg = "Write descriptor fail";
                } else {
                    notifySuccess = false;
                    failureMsg = "Descriptor is null,please check whether support use characteristicDescriptor";
                }

                if (notifySuccess) {
                    Receiver receiver = HBluetooth.getInstance().receiver();
                    if (receiver != null) {
                        BluetoothReceiver bluetoothReceiver = (BluetoothReceiver) receiver;
                        bluetoothReceiver.setFinalNotifyDescriptor(descriptor);
                    }
                    if (bleNotifyCallBack != null) {
                        bleNotifyCallBack.onNotifySuccess();
                    }
                } else {
                    if (bleNotifyCallBack != null) {
                        bleNotifyCallBack.onNotifyFailure(new BluetoothException(failureMsg));
                    }
                }

            }
        }, notifyDelay);
    }


    /**
     * Close the ble notification
     */
    public static void closeNotification() {
        Receiver receiver = HBluetooth.getInstance().receiver();
        if (receiver != null) {
            BluetoothReceiver bluetoothReceiver = (BluetoothReceiver) receiver;
            BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothReceiver.getFinalNotifyDescriptor();
            if (bluetoothGattDescriptor != null) {
                bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                bluetoothReceiver.setFinalNotifyDescriptor(null);
            }
        }

    }
}
