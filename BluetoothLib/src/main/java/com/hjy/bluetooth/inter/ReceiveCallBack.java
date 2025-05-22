package com.hjy.bluetooth.inter;

import java.io.DataInputStream;

/**
 * Created by _H_JY on 2018/10/24.
 */

public interface ReceiveCallBack {

    void onReceived(DataInputStream dataInputStream, byte[] result);

}
