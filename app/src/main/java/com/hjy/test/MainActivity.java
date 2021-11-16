package com.hjy.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.hjy.bluetooth.HBluetooth;
import com.hjy.bluetooth.entity.BluetoothDevice;
import com.hjy.bluetooth.exception.BluetoothException;
import com.hjy.bluetooth.inter.BleMtuChangedCallback;
import com.hjy.bluetooth.inter.BleNotifyCallBack;
import com.hjy.bluetooth.inter.ConnectCallBack;
import com.hjy.bluetooth.inter.ReceiveCallBack;
import com.hjy.bluetooth.inter.ScanCallBack;
import com.hjy.bluetooth.inter.SendCallBack;
import com.hjy.bluetooth.operator.abstra.Sender;

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

        mHBluetooth = HBluetooth.getInstance();

        //请填写你自己设备的UUID
        //低功耗蓝牙才需要如下配置BleConfig,经典蓝牙不需要new HBluetooth.BleConfig()
        HBluetooth.BleConfig bleConfig = new HBluetooth.BleConfig();
        bleConfig.withServiceUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                .withWriteCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                .withNotifyCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                //命令长度大于20个字节时是否分包发送，默认false,分包时可以调两参方法设置包之间发送间隔
                //.splitPacketToSendWhenCmdLenBeyond20(false)
                //useCharacteristicDescriptor 默认false
                //.useCharacteristicDescriptor(false)
                //连接后开启通知的延迟时间，单位ms，默认200ms
                //.notifyDelay(200)
                .setMtu(200, new BleMtuChangedCallback() {
                    @Override
                    public void onSetMTUFailure(int realMtuSize, BluetoothException bleException) {
                        Log.i(TAG, "bleException:" + bleException.getMessage() + "  realMtuSize:" + realMtuSize);
                    }

                    @Override
                    public void onMtuChanged(int mtuSize) {
                        Log.i(TAG, "Mtu set success,mtuSize:" + mtuSize);
                    }
                });


        mHBluetooth
                //开启蓝牙功能
                .enableBluetooth()
                //低功耗蓝牙才需要调setBleConfig
                .setBleConfig(bleConfig);


        initListener();
    }

    public void initListener() {
        HBluetooth.getInstance().setReceiver(new ReceiveCallBack() {
            @Override
            public void onReceived(DataInputStream dataInputStream, byte[] result) {
                // 打开通知后，设备发过来的数据将在这里出现
                Log.e("mylog", "收到蓝牙设备返回数据->" + Tools.bytesToHexString(result));
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

                //如果没有设置扫描时间，低功耗蓝牙扫描需要手动调用stopScan()方法停止扫描，否则会一直扫描下去
                //                new Handler().postDelayed(new Runnable() {
                //                    @Override
                //                    public void run() {
                //                        mHBluetooth.scanner().stopScan();
                //                    }
                //                }, 10000);
            }

            boolean setScanTimeUse = true;
            if (setScanTimeUse) {
                //有设置扫描时间的扫描，时间到会自动结束扫描
                scanWithTimeUse(type);
            } else {
                //扫描蓝牙设备,没有设置扫描时间,低功耗蓝牙会一直扫描下去
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
                        Toast.makeText(MainActivity.this, "扫描结束", Toast.LENGTH_LONG).show();
                        if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
                            list.clear();
                            list.addAll(bluetoothDevices);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }

        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        BluetoothDevice device = list.get(i);
        //调用连接器连接蓝牙设备
        mHBluetooth.connect(device, new ConnectCallBack() {

            @Override
            public void onConnecting() {
                Log.i(TAG, "连接中...");
            }

            @Override
            public void onConnected(Sender sender) {
                Log.i(TAG, "连接成功,isConnected:" + mHBluetooth.isConnected());
                //调用发送器发送命令
                byte[] demoCommand = new byte[]{0x01, 0x02};
                sender.send(demoCommand, new SendCallBack() {
                    @Override
                    public void onSending(byte[] command) {
                        Log.i(TAG, "命令发送中...");
                    }

                    @Override
                    public void onSendFailure(BluetoothException bleException) {
                        Log.e("mylog", "发送命令失败->" + bleException.getMessage());
                    }
                });
            }

            @Override
            public void onDisConnecting() {
                Log.i(TAG, "断开连接中...");
            }

            @Override
            public void onDisConnected() {
                Log.i(TAG, "已断开连接,isConnected:" + mHBluetooth.isConnected());
            }

            @Override
            public void onError(int errorType, String errorMsg) {
                Log.i(TAG, "错误类型：" + errorType + " 错误原因：" + errorMsg);
            }

            //低功耗蓝牙才需要BleNotifyCallBack
            //经典蓝牙可以只调两参方法connect(BluetoothDevice device, ConnectCallBack connectCallBack)
        }, new BleNotifyCallBack() {
            @Override
            public void onNotifySuccess() {
                Log.i(TAG, "打开通知成功");
            }

            @Override
            public void onNotifyFailure(BluetoothException bleException) {
                Log.i(TAG, "打开通知失败：" + bleException.getMessage());
            }
        });
    }


    private void scanWithTimeUse(int type) {
        //扫描蓝牙设备，扫描6秒就自动停止扫描
        mHBluetooth.scan(type, 6000, new ScanCallBack() {
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
                Toast.makeText(MainActivity.this, "扫描结束", Toast.LENGTH_LONG).show();
                if (bluetoothDevices != null && bluetoothDevices.size() > 0) {
                    list.clear();
                    list.addAll(bluetoothDevices);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

}
