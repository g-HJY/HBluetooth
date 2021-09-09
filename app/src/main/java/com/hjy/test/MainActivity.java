package com.hjy.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.hjy.hardwarehost.HBluetooth;
import com.hjy.hardwarehost.exception.BleException;
import com.hjy.hardwarehost.inter.BleMtuChangedCallback;
import com.hjy.hardwarehost.operator.abstra.Sender;
import com.hjy.hardwarehost.entity.BluetoothDevice;
import com.hjy.hardwarehost.inter.ConnectCallBack;
import com.hjy.hardwarehost.inter.ScanCallBack;
import com.hjy.hardwarehost.inter.SendCallBack;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private final static String TAG = "mylog";

    private ListView              listView;
    private List<BluetoothDevice> list = new ArrayList<>();
    private MyAdapter             adapter;

    private HBluetooth mHBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_scan_classic).setOnClickListener(this);
        findViewById(R.id.btn_scan_ble).setOnClickListener(this);
        findViewById(R.id.btn_disconnect).setOnClickListener(this);
        listView = findViewById(R.id.listView);


        adapter = new MyAdapter(this, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        mHBluetooth = HBluetooth.getInstance(this);

        mHBluetooth
                //开启蓝牙功能
                .enableBluetooth()
                //低功耗蓝牙才需要设置，传入你自己的UUID
                .setWriteCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                //设置MTU扩容
                .setMtu(500, new BleMtuChangedCallback() {
                    @Override
                    public void onSetMTUFailure(int realMtuSize, BleException bleException) {
                        Log.i(TAG, "bleException:" + bleException.getMessage() + "  realMtuSize:" + realMtuSize);
                    }

                    @Override
                    public void onMtuChanged() {

                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mHBluetooth.release();
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_disconnect) {
            mHBluetooth.release();
        } else {
            if (list != null && list.size() > 0) {
                list.clear();
                adapter.notifyDataSetChanged();
            }
            int type = 0;
            if (view.getId() == R.id.btn_scan_classic) {
                type = BluetoothDevice.DEVICE_TYPE_CLASSIC;
            } else if (view.getId() == R.id.btn_scan_ble) {
                type = BluetoothDevice.DEVICE_TYPE_LE;

                //低功耗蓝牙扫描需要手动调用stopScan()方法停止扫描，否则会一直扫描下去
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHBluetooth.scanner().stopScan();
                    }
                }, 10000);
            }


            mHBluetooth.scan(type, new ScanCallBack() {
                @Override
                public void onScanStart() {
                    Log.i(TAG, "开始扫描");
                }

                @Override
                public void onScanning(List<BluetoothDevice> scannedDevices, BluetoothDevice currentScannedDevice) {
                    Log.i(TAG, "扫描中");
                    if (scannedDevices != null && scannedDevices.size() > 0) {
                        list.clear();
                        list.addAll(scannedDevices);
                        adapter.notifyDataSetChanged();
                    }
                }


                @Override
                public void onError(int errorType, String errorMsg) {

                }

                @Override
                public void onScanFinished(List<BluetoothDevice> bluetoothDevices) {
                    Log.i(TAG, "扫描结束");
                    if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
                        list.clear();
                        list.addAll(bluetoothDevices);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        BluetoothDevice device = list.get(i);

        mHBluetooth.connector()
                .connect(device, new ConnectCallBack() {

                    @Override
                    public void onConnecting() {
                        Log.i(TAG, "连接中...");
                    }

                    @Override
                    public void onConnected(Sender sender) {
                        Log.i(TAG, "连接成功");
                        sender.send(new byte[]{0x01, 0x02}, new SendCallBack() {
                            @Override
                            public void onSending() {
                                Log.i(TAG, "命令发送中...");
                            }

                            @Override
                            public void onReceived(DataInputStream dataInputStream, byte[] bleValue) {
                                Log.i(TAG, "onReceived->" + dataInputStream + "---" + bleValue);
                            }
                        });
                    }

                    @Override
                    public void onDisConnecting() {
                        Log.i(TAG, "断开连接中...");
                    }

                    @Override
                    public void onDisConnected() {
                        Log.i(TAG, "已断开连接");
                    }

                    @Override
                    public void onError(int errorType, String errorMsg) {
                        Log.i(TAG, "错误类型：" + errorType + " 错误原因：" + errorMsg);
                    }
                });
    }


}
