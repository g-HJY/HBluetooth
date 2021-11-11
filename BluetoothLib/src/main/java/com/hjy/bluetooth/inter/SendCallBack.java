package com.hjy.bluetooth.inter;

import java.io.DataInputStream;

/**
 * Created by _H_JY on 2018/10/24.
 */

public interface SendCallBack {

    void onSending();

    void onReceived(DataInputStream dataInputStream, byte[] result);
}
