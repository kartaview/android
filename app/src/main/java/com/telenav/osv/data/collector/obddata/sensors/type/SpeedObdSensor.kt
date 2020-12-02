package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * class used for extracting the speed sensor value from OBD
 */
class SpeedObdSensor : CarObdSensor<Int?> {
    var speed = 0

    /**
     * Returns the current speed in km/h
     *
     * @param hexResponse Speed value that comes from OBD in hexadecimal
     * @return Speed in km/h
     */
    override fun convertValue(hexResponse: String): Int? {
        var response = hexResponse
        response = response.replace("\r".toRegex(), " ").replace(" ".toRegex(), "")
        response = if (response.length > 2) {
            response.substring(response.length - 2)
        } else {
            Timber.tag(TAG).e("Speed response has invalid format: %s", hexResponse)
            return null
        }
        return try {
            val result = Integer.decode("0x$response")
            speed = result
            result
        } catch (e: NumberFormatException) {
            Timber.tag(TAG).e(e, "Could not decode speed response %s", response)
            null
        }
    }

    companion object {
        private val TAG = SpeedObdSensor::class.java.simpleName
    }
}