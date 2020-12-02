package com.telenav.osv.data.collector.obddata

import android.content.Context
import android.text.TextUtils
import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.manager.OBDServiceManager
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import com.telenav.osv.data.collector.obddata.obdinitializer.ATConstants
import com.telenav.osv.data.collector.obddata.obdinitializer.AbstractOBDInitializer
import com.telenav.osv.data.collector.obddata.obdinitializer.WifiObdInitializer
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Created by adrianbostan on 28/09/16.
 */
class ClientDataTransmissionWifi(private val wifiSocket: Socket?, obdDataListener: ObdDataListener) : AbstractClientDataTransmission(obdDataListener) {
    private var availabilities: Map<String, Boolean>? = null

    /**
     * Input stream
     */
    private var inputStream: InputStream? = null

    /**
     * output stream
     */
    private var outputStream: OutputStream? = null

    /**
     * Communication thread instance
     */
    @Volatile
    private var wifiObdCommunicationThread: ObdCollectionThread? = null
    private var atThread: Thread? = null

    @Volatile
    private var isInitializing = false

    /**
     * write to the connected out stream.
     * @param sendingCommand the command which is sent
     */
    override fun writeCommand(sendingCommand: String) {
        try {
            if (wifiSocket != null && !wifiSocket.isClosed) {
                outputStream = wifiSocket.getOutputStream()
                inputStream = wifiSocket.getInputStream()
            } else {
                obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_WIFI_SOURCE, LibraryUtil.OBD_SOCKET_ERROR)
                return
            }
            ObdHelper.sendCommand(outputStream, sendingCommand)
            delay(getDelayForCommand(sendingCommand))
            rawData = ObdHelper.getRawData(inputStream)
            if (sendingCommand !== ATConstants.Z) {
                val sensor: BaseObject<*>? = ObdHelper.convertResult(rawData)

                //notify listener of the sensor event
                if (sensor != null) {
                    obdDataListener.onSensorChanged(sensor)
                }
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "IOException when writing command to stream")
            obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_WIFI_SOURCE, LibraryUtil.OBD_SOCKET_ERROR)
        }
    }

    /**
     * sets up the OBD interface
     * Starts the thread that sends commands to the OBD in order to take sensor values
     */
    override fun startSendingSensorCommands() {
        val initial = System.currentTimeMillis()
        //this thread will run the AT Z command, and will receive the OBD device version
        wifiObdCommunicationThread = ObdCollectionThread()
        isInitializing = true
        atThread = Thread {
            val obdVersion = wifiDeviceVersion
            val abstractOBDInitializer: AbstractOBDInitializer = WifiObdInitializer(obdVersion, obdDataListener, wifiSocket)
            initializeObd(obdVersion, abstractOBDInitializer)
        }
        atThread!!.start()
        try {
            atThread!!.join()
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e(if (e.message != null) e.message else "Interrupted exception when starting sensor commands")
            Thread.currentThread().interrupt()
        }
        isInitializing = false
        //find out which sensors are available
        //availabilities = AvailabilityRetriever.retrieveAvailabilityMap(new SensorAvailabilityWifi(wifiSocket));
        availabilities = defaultMap
        if (availabilities == null) {
            obdDataListener.onConnectionStopped(LibraryUtil.OBD_WIFI_SOURCE)
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
        Timber.tag(TAG).d("Init time ${System.currentTimeMillis() - initial}")
        wifiObdCommunicationThread?.let {
            if (availabilities != null) {
                it.setAvailabilities(availabilities!!)
            }
            if (!it.isAlive && !it.wasCollectionStopped.get()) {
                it.start()
            }
        }

        Timber.tag(TAG).d("Connected to Wifi Obd ClientDataTransmissionWifi")
    }

    /**
     * Stops the thread that sends commands to OBD
     */
    override fun stopSendingSensorCommands() {
        Timber.tag(TAG).d("stopSendingSensorCommands. Threads state. IsInitializing: $isInitializing. IsRetrievingVin: $isRetrievingVin. Thread id: ${Thread.currentThread().id}")
        if (isInitializing) {
            atThread?.interrupt()
        }
        if (isRetrievingVin) {
            vinThread?.interrupt()
        }
        wifiObdCommunicationThread?.setWasCollectionStopped(true)
        wifiObdCommunicationThread?.cancel()
    }

    override fun initializationFailed() {
        ObdHelper.notifyInitializationFailed(obdDataListener)
    }

    override fun onCollectionThreadRestartRequired() {
        wifiObdCommunicationThread?.onFrequencyChanged()
    }

    override fun closeCollectionThread() {
        wifiObdCommunicationThread?.cancel()
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
            Timber.tag(TAG).e(e)
            obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_WIFI_SOURCE, LibraryUtil.OBD_ERROR_WHILE_CLOSING_CONNECTION)
        }
    }

    override fun onConnectionStateChanged(context: Context?, @LibraryUtil.HardwareSource source: String?, @LibraryUtil.ObdStatusCode statusCode: Int) {
        if (source == LibraryUtil.OBD_WIFI_SOURCE) {
            Timber.tag(TAG).d(statusCode.toString())
        }
    }

    override fun onConnectionStopped(@LibraryUtil.HardwareSource source: String?) {
        if (source == LibraryUtil.OBD_WIFI_SOURCE) {
            stopSendingSensorCommands()
            closeIOStreams()
            OBDServiceManager.Companion.instance.unbindService()
            Timber.tag(TAG).d("Disconnected from Wifi Obd ClientDataTransmissionWifi")

            //clear obd listeners
            obdDataListener.onConnectionStopped(LibraryUtil.OBD_WIFI_SOURCE)
        }
    }

    override fun onDeviceConnected(context: Context?, source: String?) {
        throw UnsupportedOperationException("The onDeviceConnected method should not be called from ClientDataTransmissionWifi")
    }//try again

    /**
     * applies the AT Z command to the ELM327 for wifi and bt connections
     * if the ELM does not respond as expected, the method is called recursively until we have the desired result
     * @return - the OBD device version
     */
    private val wifiDeviceVersion: String
        get() {
            val responseSplitter: Array<String>
            writeCommand(ATConstants.Z)
            return if (!TextUtils.isEmpty(rawData)) {
                if (!rawData!!.contains(ELM_327) || rawData.equals(ELM_327)) {
                    //try again
                    handleFailureAndGetResponse(ATConstants.CONNECTION_WIFI)
                } else {
                    responseSplitter = rawData!!.split(ELM_327).toTypedArray()
                    responseSplitter[1].trim { it <= ' ' }
                }
            } else ""
        }

    companion object {
        /**
         * Tag used for debugging
         */
        private const val TAG = "ClientDataTransWifi"
    }
}