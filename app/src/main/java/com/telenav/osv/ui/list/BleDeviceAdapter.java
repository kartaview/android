package com.telenav.osv.ui.list;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import com.telenav.osv.R;

/**
 * Adapter for holding devices found through scanning.
 * Created by dianat on 3/11/2016.
 */
public class BleDeviceAdapter extends BaseAdapter {
    Context mContext;

    private ArrayList<BluetoothDevice> mLeDevices;

    public BleDeviceAdapter(Context mContext) {
        super();
        mLeDevices = new ArrayList<>();
        this.mContext = mContext;
    }

    public void addDevice(BluetoothDevice device) {
        if (device == null){
            mLeDevices.add(null);
        } else if (!mLeDevices.contains(device) && device.getName() != null) {
            mLeDevices.add(device);
        }
        notifyDataSetChanged();
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mLeDevices.get(position);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        ViewHolder viewHolder;
        if (view == null) {
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            view = inflater.inflate(R.layout.item_bt_device_list, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);
        if (device == null){
            viewHolder.deviceName.setText(R.string.bt_none_found);
        }
        String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
            viewHolder.deviceAddress.setText(device.getAddress());
        }

        return view;
    }

    static class ViewHolder {
        TextView deviceName;

        TextView deviceAddress;
    }
}
