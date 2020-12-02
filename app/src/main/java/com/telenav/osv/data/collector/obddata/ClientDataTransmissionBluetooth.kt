package com.telenav.osv.data.collector.obddata

import android.bluetooth.BluetoothSocket
import android.content.Context
import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.exceptions.ObdUnsupportedOperationException
import com.telenav.osv.data.collector.obddata.manager.OBDServiceManager
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import com.telenav.osv.data.collector.obddata.obdinitializer.ATConstants
import com.telenav.osv.data.collector.obddata.obdinitializer.AbstractOBDInitializer
import com.telenav.osv.data.collector.obddata.obdinitializer.BluetoothObdInitializer
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by adrianbostan on 05/09/16.
 */
class ClientDataTransmissionBluetooth(private val bluetoothSocket: BluetoothSocket?, obdDataListener: ObdDataListener) : AbstractClientDataTransmission(obdDataListener) {
    /**
     * PhoneSensorsFrequency of taking data from OBD
     */
    private var availabilities: Map<String, Boolean>? = null

    /**
     * bluetooth connection thread instance
     */
    @Volatile
    private var bluetoothObdCommunicationThread: ObdCollectionThread? = null
    private var atThread: Thread? = null
    private var isInitializing = false

    /**
     * Input stream
     */
    private var inputStream: InputStream? = null

    /**
     * output stream
     */
    private var outputStream: OutputStream? = null

    /**
     * write to the connected out stream.
     * @param sendingCommand the command which is sent
     */
    override fun writeCommand(sendingCommand: String) {
        try {
            if (bluetoothSocket != null) {
                outputStream = bluetoothSocket.outputStream
                inputStream = bluetoothSocket.inputStream
            } else {
                onErrorOccurredNotification(LibraryUtil.BLUETOOTH_ADAPTER_OFF)
                return
            }
            ObdHelper.sendCommand(outputStream, sendingCommand)
            delay(getDelayForCommand(sendingCommand))
            rawData = ObdHelper.getRawData(inputStream)
            if (sendingCommand != ATConstants.Z) {
                val sensor: BaseObject<*>? = ObdHelper.convertResult(rawData)
                if (sensor != null) {
                    //notify listener of the sensor event
                    obdDataListener.onSensorChanged(sensor)
                }
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Bluetooth stream exception")
            onErrorOccurredNotification(LibraryUtil.BLUETOOTH_ADAPTER_OFF)
        }
    }

    override fun onConnectionStateChanged(context: Context?, @LibraryUtil.HardwareSource source: String?, @LibraryUtil.ObdStatusCode statusCode: Int) {
        if (source == LibraryUtil.OBD_BLUETOOTH_SOURCE) {
            Timber.tag(TAG).d(statusCode.toString())
        }
    }

    override fun onConnectionStopped(@LibraryUtil.HardwareSource source: String?) {
        if (source == LibraryUtil.OBD_BLUETOOTH_SOURCE) {
            Timber.tag(TAG).d("Bluetooth status onBluetoothConnectionStopped")
            stopSendingSensorCommands()
            closeIOStreams()
            OBDServiceManager.instance.unbindService()
            Timber.tag(TAG).d("Disconnected from Bluetooth Obd ClientDataTransmissionBluetooth")

            //clear obd listeners
            obdDataListener.onConnectionStopped(LibraryUtil.OBD_BLUETOOTH_SOURCE)
        }
    }

    override fun onDeviceConnected(context: Context?, source: String?) {
        throw ObdUnsupportedOperationException()
    }

    private fun onErrorOccurredNotification(errorCode: Int) {
        obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_BLUETOOTH_SOURCE, errorCode)
    }

    /**
     * Starts the thread that sends commands to the OBD in order to take sensor values
     */
    override fun startSendingSensorCommands() {
        val initial = System.currentTimeMillis()
        bluetoothObdCommunicationThread = ObdCollectionThread()
        //this thread will run the AT Z command, and will receive the OBD device version
        isInitializing = true
        atThread = Thread {
            val obdVersion: String = getObdDeviceVersion(ATConstants.CONNECTION_BLUETOOTH)
            val abstractOBDInitializer: AbstractOBDInitializer = BluetoothObdInitializer(obdVersion, obdDataListener, bluetoothSocket)
            initializeObd(obdVersion, abstractOBDInitializer)
        }
        atThread!!.start()
        try {
            atThread!!.join()
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e(e, "Interrupted exception when starting collection")
            Thread.currentThread().interrupt()
        }
        atThread!!.interrupt()
        isInitializing = false
        Timber.tag(TAG).d("Init time %s", System.currentTimeMillis() - initial)

        //availabilities = AvailabilityRetriever.retrieveAvailabilityMap(new SensorAvailabilityBluetooth(bluetoothSocket));
        availabilities = defaultMap
        if (availabilities == null) {
            obdDataListener.onConnectionStopped(LibraryUtil.OBD_BLUETOOTH_SOURCE)
            return
        }

        //notify unavailable sensors
        for ((key, value) in availabilities!!) {
            if (!value) {
                ObdHelper.notifySensorNotAvailable(key, obdDataListener)
            }
        }

        //the vehicle id is only collected once, before the other sensors
        collectVehicleId()
        bluetoothObdCommunicationThread?.let {
            if (availabilities != null) {
                it.setAvailabilities(availabilities!!)
            }
            if (!it.isAlive && !it.wasCollectionStopped.get()) {
                it.start()
            }
        }

        Timber.tag(TAG).d("Connected to Bluetooth Obd ClientDataTransmissionBluetooth")
    }

    /**
     * Stops the thread that sends commands to OBD
     */
    override fun stopSendingSensorCommands() {
        Timber.tag(TAG).d("stopSendingSensorCommands: Threads state. IsInitializing: $isInitializing. IsRetrievingVin: $isRetrievingVin. Thread id: ${Thread.currentThread().id}")
        if (isInitializing) {
            atThread?.interrupt()
        }
        if (isRetrievingVin) {
            vinThread?.interrupt()
        }
        bluetoothObdCommunicationThread?.setWasCollectionStopped(true)
        bluetoothObdCommunicationThread?.cancel()
    }

    override fun initializationFailed() {
        ObdHelper.notifyInitializationFailed(obdDataListener)
    }

    override fun onCollectionThreadRestartRequired() {
        if (bluetoothObdCommunicationThread != null) {
            bluetoothObdCommunicationThread?.onFrequencyChanged()
        }
    }

    override fun closeCollectionThread() {
        bluetoothObdCommunicationThread!!.cancel()
    }

    /**
     * Close the input and output streams used for sending and getting information from OBD
     */
    private fun closeIOStreams() {
        try {
            if (inputStream != null) {
                inputStream!!.close()
            }
            if (outputStream != null) {
                outputStream!!.close()
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Exception while closing I/O streams")
            obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_BLUETOOTH_SOURCE, LibraryUtil.OBD_ERROR_WHILE_CLOSING_CONNECTION)
        }
    }

    companion object {
        /**
         * Tag used for debugging
         */
        private const val TAG = "ClientDataTransBtooth"
    }
}