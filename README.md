# HBluetooth
![未标题-1](https://user-images.githubusercontent.com/15326847/142364112-dc66af7a-440f-4c11-97d5-f3619b892e75.png)

### 简介

封装了支持经典蓝牙或低功耗蓝牙扫描，连接，以及通信的库。附带使用例子。该库后续会持续升级维护，敬请关注...

### 项目优点：

1.轻量级，无过度封装，简单易懂

2.源码关键节点均有注释，学习理解无障碍

3.源码结构按流程分工明确，便于扩展

4.同时兼容经典蓝牙和低功耗蓝牙

5....

### 适用场景：

作为主设备的智能手机（客户端Client）去连接从设备（服务端），如用安卓手机去连接智能手环、智能灯泡之类。



### 集成方式

一.项目依赖

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
	     implementation 'com.github.g-HJY:HBluetooth:V1.3.6'
	}


二.使用介绍

1.第一步，使用前先在你应用的Application中调init方法初始化HBluetooth：

             public class MyApp extends Application {

                 @Override
                 public void onCreate() {
                     super.onCreate();
                     //初始化 HBluetooth
                     HBluetooth.init(this);
                 }
             }


2.然后必须调用enableBluetooth()方法开启蓝牙功能，你可以在activity中调用：

                 //开启蓝牙功能
                 HBluetooth.getInstance().enableBluetooth();



3.如果是低功耗蓝牙，需要设置配置项，经典蓝牙忽略跳过这一步即可：

分别是主服务UUID（withServiceUUID）、读写特征值UUID（withWriteCharacteristicUUID）、通知UUID（withNotifyCharacteristicUUID）以及是否设置最大传输单元（setMtu不设置不用调）等；
您还可以设置分包发送的时间间隔和包长度

	     //请填写你自己设备的UUID
            //低功耗蓝牙才需要如下配置BleConfig,经典蓝牙不需要new HBluetooth.BleConfig()
            HBluetooth.BleConfig bleConfig = new HBluetooth.BleConfig();
            bleConfig.withServiceUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                     .withWriteCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                     .withNotifyCharacteristicUUID("0000fe61-0000-1000-8000-00805f9b34fb")
                    //命令长度大于20个字节时是否分包发送，默认false,分包时可以调两参方法设置包之间发送间隔
                    //.splitPacketToSendWhenCmdLenBeyond(false)
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
         //低功耗蓝牙才需要调setBleConfig
		HBluetooth.getInstance().setBleConfig(bleConfig);



4.开启蓝牙能力后，接着你就可以开始进行蓝牙设备扫描，其中，type 为蓝牙设备类型（经典蓝牙或低功耗蓝牙）：

               HBluetooth.getInstance()
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
            HBluetooth.getInstance()
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



5.一旦扫描到设备，你就可以找到目标设备并连接：

             HBluetooth.getInstance()
	           .connect(device, new ConnectCallBack() {

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

6.设备连接成功后，你可以开始跟设备进行通信：

               HBluetooth.getInstance()
                         .send(demoCommand, new SendCallBack() {
                          @Override
                          public void onSending(byte[] command) {
                                 Log.i(TAG, "命令发送中...");
                          }

                          @Override
                          public void onSendFailure(BluetoothException bleException) {
                                 Log.e("mylog", "发送命令失败->" + bleException.getMessage());
                          }
                          });


7.那么如何接收蓝牙设备返回给你的数据呢，很简单，在Receiver中接收：

              public void initListener() {
                  HBluetooth.getInstance().setReceiver(new ReceiveCallBack() {
                      @Override
                      public void onReceived(DataInputStream dataInputStream, byte[] result) {
                          // 打开通知后，设备发过来的数据将在这里出现
                          Log.e("mylog", "收到蓝牙设备返回数据->" + Tools.bytesToHexString(result));
                      }
                  });
              }


8.最后，调用以下方法去主动断开连接并释放资源 ：

                HBluetooth.getInstance().release();







# 更多主要方法Api介绍：

1.带设备名称过滤条件的扫描：

 public void scan(@BluetoothType int scanType, int timeUse, ScanFilter filter, ScanCallBack scanCallBack);

 public void scan(@BluetoothType int scanType, ScanFilter filter, ScanCallBack scanCallBack);


2.设置连接超时:

HBluetooth.getInstance().setConnectTimeOut(5000);


3.BleConfig(BLE)设置分包发送时间间隔(默认20ms)及包长度(默认20个字节)：

 public BleConfig splitPacketToSendWhenCmdLenBeyond(boolean splitPacketToSendWhenCmdLenBeyond, int sendTimeInterval);

 public BleConfig splitPacketToSendWhenCmdLenBeyond(boolean splitPacketToSendWhenCmdLenBeyond, int sendTimeInterval, int eachSplitPacketLen);


4.开启断开后自动重连机制，默认关闭重连：

HBluetooth.getInstance().openReconnect(3, 4000);
