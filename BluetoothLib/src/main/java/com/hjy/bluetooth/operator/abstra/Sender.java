package com.hjy.bluetooth.operator.abstra;

import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.SendCallBack;

/**
 * Created by _H_JY on 2018/10/22.
 */
public abstract class Sender {

    public abstract void send(byte[] command, SendCallBack sendCallBack);

    public abstract <T> T initChannel(T o, int type, ConnectCallBack connectCallBack);

    public abstract void discoverServices();

    public abstract <G> G initSenderHelper(G g);

    public abstract void destroyChannel();

    public abstract void resetCallBack();


    /**
     * Read characteristic according to the service uuid and characteristic uuid
     *
     * @param serviceUUID
     * @param characteristicUUID
     * @param sendCallBack
     */
    public abstract void readCharacteristic(String serviceUUID, String characteristicUUID, SendCallBack sendCallBack);

}
