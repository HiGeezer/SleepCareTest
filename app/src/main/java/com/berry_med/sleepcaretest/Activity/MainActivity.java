package com.berry_med.sleepcaretest.Activity;

import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.berry_med.sleepcaretest.R;
import com.berry_med.sleepcaretest.bean.DateTime;
import com.berry_med.sleepcaretest.bluetooth.BluetoothDeviceAdapter;
import com.berry_med.sleepcaretest.bluetooth.BluetoothSearchDialog;
import com.berry_med.sleepcaretest.bluetooth.BluetoothUtils;
import com.berry_med.sleepcaretest.bluetooth.DataParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements BluetoothUtils.BTConnectListener,
        DataParser.onPackageReceivedListener
{

    private String TAG = this.getClass().getSimpleName();

    private Button btnBluetoothSearch;
    private TextView tvBluetoothInfo;

    private BluetoothUtils mBtUtils;
    private ArrayList<BluetoothDevice> mBtDevices;
    private BluetoothDeviceAdapter mBtAdapter;

    private Timer mObtainStatesTimer;
    private Timer mObtainInfosTimer;
    private DataParser mDataParser;

    //device states
    private TextView tvBatteryLevel;
    private TextView tvDateAndTime;
    private TextView tvRecordState;
    private TextView tvBuzzState;

    //settings
    private ToggleButton tbBuzz;
    private ToggleButton tbRecord;

    //records
    private TextView tvRecordsCount;
    private TextView tvStartDateAndTime;
    private TextView tvStopDateAndTime;
    private TextView tvSPO2Records;
    private TextView tvPulseRateRecords;
    private TextView tvRRecords;
    private TextView tvACCRecords;

    //info
    private TextView tvFirmware;
    private TextView tvHardware;
    private TextView tvMemorySize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initData();
    }

    private void initViews() {
        btnBluetoothSearch = (Button) findViewById(R.id.btnBluetoothSearch);
        tvBluetoothInfo = (TextView) findViewById(R.id.tvBluetoothInfo);
        tvBatteryLevel = (TextView) findViewById(R.id.tvBatteryLevel);
        tvDateAndTime = (TextView) findViewById(R.id.tvDateAndTime);
        tvRecordState = (TextView) findViewById(R.id.tvRecordState);
        tvBuzzState = (TextView) findViewById(R.id.tvBuzzState);
        tbRecord = (ToggleButton) findViewById(R.id.tbRecord);
        tbBuzz = (ToggleButton) findViewById(R.id.tbBuzz);
        tvRecordsCount = (TextView) findViewById(R.id.tvRecordsTotalNum);
        tvStartDateAndTime = (TextView) findViewById(R.id.tvStartDateAndTime);
        tvStopDateAndTime = (TextView) findViewById(R.id.tvStopDateAndTime);
        tvSPO2Records = (TextView) findViewById(R.id.tvSPO2Records);
        tvPulseRateRecords = (TextView) findViewById(R.id.tvPulseRateRecords);
        tvRRecords = (TextView) findViewById(R.id.tvRRIntervalRecords);
        tvACCRecords = (TextView) findViewById(R.id.tvACCRecords);

        tvFirmware = (TextView) findViewById(R.id.tvFirmwareVer);
        tvHardware = (TextView) findViewById(R.id.tvHardwareVer);
        tvMemorySize = (TextView) findViewById(R.id.tvMemorySize);
    }

    private void initData() {
        mBtDevices = new ArrayList<BluetoothDevice>();
        mBtAdapter = new BluetoothDeviceAdapter(this,mBtDevices);

        //获取设备状态的信息
        mObtainStatesTimer = new Timer();
        mObtainStatesTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(mBtUtils != null){
                    mBtUtils.write(DataParser.CMD_BATT_LEVEL);
                    SystemClock.sleep(500);
                    mBtUtils.write(DataParser.CMD_DEVICE_TIME);
                    SystemClock.sleep(500);
                    mBtUtils.write(DataParser.CMD_BUZZ_STATE);
                    SystemClock.sleep(500);
                    mBtUtils.write(DataParser.CMD_RECORD_STATE);
                }
            }
        },1000,3000);

        mObtainInfosTimer = new Timer();

        mDataParser = new DataParser(this);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.btnBluetoothSearch:
                new BluetoothSearchDialog(this, mBtAdapter) {
                    @Override
                    public void onBtDeviceSelect(final int pos) {
                        BluetoothDevice dev = null;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothDevice dev = mBtDevices.get(pos);
                                tvBluetoothInfo.setVisibility(View.VISIBLE);
                                tvBluetoothInfo.setText(dev.getName()+ "(Tap to disconnect)");
                                btnBluetoothSearch.setVisibility(View.GONE);
                                mBtUtils.connect(MainActivity.this,dev);
                            }
                        });
                    }
                }.show();
                mBtDevices.clear();
                mBtUtils.startScan(true);
                break;
            case R.id.tvBluetoothInfo:
                mBtUtils.disconnect();
                break;
            case R.id.tbBuzz:
                if(tbBuzz.isChecked()){
                    mBtUtils.write(DataParser.CMD_TURNON_BUZZ);
                }
                else{
                    mBtUtils.write(DataParser.CMD_TURNOFF_BUZZ);
                }
                break;
            case R.id.tbRecord:
                if(tbRecord.isChecked()){
                    mBtUtils.write(DataParser.CMD_TURNON_RECORD);
                }
                else{
                    mBtUtils.write(DataParser.CMD_TURNOFF_RECORD);
                }
                break;
            case R.id.btnDateTimeSync:
                Calendar c = Calendar.getInstance();
                DataParser.CMD_SYNC_DATETIME[4] = (byte) (0xff & (c.get(Calendar.YEAR)%100));
                DataParser.CMD_SYNC_DATETIME[5] = (byte) (0xff & (c.get(Calendar.MONTH)+1));
                DataParser.CMD_SYNC_DATETIME[6] = (byte) (0xff & c.get(Calendar.DAY_OF_MONTH));
                DataParser.CMD_SYNC_DATETIME[7] = (byte) (0xff & c.get(Calendar.HOUR_OF_DAY));
                DataParser.CMD_SYNC_DATETIME[8] = (byte) (0xff & c.get(Calendar.MINUTE));
                DataParser.CMD_SYNC_DATETIME[9] = (byte) (0xff & c.get(Calendar.SECOND));
                DataParser.CMD_SYNC_DATETIME[10] = 0;
                for(int i = 2; i < DataParser.CMD_SYNC_DATETIME.length-1; i++)
                {
                    DataParser.CMD_SYNC_DATETIME[10]+=DataParser.CMD_SYNC_DATETIME[i];
                }
                DataParser.CMD_SYNC_DATETIME[10] = (byte) (0xff & (~DataParser.CMD_SYNC_DATETIME[10]));

                mBtUtils.write(DataParser.CMD_SYNC_DATETIME);
                Log.i(TAG, "onClick: ---------date & time" + Arrays.toString(DataParser.CMD_SYNC_DATETIME));
                break;
            case R.id.btnFetchRecordTotalNum:
                mBtUtils.write(DataParser.CMD_FETCH_RECORDS_COUNT);
                break;
            case R.id.btnFetchStartTime:
                tvStartDateAndTime.setText("--");
                mBtUtils.write(DataParser.CMD_FETCH_START_DATETIME);
                break;
            case R.id.btnFetchStopTime:
                tvStopDateAndTime.setText("--");
                mBtUtils.write(DataParser.CMD_FETCH_STOP_DATETIME);
                break;
            case R.id.btnFetchSpO2:
                tvSPO2Records.setText("--");
                mBtUtils.write(DataParser.CMD_FETCH_SPO2);
                break;
            case R.id.btnFetchPulseRate:
                tvPulseRateRecords.setText("--");
                mBtUtils.write(DataParser.CMD_FETCH_PULSE_RATE);
                break;
            case R.id.btnFetchRR:
                tvRRecords.setText("--");
                mBtUtils.write(DataParser.CMD_FETCH_RR_INTERVAL);
                break;
            case R.id.btnFetchACC:
                tvACCRecords.setText("--");
                mBtUtils.write(DataParser.CMD_FETCH_ACC);
                break;
            case R.id.btnEraseRecords:
                mBtUtils.write(DataParser.CMD_ERASE_RECORDS);
                break;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBtUtils = BluetoothUtils.getDefaultBluetoothUtils();
        mBtUtils.registerBroadcastReceiver(this);
        mBtUtils.setConnectListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBtUtils.unregisterBroadcastReceiver(this);
        //mBtUtils.unbindService(this);
    }

    @Override
    public void onFoundDevice(BluetoothDevice device) {
        if(!mBtDevices.contains(device)){
            mBtDevices.add(device);
            mBtAdapter.notifyDataSetChanged();
            Log.i(TAG, "onFoundDevice: ------------" + device.getName() + device.getAddress());
        }
    }

    @Override
    public void onStopScan() {

    }

    @Override
    public void onStartScan() {

    }

    @Override
    public void onConnected() {
        Toast.makeText(this,"Connected",Toast.LENGTH_SHORT).show();
        //mDataParser.start();
        mObtainInfosTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(mBtUtils != null){
                    mBtUtils.write(DataParser.CMD_FIRMWARE_VER);
                    SystemClock.sleep(100);
                    mBtUtils.write(DataParser.CMD_HARDWARE_VER);
                    SystemClock.sleep(100);
                    mBtUtils.write(DataParser.CMD_MEMORY_SIZE);
                }
            }
        },500);
    }

    @Override
    public void onDisconnected() {
        //mBtUtils.unbindService(this);
        Toast.makeText(this,"Disconnected",Toast.LENGTH_SHORT).show();
        tvBluetoothInfo.setVisibility(View.GONE);
        btnBluetoothSearch.setVisibility(View.VISIBLE);
        //mDataParser.stop();
    }

    @Override
    public void onReceiveData(byte[] dat) {
        //Log.i(TAG, "onReceiveData: " + Arrays.toString(dat));
        mDataParser.add(dat,dat.length);
    }

    @Override
    public void onBatteryLevelReceived(final int level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvBatteryLevel.setText(level+"%");
            }
        });
    }

    @Override
    public void onDateTimeReceived(final DateTime datetime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDateAndTime.setText(datetime.toString());
            }
        });
    }

    @Override
    public void onBuzzStateReceived(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvBuzzState.setText(state?"On":"Off");
            }
        });
    }

    @Override
    public void onRecordStateReceived(final int state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String strState = null;
                switch (state){
                    case 0:
                        strState = "Not start";
                        break;
                    case 1:
                        strState = "During recoding";
                        break;
                    case 2:
                        strState = "Finished";
                        break;
                }
                tvRecordState.setText(strState);
            }
        });
    }

    @Override
    public void onRecordsCountReceived(final int count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvRecordsCount.setText(count+"records");
            }
        });
    }

    @Override
    public void onRecordStartDateTimeReceived(final DateTime dt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStartDateAndTime.setText(dt.toString());
            }
        });
    }

    @Override
    public void onRecordStopDateTimeReceived(final DateTime dt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStopDateAndTime.setText(dt.toString());
            }
        });
    }

    @Override
    public void onSPO2RecordFinished(final int num) {
       runOnUiThread(new Runnable() {
           @Override
           public void run() {
               tvSPO2Records.setText("Done. "+num + " records");
           }
       });
    }

    @Override
    public void onPulseRateRecordFinished(final int num) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvPulseRateRecords.setText("Done. "+num + " records");
            }
        });
    }

    @Override
    public void onRRIntervalRecordFinished(final int num) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvRRecords.setText("Done. "+num + " records");
            }
        });
    }

    @Override
    public void onACCRecordFinished(final int num) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvACCRecords.setText("Done. "+num + " records");
            }
        });
    }

    @Override
    public void onFirmwareVersionReceived(final String ver) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvFirmware.setText(ver);
            }
        });
    }

    @Override
    public void onHardwareVersionReceived(final String ver) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvHardware.setText(ver);
            }
        });
    }

    @Override
    public void onMemorySizeReceived(final String size) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvMemorySize.setText(size);
            }
        });
    }

}
