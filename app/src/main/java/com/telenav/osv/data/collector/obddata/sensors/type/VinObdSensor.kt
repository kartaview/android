package com.telenav.osv.data.collector.obddata.sensors.type

import timber.log.Timber

/**
 * Created by ovidiuc2 on 12/13/16.
 */
class VinObdSensor {
    /**
     * get VIN from a response of form:
     * 0:bb...
     * 1:bb...
     * 2:bb...
     * ...
     * ...
     *
     * @param hexResponse
     * @return
     */
    fun convertValue(hexResponse: String): String? {
        val hexResponseEscaped = hexResponse.replace(" ".toRegex(), "")
        Timber.tag(TAG).d("vin hexa is: %s", hexResponseEscaped)
        var response = hexResponseEscaped
        if (!isVinFormatValid(response)) {
            Timber.tag(TAG).d("Vin format is invalid: %s", response)
            return null
        }
        //step 1 - remove the first 8 characters
        response = response.substring(PREFIX_LENGTH)

        //step 2 - get the first information line
        var firstLine = response.split("1:".toRegex()).toTypedArray()[0]
        response = response.split("1:".toRegex()).toTypedArray()[1]

        //step 3 - get the second information line
        val secondLine = response.split("2:".toRegex()).toTypedArray()[0]

        //step 4 - get the third information line
        val thirdLine = response.split("2:".toRegex()).toTypedArray()[1]

        //step 5 - remove the 00 pairs at the beginning
        while (firstLine.startsWith("00")) {
            firstLine = firstLine.substring(2)
        }
        val vinRawInfo = firstLine + secondLine + thirdLine
        val result = StringBuilder()

        //if the raw info still contains the : character, then the received data is invalid
        if (!vinRawInfo.contains(":")) {
            var i = 0
            while (i < 2 * VIN_LENGTH) {
                result.append(Character.toString(vinRawInfo.substring(i, i + 2).toInt(HEXA_BASE).toChar()))
                i += 2
            }
        } else {
            return null
        }
        return result.toString()
    }

    private fun isVinFormatValid(hexResponse: String): Boolean {
        return hexResponse.contains("0:") && hexResponse.contains("1:") && hexResponse.contains("2:")
    }

    companion object {
        private val TAG = VinObdSensor::class.java.simpleName
        private const val VIN_LENGTH = 17

        /**
         * the response to the vin command will start with 0:490201, which has a length of 8
         */
        private const val PREFIX_LENGTH = 8

        /**
         * used for retrieving the hexadecimal ASCII code of a character
         */
        private const val HEXA_BASE = 16
    }
}