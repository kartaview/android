package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * class used for extracting the rpm sensor value from OBD
 */
class RpmObdSensor : CarObdSensor<Double?> {
    /**
     * the first byte which follows the 41 0C response is A, the second is B
     * rpm is: (256*A+B)/4
     *
     * @param hexResponse
     * @return Engine speed in rpm
     */
    override fun convertValue(hexResponse: String): Double? {
        var hexResponse = hexResponse
        hexResponse = hexResponse.replace("\r".toRegex(), " ").replace(" ".toRegex(), "")
        val responseData: String
        return if (hexResponse.length > 4) {
            responseData = hexResponse.substring(hexResponse.length - 4)
            try {
                val a = Integer.decode("0x" + responseData[0] + responseData[1])
                val b = Integer.decode("0x" + responseData[2] + responseData[3])
                (256 * a + b) / 4.0
            } catch (e: NumberFormatException) {
                Timber.tag(TAG).e(e, "Invalid response data for rpm: %s", responseData)
                null
            }
        } else {
            Timber.tag(TAG).e("word has less than 4 characters: %s", hexResponse)
            null
        }
    }

    companion object {
        val TAG = RpmObdSensor::class.java.simpleName
    }
}