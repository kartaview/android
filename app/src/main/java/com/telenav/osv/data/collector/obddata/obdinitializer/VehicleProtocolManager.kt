package com.telenav.osv.data.collector.obddata.obdinitializer

import timber.log.Timber
import java.util.*

/**
 *
 */
internal class VehicleProtocolManager {
    /**
     * retrieves the number of bits of a specific protocol
     *
     * @param protocol - the vehicle protocol
     * @return number of bits for the protocol
     */
    fun getProtocolBits(protocol: String?): Int {
        if (protocol!!.contains(ATConstants.PROTOCOL_ISO15765_4)) {
            val parsingArray: Array<String> = protocol.split(ATConstants.PROTOCOL_ISO15765_4).toTypedArray()
            return try {
                parsingArray[1].substring(4, 6).toInt()
            } catch (nfe: NumberFormatException) {
                0
            }
        }
        Timber.tag(TAG).d("Unrecognized protocol: %s", protocol)
        return 0
    }

    /**
     * gets the ECU that the OBD should listen from, when sending commands
     *
     *
     * the number of lines of the 0100 command response is equivalent with the number of the ECUs that respond to a command
     *
     * @param response0100 - the response to the 0100 command
     * @param bits         - the number of bits for a protocol(e.g. AUTO,ISO15765-4(CAN11/500) is a protocol of 11 bits)
     * @return - the ID of the ECU that the OBD should listen to
     */
    fun getMainEcu(response0100: String?, bits: Int): String? {
        var response0100 = response0100 ?: return null
        response0100 = response0100.replace("\r", "")
        if (is11BitIso(response0100, bits)) {
            return parseIsoResponse(response0100, 17, 3)
        } else if (is29BitIso(response0100, bits)) {
            return parseIsoResponse(response0100, 22, 8)
        }
        return null
    }

    /**
     * @param response0100 - the response to the 0100 command
     * @param bits         - the number of bits for the protocol
     * @return - true for an 11bit protocol, false otherwise
     */
    private fun is11BitIso(response0100: String, bits: Int): Boolean {
        return bits == 11 && response0100.length % 17 == 0
    }

    /**
     * @param response0100 - the response to the 0100 command
     * @param bits         - the number of bits for the protocol
     * @return - true for an 29bit protocol, false otherwise
     */
    private fun is29BitIso(response0100: String, bits: Int): Boolean {
        return bits == 29 && response0100.length % 22 == 0
    }

    /**
     * finds out which is the ecu that should be listened when collecting data through OBDII
     *
     *
     * Example: send 0100 command, and the response is:
     * 7E8064100BE1B3013
     * 7E906410088180010
     * 7EA06410080080010
     *
     *
     * the first 3 characters for each line represent the ECU ID that responded to the request. So, the ECUs that the car has
     * available are: 7E8, 7E9, 7EA. The highest priority ECU is the one with the smallest ID, so the 7E8(engine) ECU is chosen.
     *
     * @param response0100  - the response to the 0100 command
     * @param oneLineLength - the length that one response line should have
     * @param ecuIdLength   - the length of the ECU ID
     * @return - the ID of the ECU with the highest priority(aka the lowest address)
     */
    private fun parseIsoResponse(response0100: String, oneLineLength: Int, ecuIdLength: Int): String? {
        val ecuIds = arrayOfNulls<String>(response0100.length / oneLineLength)
        for (i in 0 until response0100.length / oneLineLength) {
            ecuIds[i] = response0100.substring(i * oneLineLength, i * oneLineLength + ecuIdLength)
        }
        Arrays.asList<String>(*ecuIds).sort()
        return ecuIds[0]
    }

    companion object {
        private val TAG = VehicleProtocolManager::class.java.simpleName
    }
}