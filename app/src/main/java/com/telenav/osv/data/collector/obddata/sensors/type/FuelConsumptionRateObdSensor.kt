package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * class used for extracting the fuel consumption rate sensor value from OBD
 */
class FuelConsumptionRateObdSensor : CarObdSensor<Double?> {
    override fun convertValue(hexResponse: String): Double? {
        return if (hexResponse.length < RESPONSE_LENGTH) {
            null
        } else getFuelConsumptionRate(hexResponse.substring(PREFIX_LENGTH))
    }

    companion object {
        private val TAG = FuelConsumptionRateObdSensor::class.java.simpleName

        /**
         * the required response length
         */
        private const val RESPONSE_LENGTH = 8

        /**
         * Prefix that comes with the hexadecimal response
         */
        private const val PREFIX_LENGTH = 4

        /**
         * converts the result from hexadecimal to decimal using the formula: (256*a+b)/20
         *
         * @param fuelRate - AABB, a=AA, b=BB
         * @return - decimal value of the sensor
         */
        private fun getFuelConsumptionRate(fuelRate: String): Double? {
            var fuelRate = fuelRate
            fuelRate = fuelRate.replace("\r".toRegex(), " ").replace(" ".toRegex(), "")
            return try {
                val a = Integer.decode("0x" + fuelRate[0] + fuelRate[1])
                val b = Integer.decode("0x" + fuelRate[2] + fuelRate[3])
                (256 * a + b) / 20.0
            } catch (e: NumberFormatException) {
                Timber.tag(TAG).e(e, "Fuel consumption response has invalid format: %s", fuelRate)
                null
            }
        }
    }
}