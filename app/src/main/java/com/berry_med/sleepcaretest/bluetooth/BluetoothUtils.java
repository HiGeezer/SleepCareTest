package com.berry_med.sleepcaretest.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by ZXX on 2015/11/24.
 */
@TargetApi(18)
public class BluetoothUtils{
    //TAG
    private final String TAG = this.getClass().getName();

    //UTILS
    private static BluetoothUtils mBtUtils   = null;

    //Scan
    private final int BLUETOOTH_SEARCH_TIME = 8000;
    private final BluetoothAdapter.LeScanCallback leScanCallback;
    private boolean isScanning = false;



    private BluetoothAdapter    mBtAdapter  = null;
    private BluetoothLeService  mBLEService = null;


    private BluetoothDevice   curDevice = null;
    private BTConnectListener mConnectListener;

    private BluetoothGattCharacteristic chSend;



    /**
     * init settings
     */
    private BluetoothUtils() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= 18) {
            leScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    //Log.i(BluetoothUtils.class.getName(), "API18  " + device.getName() + "---" + device.getAddress());
                    mConnectListener.onFoundDevice(device);
                }
            };
        }
        else{
            leScanCallback = null;
        }
    }

    public static BluetoothUtils getDefaultBluetoothUtils() {
        if (mBtUtils == null) {
            mBtUtils = new BluetoothUtils();
        }
        mBtUtils.enable();
        return mBtUtils;
    }


    /**
     * 打开蓝牙
     */
    public void enable() {
        if (!mBtAdapter.isEnabled()) {
            mBtAdapter.enable();
        }
    }

    /**
     * 关闭蓝牙的Runnable
     */
    final Runnable cancelRunnable = new Runnable() {
        @Override
        public void run() {
            startScan(false);
        }
    };
    final Handler  postHandler = new Handler();


    /**
     * 扫描蓝牙设备
     * @param b
     */
    @TargetApi(21)
    public void startScan(boolean b) {
        if(b){
            mConnectListener.onStartScan();

            if (Build.VERSION.SDK_INT >= 18)
            {
                if(isScanning){
                    postHandler.removeCallbacks(cancelRunnable);
                    mBtAdapter.stopLeScan(leScanCallback);
                }
                mBtAdapter.startLeScan(leScanCallback);
                postHandler.postDelayed(cancelRunnable, BLUETOOTH_SEARCH_TIME);
            }
            else{
                mBtAdapter.startDiscovery();
            }

            isScanning = true;
        }
        else{
            if (Build.VERSION.SDK_INT >= 18){
                postHandler.removeCallbacks(cancelRunnable);
                mBtAdapter.stopLeScan(leScanCallback);
            }else{
                mBtAdapter.cancelDiscovery();
            }

            mConnectListener.onStopScan();
            isScanning = false;
        }
    }



    // Code to manage Service lifecycle.
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization
            Log.w(TAG, "-------------------connect--------------------------");
            mBLEService.connect(curDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            mBLEService = null;
            mServiceConnection = null;
            curDevice = null;
        }
    };

    /**
     * 连接蓝牙设备
     * @param context
     * @param device
     */
    public void connect(Context context, final BluetoothDevice device) {
        curDevice = device;
        bindService(context);
    }

    /**
     * 断开蓝牙设备
     */
    public void disconnect()
    {
        if(mBLEService != null){
            mBLEService.disconnect();
        }
    }

    private Intent gattServiceIntent;
    public void bindService(Context context)
    {
        gattServiceIntent = new Intent(context, BluetoothLeService.class);
        context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);

        isServiceBinded = true;
    }

    private boolean isServiceBinded = false;
    public void unbindService(Context context)
    {
        if(isServiceBinded){
            context.unbindService(mServiceConnection);
            isServiceBinded = false;
        }

    }



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE);
        return intentFilter;
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            //BLE
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnectListener.onConnected();

                Log.i(TAG,"Bluetooth Connected...");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBLEService.disconnect();
                context.unbindService(mServiceConnection);

                mConnectListener.onDisconnected();
                chSend=null;
                Log.i(TAG,"Bluetooth Disonnected...");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

                for(BluetoothGattService service : mBLEService.getSupportedGattServices())
                {
                    if(service.getUuid().equals(Const.UUID_SERVICE_DATA))
                    {
                        for(BluetoothGattCharacteristic ch: service.getCharacteristics())
                        {
                            if(ch.getUuid().equals(Const.UUID_CHARACTER_RECEIVE))
                            {
                                mBLEService.setCharacteristicNotification(ch,true);
                            }
                            else if(ch.getUuid().equals(Const.UUID_CHARACTER_SEND)){
                                chSend = ch;
                            }
                        }
                    }
                }
            }
            else if (BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE.equals(action)) {
                mConnectListener.onReceiveData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
            //not BLE
            else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mConnectListener.onFoundDevice(device);
                //Log.i(TAG, "onReceive: GATT:"+ device.getName());
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                isScanning = false;
                mConnectListener.onStopScan();
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                isScanning = true;
                mConnectListener.onStartScan();
            }

        }
    };

    public void registerBroadcastReceiver(Context context)
    {
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    public void unregisterBroadcastReceiver(Context context)
    {
        context.unregisterReceiver(mGattUpdateReceiver);
    }

    public void setConnectListener(BTConnectListener listener)
    {
        mConnectListener = listener;
    }


    public void write(byte[] bytes){
        if(mBLEService!=null && chSend != null) {
            mBLEService.write(chSend, bytes);
            Log.i(TAG, "write cmd -------------" + Arrays.toString(bytes));
        }
    }

    public interface BTConnectListener
    {
        void onFoundDevice(BluetoothDevice device);
        void onStopScan();
        void onStartScan();

        void onConnected();
        void onDisconnected();
        void onReceiveData(byte[] dat);
    }
}
