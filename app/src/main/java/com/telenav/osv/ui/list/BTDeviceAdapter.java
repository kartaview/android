package com.telenav.osv.ui.list;

import java.util.ArrayList;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.utils.Log;

/**
 * Created by dianat on 3/11/2016.
 */
// Adapter for holding devices found through scanning.
public class BTDeviceAdapter extends BaseAdapter {
    private static final String TAG = "BTDeviceAdapter";

    Context mContext;

    private ArrayList<DeviceId> mLeDevices;

    public BTDeviceAdapter(Context mContext) {
        super();
        mLeDevices = new ArrayList<>();
        this.mContext = mContext;
    }

    public void addDevice(String device, String address) {
        mLeDevices.add(new DeviceId(device, address));
        notifyDataSetChanged();
    }

    public DeviceId getDevice(int position) {
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
            view = inflater.inflate(R.layout.listitem_device, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        DeviceId device = mLeDevices.get(i);
        String name = device.name;
        String address = device.address;
        if (name != null && name.length() > 0)
            viewHolder.deviceName.setText(name);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);
        viewHolder.deviceAddress.setText(address);

        return view;
    }

    static class ViewHolder {
        TextView deviceName;

        TextView deviceAddress;
    }

    private class DeviceId {
        public String name = "";
        public String address = "";

        public DeviceId(String device, String address) {
            this.name = device;
            this.address = address;
        }
    }
}
