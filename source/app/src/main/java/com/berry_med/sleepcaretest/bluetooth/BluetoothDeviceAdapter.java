package com.berry_med.sleepcaretest.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.berry_med.sleepcaretest.R;

import java.util.ArrayList;

/**
 * Created by ZXX on 2016/11/22.
 */

public class BluetoothDeviceAdapter extends BaseAdapter
{

    private LayoutInflater mInflater;
    private Context mContext;
    private ArrayList<BluetoothDevice> arrayBluetoothDevices;

    public BluetoothDeviceAdapter(Context context, ArrayList<BluetoothDevice> devices){
        mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.arrayBluetoothDevices = devices;
    }

    @Override
    public int getCount() {
        return arrayBluetoothDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayBluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View contentView, ViewGroup parent) {

        BluetoothDevice dev = arrayBluetoothDevices.get(position);
        //内存优化
        LinearLayout llItem = null;
        if(contentView == null)
        {
            llItem = (LinearLayout) mInflater.inflate(R.layout.bluetooth_item,null);
        }
        else
        {
            llItem = (LinearLayout)contentView;
        }
        TextView tvName = (TextView) llItem.findViewById(R.id.tvBtItemName);
        TextView tvAddr = (TextView) llItem.findViewById(R.id.tvBtItemAddr);
        tvName.setText(dev.getName());
        tvAddr.setText(dev.getAddress());

        return llItem;
    }
}
