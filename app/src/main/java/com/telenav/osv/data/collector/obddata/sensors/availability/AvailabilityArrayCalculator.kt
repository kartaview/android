package com.telenav.osv.data.collector.obddata.sensors.availability

import java.util.*

/**
 * Created by ovidiuc2 on 30.01.2018.
 */
internal class AvailabilityArrayCalculator(private var rawResponse: String) {
    /**
     * calculates sensor availabilities
     *
     * @return the availabilities of sensors in a specific group of Mode 1.
     * @see [https://en.wikipedia.org/wiki/OBD-II_PIDs.Mode_01](https://en.wikipedia.org/wiki/OBD-II_PIDs.Mode_01)
     */
    val availabilities: BooleanArray
        get() {
            val nrOfEcus = rawResponse.length / ECU_LINE_LENGTH
            if (nrOfEcus == 0) {
                return BooleanArray(0)
            }
            val ecuResponses = arrayOfNulls<String>(nrOfEcus)
            for (i in 0 until nrOfEcus) {
                ecuResponses[i] = rawResponse.substring(0, ECU_LINE_LENGTH)
                if (ecuResponses[i]!!.substring(1, 5) == EXTRA_HEADER_INFO) {
                    ecuResponses[i] = ecuResponses[i]!!.substring(5)
                }
                rawResponse = rawResponse.substring(ECU_LINE_LENGTH)
            }
            val allAvailabilities = getAvailabilityMatrix(ecuResponses)
            return getAvailabilitiesFromMatrix(allAvailabilities)
        }

    /**
     * converts the hexadecimal response of each ECU into a matrix of availabilities,
     * where each line represents the availabilities of an ECU
     *
     * @param ecuResponses hexadecimal responses from the ECU's
     * @return matrix of availabilities
     */
    private fun getAvailabilityMatrix(ecuResponses: Array<String?>): Array<BooleanArray> {
        val nrOfResponses = ecuResponses.size
        val allAvailabilities = Array(nrOfResponses) { BooleanArray(LINE_LENGTH * 4) }
        for (i in 0 until nrOfResponses) {
            if (ecuResponses[i]!!.startsWith("$i:")) {
                ecuResponses[i] = ecuResponses[i]!!.substring(4)
            }
            val ecuAvailability = convertHexToBitArray(ecuResponses[i])
            allAvailabilities[i] = ecuAvailability
        }
        return allAvailabilities
    }

    private fun getAvailabilitiesFromMatrix(allAvailabilities: Array<BooleanArray>): BooleanArray {
        val result = BooleanArray(LINE_LENGTH * 4)
        for (i in 0 until LINE_LENGTH * 4) {
            for (allAvailability in allAvailabilities) {
                if (allAvailability[i]) {
                    result[i] = true
                    break
                }
            }
        }
        return result
    }

    companion object {
        /**
         * the format for availability responses is: PPPPDDDDDDDD, where P is the prefix, and D is the hexadecimal data
         */
        const val ECU_LINE_LENGTH = 12
        private const val LINE_LENGTH = 8

        /**
         * some OBD-2 versions format differently multi-line responses, by stating at the beginning of each row: each new line has
         * a line counter, followed by the number of following bytes(06), and a prefix response(41-in case of mode 1 sensors)
         * Example:
         * 123456789ABC
         * 1:064100123456
         * 2:064100123456
         */
        private const val EXTRA_HEADER_INFO = ":0641"

        /**
         * converts the hexadecimal value returned from one of the commands(CMD_0100, CMD_0120, CMD_0140, CMD_0160) into a bit array
         *
         * @param response - hexadecimal value returned by OBD II
         * @return - boolean array, where each boolean represents the availability of a mode 1 PID
         */
        private fun convertHexToBitArray(response: String?): BooleanArray {
            val binary = setupHexadecimalConverter()
            val streamToBeParsed = response!!.substring(4)
            val binaryResult = StringBuilder()
            for (i in 0 until LINE_LENGTH) {
                binaryResult.append(binary[streamToBeParsed[i].toString()])
            }
            val result = BooleanArray(LINE_LENGTH * 4)
            for (i in 0 until LINE_LENGTH * 4) {
                result[i] = binaryResult[i] != '0'
            }
            return result
        }

        /**
         * creates the map that stores the hexadecimal equivalent of each 4-bit decimal value
         */
        private fun setupHexadecimalConverter(): Map<String, String> {
            val hexadecimalConverterMap: MutableMap<String, String> = HashMap()
            hexadecimalConverterMap["0"] = "0000"
            hexadecimalConverterMap["1"] = "0001"
            hexadecimalConverterMap["2"] = "0010"
            hexadecimalConverterMap["3"] = "0011"
            hexadecimalConverterMap["4"] = "0100"
            hexadecimalConverterMap["5"] = "0101"
            hexadecimalConverterMap["6"] = "0110"
            hexadecimalConverterMap["7"] = "0111"
            hexadecimalConverterMap["8"] = "1000"
            hexadecimalConverterMap["9"] = "1001"
            hexadecimalConverterMap["A"] = "1010"
            hexadecimalConverterMap["B"] = "1011"
            hexadecimalConverterMap["C"] = "1100"
            hexadecimalConverterMap["D"] = "1101"
            hexadecimalConverterMap["E"] = "1110"
            hexadecimalConverterMap["F"] = "1111"
            return hexadecimalConverterMap
        }
    }
}