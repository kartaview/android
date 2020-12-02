package com.telenav.osv.data.collector.obddata.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

/**
 * Created by adrianbostan on 12/10/16.
 */
interface BleObdDataListener {
    fun onCharacteristicChangedBle(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic)
}