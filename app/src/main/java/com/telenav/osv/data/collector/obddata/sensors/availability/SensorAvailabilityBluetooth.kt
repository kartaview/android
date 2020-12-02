package com.telenav.osv.data.collector.obddata.sensors.availability

import android.bluetooth.BluetoothSocket
import com.telenav.osv.data.collector.obddata.ObdHelper
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by ovidiuc2 on 11/7/16.
 */
/**
 * handles the sensor availability when the connection is bluetooth
 */
class SensorAvailabilityBluetooth(
        /**
         * Bluetooth socket
         */
        private val bluetoothSocket: BluetoothSocket) : AbstractSensorAvailability() {
    /**
     * Input stream
     */
    private var inputStream: InputStream? = null

    /**
     * output stream
     */
    private var outputStream: OutputStream? = null
    override fun isSensorAvailable(sensorType: String?): Boolean {
        Timber.tag(TAG).d("Check availability for %s", sensorType)
        val command = getCommand(sensorType)

        //if we know that this sensor belongs to a group that has no sensors available, return false and do not attempt retries
        if (NoDataDetector.instance.wasNoDataDetected(command)) {
            return false
        }
        val wasAvailabilityDetermined = retrieveCalculatedAvailability(command, sensorType!!)
        if (wasAvailabilityDetermined != null) {
            return wasAvailabilityDetermined
        }
        try {
            outputStream = bluetoothSocket.outputStream
            inputStream = bluetoothSocket.inputStream
            if (outputStream != null) {
                ObdHelper.sendCommand(outputStream, command)
            }
            delay(100)
            val response: String = ObdHelper.getRawData(inputStream)
            return getAvailability(command!!, response, sensorType)
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Socket exception when running AT command %s", command)
            Thread.currentThread().interrupt()
        }
        return false
    }
}