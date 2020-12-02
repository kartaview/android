package com.telenav.osv.listener;

/**
 * Callback used for a bluetooth device selection.
 * Created by Kalman on 23/05/2017.
 */
public interface OnDeviceSelectedListener {

    /**
     * Method used to send the address of a selected bluetooth device.
     * @param address the MAC address of the bluetooth device.
     */
    void onDeviceSelected(String address);
}
