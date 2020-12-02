package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * class used for extracting the engine torque sensor value from OBD
 */
class EngineTorqueObdSensor : CarObdSensor<Int?> {
    override fun convertValue(hexResponse: String): Int? {
        return if (hexResponse.length < RESPONSE_LENGTH) {
            null
        } else getEngineTorque(hexResponse.substring(PREFIX_LENGTH))
    }

    companion object {
        private val TAG = EngineTorqueObdSensor::class.java.simpleName

        /**
         * the required response length
         */
        private const val RESPONSE_LENGTH = 8

        /**
         * Prefix that comes with the hexadecimal response
         */
        private const val PREFIX_LENGTH = 4

        /**
         * Returns engine torque in Nm using formula 256a*b (AABB  - > a = AA, b = BB)
         *
         * @param engineTorque Value in hexadecimal that comes from OBD
         * @return Engine torque in Nm
         */
        private fun getEngineTorque(engineTorque: String): Int? {
            var engineTorque = engineTorque
            engineTorque = engineTorque.replace("\r".toRegex(), " ").replace(" ".toRegex(), "")
            return try {
                val a = Integer.decode("0x" + engineTorque[0] + engineTorque[1])
                val b = Integer.decode("0x" + engineTorque[2] + engineTorque[3])
                256 * a + b
            } catch (e: NumberFormatException) {
                Timber.tag(TAG).e("Engine torque response has invalid format: %s", engineTorque)
                null
            }
        }
    }
}