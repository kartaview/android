package com.telenav.osv.data.collector.obddata.sensors.availability

import com.telenav.osv.data.collector.obddata.ObdHelper
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Created by ovidiuc2 on 11/4/16.
 */
class SensorAvailabilityWifi(
        /**
         * Wifi socket
         */
        private val wifiSocket: Socket?) : AbstractSensorAvailability() {
    /**
     * Input stream
     */
    private var inputStream: InputStream? = null

    /**
     * output stream
     */
    private var outputStream: OutputStream? = null
    override fun isSensorAvailable(sensorType: String?): Boolean {
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
            if (wifiSocket != null) {
                outputStream = wifiSocket.getOutputStream()
                inputStream = wifiSocket.getInputStream()
            }
            ObdHelper.sendCommand(outputStream, command)
            delay(100)
            val response: String = ObdHelper.getRawData(inputStream)
            return getAvailability(command!!, response, sensorType)
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Socket exception when running AT command %s", command)
        }
        return false
    }
}