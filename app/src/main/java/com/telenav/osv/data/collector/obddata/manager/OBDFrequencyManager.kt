package com.telenav.osv.data.collector.obddata.manager

import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import java.util.*

/**
 * class used for constructing a vector with the order of the sensors to be collected, based on the desired frequency
 */
class OBDFrequencyManager(frequencies: MutableMap<String, Int>, availabilities: Map<String, Boolean>?) {
    private var frequencies: Map<String, Int> = HashMap()

    /**
     * builds a collection vector, based on the sensor frequencies
     * Example: for a sensor <sensor1> with frequency FAST, a sensor <sensor2> with frequency SLOW, and
     * a sensor <sensor3> with frequency FAST, then the result vector should be:
     * [<sensor1> <sensor2> <sensor3> <sensor1> <sensor3>]
     *
     * @return - the collection vector
    </sensor3></sensor1></sensor3></sensor2></sensor1></sensor3></sensor2></sensor1> */
    fun computeFrequencyArray(): Array<String?> {
        val weightSum = weightSum
        val result = arrayOfNulls<String>(weightSum)
        var counter = 0
        val frequenciesCopy = mapCopy
        var done = false
        while (!done) {
            var allValuesAreZero = true
            for ((key, value) in frequenciesCopy) {
                if (value > 0) {
                    allValuesAreZero = false
                    frequenciesCopy[key] = value - 1
                    result[counter] = key
                    counter++
                }
                if (allValuesAreZero) {
                    done = true
                }
            }
        }
        return result
    }

    /**
     * computes the sum of the weights of each sensor
     *
     * @return - sum of the weights of each sensor
     */
    private val weightSum: Int
        get() {
            var weightSum = 0
            for (sensor in frequencies.entries) {
                weightSum += sensor.value
            }
            return weightSum
        }

    /**
     * @param frequency - the constant associated with a frequency stored in [LibraryUtil]
     * @return - the weight of the sensor
     */
    private fun getWeightForSensor(frequency: Int?): Int {
        //if the client did not setup frequency for a registered sensor, assign a fast weight by default
        return if (frequency == null) {
            OBD_FAST_WEIGHT
        } else when (frequency) {
            LibraryUtil.OBD_SLOW -> OBD_SLOW_WEIGHT
            LibraryUtil.OBD_FAST -> OBD_FAST_WEIGHT
            else -> OBD_FAST_WEIGHT
        }
    }

    /**
     * creates a copy of the current frequency map
     *
     * @return - a map containing the same key-value pairs as the global variable frequencies
     */
    private val mapCopy: MutableMap<String, Int>
        private get() {
            val result: MutableMap<String, Int> = HashMap()
            for ((key, value) in frequencies) {
                result[key] = value
            }
            return result
        }

    companion object {
        /**
         * the weight associated with every sensor
         */
        private const val OBD_FAST_WEIGHT = 2
        private const val OBD_SLOW_WEIGHT = 1
    }

    init {
        val desiredSensors: MutableList<String> = OBDSensorManager.instance.listOfDesiredSensors

        //the vehicle id is only collected once, we should not assign a frequency to it
        desiredSensors.remove(LibraryUtil.VEHICLE_ID)
        for (s in desiredSensors) {
            if (availabilities != null && availabilities[s]!!) {
                if (frequencies[s] != null && (frequencies[s] == LibraryUtil.OBD_FAST || frequencies[s] == LibraryUtil.OBD_SLOW)) {
                    frequencies[s] = getWeightForSensor(frequencies[s])
                } else if (frequencies[s] == null) {
                    //if the client registered for a sensor, and that sensor is available, but a frequency was not setup, assign
                    //highest possible frequency
                    frequencies[s] = OBD_FAST_WEIGHT
                }
            } else {
                frequencies.remove(s)
            }
        }
        this.frequencies = frequencies
    }
}