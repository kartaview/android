package com.telenav.osv.data.collector.obddata.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.NonNull
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.ClientDataTransmissionBluetooth
import com.telenav.osv.data.collector.obddata.manager.OBDSensorManager
import com.telenav.osv.data.collector.obddata.manager.OBDServiceManager
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 *
 */
class BluetoothObdConnection
/**
 * creates a new bluetooth object
 */(
        /**
         * Context used for service
         */
        private val context: Context,
        /**
         * remote device
         */
        private val bluetoothDevice: BluetoothDevice) : AbstractObdConnection() {
    /**
     * bluetooth client socket
     */
    private var bluetoothClientSocket: BluetoothSocket? = null

    /**
     * thread used by client to initiate a bluetooth connection
     */
    private var clientRequestConnectionThread: ClientRequestConnectionThread? = null

    /**
     * Used to check if the broadcast receiver is registered or not
     */
    private var isRegistered = false
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                OBDSensorManager.instance.getAbstractClientDataTransmission()?.closeCollectionThread()
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    Timber.tag(TAG).e("Bluetooth not supported on this device")
                } else {
                    if (!bluetoothAdapter.isEnabled) {
                        //bluetooth was disabled by the client
                        onConnectionStateChanged(LibraryUtil.BLUETOOTH_NOT_ENABLED)
                        stopClientObdBluetoothConnection(false)
                    } else {
                        //connection with the remote OBD device was lost
                        stopClientObdBluetoothConnection(false)
                        connect()
                    }
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    connect()
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    onConnectionStateChanged(LibraryUtil.BLUETOOTH_ADAPTER_OFF)
                }
            }
        }
    }

    override fun connect() {
        // starts the thread to connectToBluetooth with the given device
        clientRequestConnectionThread = ClientRequestConnectionThread(BluetoothAdapter.getDefaultAdapter())
        clientRequestConnectionThread!!.start()
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (!isRegistered) {
            context.registerReceiver(mReceiver, filter)
        }
        isRegistered = true
    }

    /**
     * stops current bluetooth connection
     * @param stoppedByClient - if true, unregister from the broadcast receiver
     */
    fun stopClientObdBluetoothConnection(stoppedByClient: Boolean) {
        OBDSensorManager.instance.obdListeners.clear()
        //unbind from the collection service
        OBDServiceManager.instance.unbindService()
        onConnectionStoppedNotification(stoppedByClient)
        if (isRegistered) {
            isRegistered = false
        }
        if (stoppedByClient) {
            clientRequestConnectionThread!!.cancel()
            context.unregisterReceiver(mReceiver)
        }
    }

    /**
     * Sends notification to all listeners when the connection is established
     */
    private fun onDeviceConnectedNotification() {
        for (bluetoothListener in obdConnectionListeners) {
            bluetoothListener.onDeviceConnected(context, LibraryUtil.OBD_BLUETOOTH_SOURCE)
        }

        //create a transmission object and start collection service
        OBDSensorManager.instance.setAbstractClientDataTransmission(ClientDataTransmissionBluetooth(bluetoothClientSocket, OBDSensorManager.instance.obdDataListener))
        OBDServiceManager.instance.init(context)
        OBDServiceManager.instance.bindService()
    }

    /**
     * Sends notification to all listeners when the connection is stopped
     * @param stoppedByClient - if true, notify the client that the collection process has stopped.
     * if false, the listeners will not be cleared
     */
    private fun onConnectionStoppedNotification(stoppedByClient: Boolean) {
        for (bluetoothListener in obdConnectionListeners) {
            bluetoothListener.onConnectionStopped(LibraryUtil.OBD_BLUETOOTH_SOURCE)
        }

        //notify the transmission class
        if (stoppedByClient) {
            for (obdConnectionListener in OBDSensorManager.instance.getObdConnectionListeners()) {
                obdConnectionListener.onConnectionStopped(LibraryUtil.OBD_BLUETOOTH_SOURCE)
            }
        }
    }

    /**
     * Sends notification to all listeners when an error occurs
     * @param errorCode Error code sent to client in case of an error
     */
    private fun onConnectionStateChanged(errorCode: Int) {
        for (bluetoothListener in obdConnectionListeners) {
            bluetoothListener.onConnectionStateChanged(null, LibraryUtil.OBD_BLUETOOTH_SOURCE, errorCode)
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a device - client side
     */
    private inner class ClientRequestConnectionThread internal constructor(var bluetoothAdapter: BluetoothAdapter) : Thread() {
        override fun run() {
            if (bluetoothClientSocket != null) {
                connectToBluetooth(bluetoothClientSocket!!)
            }
        }

        private fun connectToBluetooth(@NonNull bluetoothClientSocket: BluetoothSocket) {
            var isConnected = false
            //tries to connectToBluetooth to remote device (to server)
            do {
                try {
                    bluetoothClientSocket.connect()
                    isConnected = true
                    Timber.tag(TAG).d("re-at: connected")
                } catch (e: IOException) {
                    Timber.tag(TAG).e(e)

                    //in case bluetooth is disabled, there is no point in attempting to reconnect
                    if (!bluetoothAdapter.isEnabled) {
                        onConnectionStateChanged(LibraryUtil.BLUETOOTH_NOT_ENABLED)
                        return
                    } else {
                        delay(RETRY_INTERVAL_MILLISECONDS)
                        Timber.tag(TAG).d("re-attempt connection")
                        onConnectionStateChanged(LibraryUtil.OBD_REATTEMPT_CONNECTION)
                    }
                }
            } while (!isConnected && !currentThread().isInterrupted)
            if (isConnected) {
                onDeviceConnectedNotification()
            }
        }

        /**
         * stops bluetooth connection
         */
        fun cancel() {
            onConnectionStoppedNotification(true)
            try {
                bluetoothClientSocket?.close()
            } catch (e: IOException) {
                Timber.tag(TAG).e(e)
                onConnectionStateChanged(LibraryUtil.OBD_ERROR_WHILE_CLOSING_CONNECTION)
            }
            interrupt()
        }

        /**
         * creates an object of this type
         */
        init {

            //get the bluetooth socket for a connection with the given bluetooth device
            try {
                bluetoothClientSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
            } catch (e: IOException) {
                if (!bluetoothAdapter.isEnabled) {
                    onConnectionStateChanged(LibraryUtil.BLUETOOTH_NOT_ENABLED)
                } else {
                    onConnectionStateChanged(LibraryUtil.OBD_ERROR_WHILE_CONNECTING)
                }
                Timber.tag(TAG).e(e)
            }
        }
    }

    companion object {
        /**
         * UUID for bluetooth service used in application
         */
        private val MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /**
         * Tag used for logging
         */
        private const val TAG = "BluetoothObdManager"

        /**
         * delay between connection re-tries
         */
        private const val RETRY_INTERVAL_MILLISECONDS = 4000
    }
}