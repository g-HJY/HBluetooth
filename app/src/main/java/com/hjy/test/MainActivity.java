package com.hjy.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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
    private int bluetoothType;
    private static final int REQUEST_CODE_SCAN_BT = 100;
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
                //指定UUID扫描，即扫描过滤掉非当前设置的UUID的设备
                //.setScanFilterServerUUIDs(new UUID[]{UUID.fromString("0000fe61-0000-1000-8000-00805f9b34fb")})
                //.liveUpdateScannedDeviceName(true)
                //命令长度大于20个字节时是否分包发送，默认false,分包时可以调两参方法设置包之间发送间隔
                //默认false,注释部分为默认值
                //.splitPacketToSendWhenCmdLenBeyond(false)
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
                //设备发过来的数据将在这里出现
                Log.e("mylog", "收到蓝牙设备返回数据->" + Tools.bytesToHexString(result));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mHBluetooth.release();
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_disconnect:
                mHBluetooth.release();
                break;
            case R.id.btn_scan_classic:
                bluetoothType = BluetoothDevice.DEVICE_TYPE_CLASSIC;
                checkPermissionAndScan();
                break;
            case R.id.btn_scan_ble:
                bluetoothType = BluetoothDevice.DEVICE_TYPE_LE;
                checkPermissionAndScan();
                break;
        }
    }

    private boolean isAboveAndroid12(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    private boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_SCAN_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，可以执行相关操作
                scan();
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "Bluetooth permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void checkPermissionAndScan(){
        if (isAboveAndroid12()) {
            //Android 12 and above, subdivided Bluetooth permissions, dynamic application required
            if(bluetoothType == BluetoothDevice.DEVICE_TYPE_LE){
                if (hasPermission(Manifest.permission.BLUETOOTH_SCAN) && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    scan();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{ Manifest.permission.BLUETOOTH_SCAN,
                                          Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_CODE_SCAN_BT);
                }
            }else if(bluetoothType == BluetoothDevice.DEVICE_TYPE_CLASSIC){
                if (hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)) {
                    scan();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_CODE_SCAN_BT);
                }
            }
        } else {
            scan();
        }
    }


    private void scan(){
        if (list != null && list.size() > 0) {
            list.clear();
            adapter.notifyDataSetChanged();
        }

        //        if(bleType == BluetoothDevice.DEVICE_TYPE_LE){
        //            //如果没有设置扫描时间，低功耗蓝牙扫描需要手动调用stopScan()方法停止扫描，否则会一直扫描下去
        //                            new Handler().postDelayed(new Runnable() {
        //                                @Override
        //                                public void run() {
        //                                    mHBluetooth.scanner().stopScan();
        //                                }
        //                            }, 10000);
        //        }

        boolean setScanTimeUse = true;
        if (setScanTimeUse) {
            //有设置扫描时间的扫描，时间到会自动结束扫描
            scanWithTimeUse(bluetoothType);
        } else {
            //扫描蓝牙设备,没有设置扫描时间,低功耗蓝牙会一直扫描下去
            mHBluetooth.scan(bluetoothType, new ScanCallBack() {
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
                    Log.e(TAG, "errorType:"+errorType+"  errorMsg:"+errorMsg);
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


    private void scanWithTimeUse(int bleType) {
        //扫描蓝牙设备，扫描6秒就自动停止扫描
        mHBluetooth.scan(bleType, 6000, new ScanCallBack() {
            @Override
            public void onScanStart() {
                Log.i(TAG, "开始扫描");
            }

            @Override
            public void onScanning(List<BluetoothDevice> scannedDevices, BluetoothDevice currentScannedDevice) {
                //Log.i(TAG, "扫描中");
                if (scannedDevices != null && scannedDevices.size() > 0) {
                    list.clear();
                    list.addAll(scannedDevices);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(int errorType, String errorMsg) {
                Log.e(TAG, "errorType:"+errorType+"  errorMsg:"+errorMsg);
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
