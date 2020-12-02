package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * class used for extracting the fuel fuel tank level input sensor value from OBD
 */
class FuelTankLevelInputObdSensor : CarObdSensor<Double?> {
    override fun convertValue(hexResponse: String): Double? {
        val responseData: String
        val hexResponseReplaced = hexResponse.replace("\r".toRegex(), " ").replace(" ".toRegex(), "")
        if (hexResponseReplaced.length > 4) {
            responseData = hexResponseReplaced.substring(hexResponseReplaced.length - 2)
            return try {
                getFuelTankLevel(Integer.decode("0x" + responseData[0] + responseData[1]))
            } catch (e: NumberFormatException) {
                Timber.tag(TAG).e(e, "Fuel level response has invalid format: %s", hexResponseReplaced)
                null
            }
        }
        return null
    }

    companion object {
        private val TAG = FuelTankLevelInputObdSensor::class.java.simpleName

        /**
         * Returns the fuel level in % using formula (100/255)*a (a = AA)
         *
         * @param tankLevelHexa Response from OBD in hexadecimal
         * @return Fuel level in %
         */
        private fun getFuelTankLevel(tankLevelHexa: Int): Double {
            return 10 / 255.0 * tankLevelHexa
        }
    }
}