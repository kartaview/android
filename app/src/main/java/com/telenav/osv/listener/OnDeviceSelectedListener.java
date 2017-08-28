package com.telenav.osv.listener;

import android.bluetooth.BluetoothDevice;

/**
 * callback from the list ui for bluetooth and ble devices selection
 * Created by Kalman on 23/05/2017.
 */
public interface OnDeviceSelectedListener {

  void onDeviceSelected(String address);

  void onDeviceSelected(BluetoothDevice device);
}
