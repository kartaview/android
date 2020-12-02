package com.telenav.osv.data.collector.obddata.connection

import android.annotation.TargetApi
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import timber.log.Timber
import java.util.*

/**
 * Created by adrianbostan on 06/10/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class OBDCommunication
/**
 * private constructor
 */
private constructor() {
    /**
     * bluetooth gatt - Generic Attribute Profile
     */
    var bluetoothGatt: BluetoothGatt? = null
        private set

    /**
     * bluetooth adapter
     */
    var bluetoothAdapter: BluetoothAdapter? = null
        private set

    /**
     * Bluetooth manager
     */
    private var bluetoothManager: BluetoothManager? = null

    /**
     * Initializes a reference to the local Bluetooth adapter.
     * @param context context
     * @return Return true if the initialization is successful.
     */
    fun initialize(context: Context): Boolean {
        if (bluetoothManager == null) {
            bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                Timber.tag(TAG).e("Unable to initialize BluetoothManager.")
                return false
            }
        }
        bluetoothAdapter = bluetoothManager!!.adapter
        if (bluetoothAdapter == null) {
            Timber.tag(TAG).e("Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * @param context context
     * @param address The device address of the destination device.
     * @param gattCallback the callback of the connection
     * @return A [java.util.AbstractMap.SimpleEntry] with true as a key, if the connection was successful, false otherwise
     * The connection result is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun connect(context: Context?, address: String?, gattCallback: BluetoothGattCallback?): AbstractMap.SimpleEntry<Boolean, String> {
        if (bluetoothAdapter == null || address == null) {
            Timber.w("BluetoothAdapter not initialized or unspecified address.")
            return AbstractMap.SimpleEntry(false, LibraryUtil.BL_ADAPTER_NOT_INITIALIZED)
        }
        try {
            val device = bluetoothAdapter!!.getRemoteDevice(address)
            if (device == null) {
                Timber.w(LibraryUtil.BL_DEVICE_NOT_FOUND)
                //TODO - error occurred notification required
                return AbstractMap.SimpleEntry(false, LibraryUtil.BL_DEVICE_NOT_FOUND)
            }
            // We want to directly connect to the device
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Timber.tag(TAG).d("Trying to create a new connection.")
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).e(e, LibraryUtil.BL_INVALID_MAC_ADDRESS)
            return AbstractMap.SimpleEntry(false, LibraryUtil.BL_INVALID_MAC_ADDRESS)
        }
        return AbstractMap.SimpleEntry(true, "Connected")
    }

    /**
     * subscribe the characteristic for the notification
     */
    fun subscribeToNotification() {
        //subscribe to notification
        val gatt = bluetoothGatt
        if (gatt != null) {
            val characteristic = gatt.getService(serviceUUID).getCharacteristic(characteristicUUID)
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(
                    CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    /**
     * Discover the services
     */
    fun discoverServices() {
        bluetoothGatt!!.discoverServices()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun disconnectFromBLEDevice() {
        val gatt = bluetoothGatt ?: return
        gatt.close()
        bluetoothGatt = null
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        bluetoothGatt?.let {
            if (bluetoothAdapter == null) {
                Timber.tag(TAG).w("Bluetooth adapter not initialized")
                return
            }
            it.disconnect()
        }
    }

    companion object {
        /**
         * UUID service
         */
        val serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")

        /**
         * UUID characteristic for write and notify
         */
        val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val TAG = OBDCommunication::class.java.simpleName

        /**
         * client characteristic
         */
        private val CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /**
         * Instance of the class
         */
        val instance = OBDCommunication()
    }
}