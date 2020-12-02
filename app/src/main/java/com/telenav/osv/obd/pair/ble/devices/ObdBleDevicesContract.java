package com.telenav.osv.obd.pair.ble.devices;

import java.util.Set;
import android.bluetooth.BluetoothDevice;
import com.telenav.osv.listener.OnDeviceSelectedListener;
import com.telenav.osv.obd.pair.base.ObdConnectionDialogContract;

/**
 * The contract between the OBD devices view and the business logic.
 * @author cameliao
 */

interface ObdBleDevicesContract {

    /**
     * Interface defining all the available UI operations for the OBD devices screen.
     */
    interface ObdBleDevicesView extends ObdConnectionDialogContract.ObdConnectionDialogView {
        /**
         * Adds the list of paired devices to the list which will be displayed to the user.
         * @param pairedDevices the list containing all the bluetooth devices that were once paired with the device.
         */
        void addPairedDevices(Set<BluetoothDevice> pairedDevices);
    }

    /**
     * Interface defining all the available operations for the OBD devices screen business logic.
     */
    interface ObdBleDevicesPresenter extends ObdConnectionDialogContract.ObdConnectionDialogPresenter, OnDeviceSelectedListener {
        /**
         * Starts discovering the bluetooth devices.
         */
        void startDiscoveringDevices();

        /**
         * Cancels the bluetooth devices discovering.
         */
        void cancelDevicesDiscovery();
    }
}
