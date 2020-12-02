package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * class used for extracting the fuel type sensor value from OBD
 */
class FuelTypeObdSensor : CarObdSensor<Int?> {
    override fun convertValue(hexResponse: String): Int? {
        val hexResponseReplaced = hexResponse.replace("\r".toRegex(), " ").replace(" ".toRegex(), "")
        return try {
            hexResponseReplaced.substring(hexResponseReplaced.length - 2).toInt()
        } catch (e: NumberFormatException) {
            Timber.tag(TAG).e(e, "Fuel type response has invalid format:%s", hexResponseReplaced)
            null
        }
    }

    companion object {
        private val TAG = FuelTypeObdSensor::class.java.simpleName
    }
}