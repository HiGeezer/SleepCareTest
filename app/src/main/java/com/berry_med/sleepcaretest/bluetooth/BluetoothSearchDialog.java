package com.berry_med.sleepcaretest.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.berry_med.sleepcaretest.R;

import java.util.ArrayList;

/**
 * Created by ZXX on 2016/11/22.
 */

public abstract class BluetoothSearchDialog {


    private Context mContext;
    private BluetoothDeviceAdapter mBtAdapter;

    public BluetoothSearchDialog(Context context, BluetoothDeviceAdapter adapter){
        this.mContext = context;
        this.mBtAdapter = adapter;
    }

    public void show(){

        final AlertDialog searchDialog = new AlertDialog.Builder(mContext).setTitle("Bluetooth Devices:").create();


        ListView lvBluetoothDevices = new ListView(mContext);
        lvBluetoothDevices.setAdapter(mBtAdapter);
        lvBluetoothDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onBtDeviceSelect(position);
                searchDialog.dismiss();
            }
        });

        searchDialog.setView(lvBluetoothDevices);
        //searchDialog.setCanceledOnTouchOutside(false);//使除了dialog以外的地方不能被点击
        searchDialog.show();
    }

    public abstract void onBtDeviceSelect(int pos);

}
