# HBluetooth
封装了支持经典蓝牙或低功耗蓝牙扫描，连接，以及通信的库。附带使用例子。该库后续会持续升级维护，敬请关注...

一.集成方式

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
 
 Step 2. Add the dependency

	dependencies {
	     implementation 'com.github.g-HJY:HBluetooth:V1.1.0'
	}


二.使用介绍

1.第一步，使用前先实例化HBluetooth（全局单例）,并且必须调用enableBluetooth()方法开启蓝牙功能：

               HBluetooth.getInstance(this).enableBluetooth()；



2.如果是低功耗蓝牙，需要设置配置项，经典蓝牙忽略跳过即可：

分别是主服务UUID（withServiceUUID）、读写特征值UUID（withWriteCharacteristicUUID）、通知UUID（withNotifyCharacteristicUUID）以及是否设置最大传输单元（setMtu不设置不用调）等

	//请填写你自己设备的UUID
        //低功耗蓝牙才需要如下配置BleConfig,经典蓝牙不需要new HBluetooth.BleConfig()
        HBluetooth.BleConfig bleConfig = new HBluetooth.BleConfig();
        bleConfig.withServiceUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                .withWriteCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                .withNotifyCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                //useCharacteristicDescriptor 默认为false
                .useCharacteristicDescriptor(false)
                .setMtu(200, new BleMtuChangedCallback() {
                    @Override
                    public void onSetMTUFailure(int realMtuSize, BleException bleException) {
                        Log.i(TAG, "bleException:" + bleException.getMessage() + "  realMtuSize:" + realMtuSize);
                    }

                    @Override
                    public void onMtuChanged(int mtuSize) {
                        Log.i(TAG, "Mtu set success,mtuSize:" + mtuSize);
                    }
                });
		HBluetooth.getInstance(this).setBleConfig(bleConfig);



3.开启蓝牙能力后，接着你就可以开始进行蓝牙设备扫描，其中，type 为蓝牙设备类型（经典蓝牙或低功耗蓝牙）：

               HBluetooth.getInstance(this)
                    .scanner()
                    .scan(type, new ScanCallBack() {
                @Override
                public void onScanStart() {
                    Log.i(TAG, "开始扫描");
                }

                @Override
                public void onScanning() {
                    Log.i(TAG, "扫描中");
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


    或者，如果你想在第一步操作后直接进行扫描，则可以这样调用：
            HBluetooth.getInstance(this)
                    .enableBluetooth()
                    .scan(type, new ScanCallBack() {
                @Override
                public void onScanStart() {
                    Log.i(TAG, "开始扫描");
                }

                @Override
                public void onScanning() {
                    Log.i(TAG, "扫描中");
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



4.一旦扫描到设备，你就可以找到目标设备并连接：

             HBluetooth.getInstance(this)
	            .connector()
                .connect(device, new ConnectCallBack() {

                    @Override
                    public void onConnecting() {
                        Log.i(TAG, "连接中...");
                    }

                    @Override
                    public void onConnected(Sender sender) {
                        Log.i(TAG, "连接成功,isConnected:" + mHBluetooth.isConnected());
                        //调用发送器发送命令
                        sender.send(new byte[]{0x01, 0x02}, new SendCallBack() {
                            @Override
                            public void onSending() {
                                Log.i(TAG, "命令发送中...");
                            }

                            @Override
                            public void onReceived(DataInputStream dataInputStream, byte[] result) {
                                Log.i(TAG, "onReceived->" + dataInputStream + "-result->" + Tools.bytesToHexString(result));
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
                    public void onNotifyFailure(BleException bleException) {
                        Log.i(TAG, "打开通知失败：" + bleException.getMessage());
                    }
                });

5.设备连接成功后，你可以开始跟设备进行通信：

               HBluetooth.getInstance(this)
                                .sender()
                                .send(new byte[]{0x01, 0x02}, new SendCallBack() {
                            @Override
                            public void onSending() {
                                Log.i(TAG, "命令发送中...");
                            }

                            @Override
                            public void onReceived(DataInputStream dataInputStream, byte[] result) {
                                Log.i(TAG, "onReceived->" + dataInputStream + "-result->" + result);
                            }
                        });

 6.最后，调用以下方法去主动断开连接并释放资源 ：
                
                HBluetooth.getInstance(this).release();
