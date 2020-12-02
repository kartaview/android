package com.telenav.osv.data.collector.obddata.connection

import android.annotation.TargetApi
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.AbstractClientDataTransmission
import com.telenav.osv.data.collector.obddata.ClientDataTransmissionBle
import com.telenav.osv.data.collector.obddata.manager.OBDSensorManager
import com.telenav.osv.data.collector.obddata.manager.OBDServiceManager
import timber.log.Timber
import java.util.*

/**
 *
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class BleObdConnection(context: Context, macAddress: String?) : AbstractObdConnection() {
    /**
     * initialized when the app has connected to the remote device and is ready to collect data
     */
    private var bleTransmission: AbstractClientDataTransmission? = null

    /**
     * List of ble data listeners
     */
    private val bleObdDataListenerList: MutableList<BleObdDataListener>? = ArrayList()

    /**
     * Context of the app
     */
    private val context: Context?

    /**
     * Used to check if the app calls the stop of ble connection
     */
    private var isStoppedByClient = false

    /**
     * Device address
     */
    private val macAddress: String?

    /**
     * flag used to check if a data listener was added for sensor events
     */
    private var addedListener = false

    /**
     * Implements callback methods for GATT events that the app cares about.  For example,connection change and services discovered.
     */
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            Timber.tag(TAG).d("BluetoothGattCallback onCharacteristicChanged")
            onCharacteristicChangedBleNotification(gatt, characteristic)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Timber.tag(TAG).d("BluetoothGattCallback onConnectionStateChange and status: %s", status)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                OBDCommunication.instance.discoverServices()
                Timber.tag(TAG).d("onConnectionStateChange STATE_CONNECTED")
                onDeviceConnectedNotification()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.tag(TAG).d("onConnectionStateChange STATE_DISCONNECTED")
                broadcastUpdate(ACTION_GATT_DISCONNECTED, "Disconnected")
                //Close gatt to release resources
                OBDServiceManager.instance.unbindService()
                OBDCommunication.instance.disconnectFromBLEDevice()
                onConnectionStoppedNotification()
                if (status == GATT_ERROR) {
                    onErrorOccurredNotification(LibraryUtil.OBD_DEVICE_NOT_REACHABLE)
                }
                if (!isStoppedByClient) {
                    if (!checkIfBluetoothIsOn(OBDCommunication.instance.bluetoothAdapter)) {
                        Timber.tag(TAG).d(" stopping as bluetooth is off")
                        return
                    }
                    retryConnection()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Timber.tag(TAG).d("BluetoothGattCallback onServicesDiscovered")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_DISCOVERED, "Discovered")
                OBDCommunication.instance.subscribeToNotification()

                //create a transmission object and start collection service
                bleTransmission = ClientDataTransmissionBle(OBDSensorManager.instance.obdDataListener)
                OBDSensorManager.instance.setAbstractClientDataTransmission(bleTransmission!!)
                OBDServiceManager.instance.init(context)
                OBDServiceManager.instance.bindService()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            Timber.tag(TAG).d("BluetoothGattCallback onDescriptorWrite")
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor.value!!.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                broadcastUpdate(ACTION_GATT_SUBSCRIBED, "Subscribed")
            }
        }

        private fun retryConnection() {
            if (macAddress != null) {
                onErrorOccurredNotification(LibraryUtil.OBD_REATTEMPT_CONNECTION)
                connect()
            }
        }

        /**
         * broadcasts the results for different actions (connect, disconnect, receive response )
         *
         * @param action - the action that occurred
         * @param result - the result of the action
         */
        private fun broadcastUpdate(action: String, result: String) {
            val intent = Intent(action)
            intent.putExtra(EXTRA_DATA, result)
            Timber.tag(TAG).d("broadcastUpdate: %s", action)
            context.sendBroadcast(intent)
        }

        /**
         * Sends notification to all listeners when the connection is established
         */
        private fun onDeviceConnectedNotification() {
            for (bleObdListener in obdConnectionListeners) {
                bleObdListener.onDeviceConnected(context, LibraryUtil.OBD_BLE_SOURCE)
            }
        }

        private fun onCharacteristicChangedBleNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (!addedListener) {
                bleObdDataListenerList!!.add(bleTransmission as ClientDataTransmissionBle)
                addedListener = true
            }
            if (bleObdDataListenerList != null) {
                for (bleObdDataListener in bleObdDataListenerList) {
                    bleObdDataListener.onCharacteristicChangedBle(gatt, characteristic)
                }
            }
        }
    }

    override fun connect() {
        isStoppedByClient = false
        if (!hasBleSystemFeature(context)) {
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_NOT_SUPPORTED)
            return
        }
        if (!OBDCommunication.instance.initialize(context!!)) {
            onErrorOccurredNotification(LibraryUtil.OBD_ERROR_WHILE_CONNECTING)
            return
        }
        if (!checkIfBluetoothIsSupported(OBDCommunication.instance.bluetoothAdapter)) {
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_NOT_SUPPORTED)
            return
        }
        if (!checkIfBluetoothIsOn(OBDCommunication.instance.bluetoothAdapter)) {
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_NOT_ENABLED)
            return
        }
        // Automatically connects to the device upon successful start-up initialization.
        val response: AbstractMap.SimpleEntry<Boolean, String> = OBDCommunication.instance.connect(context, macAddress, gattCallback)
        if (!response.key) {
            onErrorOccurredNotification(LibraryUtil.OBD_ERROR_WHILE_CONNECTING)
        }
    }

    /**
     * stops current ble connection
     */
    fun stopBleObdConnection() {
        OBDSensorManager.instance.obdListeners.clear()
        isStoppedByClient = true
        OBDServiceManager.instance.unbindService()
        OBDCommunication.instance.disconnect()
        OBDSensorManager.instance.getAbstractClientDataTransmission()?.closeCollectionThread()
        onConnectionStoppedNotification()
    }

    /**
     * Check if device has ble feature
     * @param context Context of the app
     * @return True if yes.
     */
    private fun hasBleSystemFeature(context: Context?): Boolean {
        if (!context!!.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Timber.tag(TAG).d(" stopping service as Bluetooth not supported")
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_NOT_SUPPORTED)
            return false
        }
        return true
    }

    /**
     * Checks if Bluetooth is supported on the device.
     * @param bluetoothAdapter Bluetooth adapter
     * @return True if yes.
     */
    private fun checkIfBluetoothIsSupported(bluetoothAdapter: BluetoothAdapter?): Boolean {
        if (bluetoothAdapter == null) {
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_NOT_SUPPORTED)
            return false
        }
        return true
    }

    /**
     * Ensures Bluetooth is available on the device and it is enabled
     * @param bluetoothAdapter Bluetooth adapter
     * @return True if yes.
     */
    private fun checkIfBluetoothIsOn(bluetoothAdapter: BluetoothAdapter?): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Timber.tag(TAG).d(" stopping as bluetooth is off")
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_NOT_ENABLED)
            return false
        }
        return true
    }

    /**
     * Sends notification to all listeners when an error occurs
     * @param errorCode Error code sent to client in case of an error
     */
    private fun onErrorOccurredNotification(errorCode: Int) {
        for (bleObdListener in obdConnectionListeners) {
            bleObdListener.onConnectionStateChanged(null, LibraryUtil.OBD_BLE_SOURCE, errorCode)
        }
    }

    /**
     * Sends notification to all listeners when the connection is stopped
     */
    private fun onConnectionStoppedNotification() {
        for (bleObdListener in obdConnectionListeners) {
            bleObdListener.onConnectionStopped(LibraryUtil.OBD_BLE_SOURCE)
        }

        //notify the transmission class
        if (isStoppedByClient) {
            for (obdConnectionListener in OBDSensorManager.instance.getObdConnectionListeners()) {
                obdConnectionListener.onConnectionStopped(LibraryUtil.OBD_BLE_SOURCE)
            }
        }
    }

    companion object {
        /**
         * Error code used for determining if device is reachable
         */
        private const val GATT_ERROR = 133

        /**
         * Tag used for logging
         */
        private val TAG = BleObdConnection::class.java.simpleName


        /**
         * ble gatt discovered intent action
         */
        const val ACTION_GATT_DISCOVERED = "com.bluetooth.le.ACTION_GATT_DISCOVERED"

        /**
         * ble gatt disconnected intent action
         */
        const val ACTION_GATT_DISCONNECTED = "com.bluetooth.le.ACTION_GATT_DISCONNECTED"

        /**
         * ble gatt subscribed intent action
         */
        const val ACTION_GATT_SUBSCRIBED = "com.bluetooth.le.ACTION_GATT_SUBSCRIBED"

        /**
         * ble gatt extra data
         */
        const val EXTRA_DATA = "com.bluetooth.le.EXTRA_DATA"
    }


    /**
     * creates a new ble object
     */
    init {
        this.context = context
        this.macAddress = macAddress
    }
}