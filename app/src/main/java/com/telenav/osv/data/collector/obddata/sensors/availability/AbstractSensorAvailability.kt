package com.telenav.osv.data.collector.obddata.sensors.availability

import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.OBDConstants
import com.telenav.osv.data.collector.obddata.sensors.ObdReadFailure
import timber.log.Timber
import java.util.*

/**
 * the superclass that handles sensor availabilities
 */
abstract class AbstractSensorAvailability
/**
 * we need to have the default constructor for
 * checking the availability for one more sensor, during collection
 */
internal constructor() {
    /**
     * map that specifies the position of a sensor availability flag
     */
    private val positionMap = getPositionMap()

    /**
     * the maps contain the registered sensors, grouped by their OBD PID group,
     * and their availability
     */
    private val group0100Sensors: MutableMap<String, Boolean?> = HashMap()
    private val group0120Sensors: MutableMap<String, Boolean?> = HashMap()
    private val group0140Sensors: MutableMap<String, Boolean?> = HashMap()
    private val group0160Sensors: MutableMap<String, Boolean?> = HashMap()
    private val failure: ObdReadFailure = ObdReadFailure(TOTAL_NUMBER_OF_ALLOWED_FAILURES)

    /**
     * detects whether or not the sensor value can be extracted from OBD
     * @param sensorType - the type of sensor to be extracted
     * @return - true if the sensor is available, false otherwise
     */
    abstract fun isSensorAvailable(sensorType: String?): Boolean

    /**
     * find the command that retrieves availability of a sensor
     * @param sensorType - the sensor name
     * @return the command that retrieves sensor availability(could be "0100", "0120", "0140", "0160", "0180")
     */
    fun getCommand(sensorType: String?): String? {
        return when (sensorType) {
            LibraryUtil.SPEED, LibraryUtil.RPM -> OBDConstants.CMD_0100
            LibraryUtil.FUEL_TANK_LEVEL_INPUT -> OBDConstants.CMD_0120
            LibraryUtil.FUEL_TYPE, LibraryUtil.FUEL_CONSUMPTION_RATE -> OBDConstants.CMD_0140
            LibraryUtil.ENGINE_TORQUE -> OBDConstants.CMD_0160
            LibraryUtil.VEHICLE_ID -> OBDConstants.CMD_0900
            else -> null
        }
    }

    /**
     * There are 9 sensor type modes. Each mode has a different response prefix
     * @param sensorType - the type of sensor
     * @return - the response prefix of the requested sensor
     */
    protected fun getResponsePrefix(sensorType: String?): String? {
        return when (sensorType) {
            LibraryUtil.SPEED, LibraryUtil.RPM, LibraryUtil.FUEL_TANK_LEVEL_INPUT, LibraryUtil.FUEL_TYPE, LibraryUtil.FUEL_CONSUMPTION_RATE, LibraryUtil.ENGINE_TORQUE -> OBDConstants.PREFIX_RESPONSE_MODE1_PID
            LibraryUtil.VEHICLE_ID -> OBDConstants.PREFIX_RESPONSE_MODE9_PID
            else -> null
        }
    }

    /**
     * method called whenever we get an unexpected result from OBD
     * @param sensorType - the sensor that we tried to retrieved
     * @return - true, if the required sensor was successfully retrieved, false otherwise
     */
    fun handleFailureAndGetResponse(sensorType: String?): Boolean {
        if (failure.continueTrying()) {
            delay(500)
            failure.incrementFailures()
            return isSensorAvailable(sensorType)
        }
        return false
    }

    /**
     * checks for the availability of a given sensor
     * @param command
     * @param response
     * @param sensorType
     * @return - true if the sensor is available, false otherwise
     */
    fun getAvailability(command: String, response: String, sensorType: String): Boolean {
        var response = response
        response = response.replace(" ".toRegex(), "").replace("\r".toRegex(), "")
        response = response.replace("1:06", "")
        response = response.replace("2:06", "")
        response = response.replace("3:06", "")
        response = response.replace("4:06", "")
        response = response.replace("5:06", "")
        response = response.replace("6:06", "")
        response = response.replace("7:06", "")
        val responsePrefix = getResponsePrefix(sensorType)
        if (response.isEmpty()) {
            return handleFailureAndGetResponse(sensorType)
        }
        if (response.startsWith(OBDConstants.SEARCHING + responsePrefix)) {
            return handleFailureAndGetResponse(sensorType)
        }
        if (response.contains(OBDConstants.CAN_ERROR) || response.contains(OBDConstants.CAN_ERROR.replace(" ", "")) || response.contains(OBDConstants.STOPPED)) {
            return handleFailureAndGetResponse(sensorType)
        }
        if (response.length % AvailabilityArrayCalculator.ECU_LINE_LENGTH != 0) {
            return handleFailureAndGetResponse(sensorType)
        }
        if (!response.startsWith(responsePrefix + command.substring(2, 4))) {
            return handleFailureAndGetResponse(sensorType)
        }

        //reset failures after each successful command
        resetFailures()
        Timber.tag(TAG).d("Availability for  %s: %s", sensorType, response)
        val availabilityCalculator = AvailabilityArrayCalculator(response)
        val availabilities: BooleanArray = availabilityCalculator.availabilities

        //if the last bit is 0, it means that the next group does not have any sensor available
        if (!availabilities[availabilities.size - 1]) {
            NoDataDetector.instance.noDataDetected(responsePrefix + command.substring(2, 4))
        }
        val sensorIndex = positionMap[sensorType]!!
        when (responsePrefix + command.substring(2, 4)) {
            OBDConstants.PREFIX_RESPONSE_0100 -> for (s in getGroup0100Sensors()) {
                group0100Sensors[s] = availabilities[positionMap[s]!!]
            }
            OBDConstants.PREFIX_RESPONSE_0120 -> for (s in getGroup0120Sensors()) {
                group0120Sensors[s] = availabilities[positionMap[s]!!]
            }
            OBDConstants.PREFIX_RESPONSE_0140 -> for (s in getGroup0140Sensors()) {
                group0140Sensors[s] = availabilities[positionMap[s]!!]
            }
            OBDConstants.PREFIX_RESPONSE_0160 -> for (s in getGroup0160Sensors()) {
                group0160Sensors[s] = availabilities[positionMap[s]!!]
            }
            else -> Timber.tag(TAG).d("Availability prefix invalid")
        }
        Timber.tag(TAG).d("DEBUG: Availability vector for $sensorType:$response")
        return availabilities[sensorIndex]
    }

    /**
     * if the sensor availability was already calculated because it is in a sensor group that
     * was already requested, then another availability command will not be triggered
     * @param command
     * @param sensorType
     * @return - null, if the sensor was not yet verified, true/false if it was
     */
    fun retrieveCalculatedAvailability(command: String?, sensorType: String): Boolean? {
        when (command) {
            OBDConstants.CMD_0100 -> if (group0100Sensors.containsKey(sensorType)) {
                Timber.tag(TAG).d(AVAILABILITY_FOR + sensorType + ALREADY_KNWON)
                return group0100Sensors[sensorType]
            }
            OBDConstants.CMD_0120 -> if (group0120Sensors.containsKey(sensorType)) {
                Timber.tag(TAG).d(AVAILABILITY_FOR + sensorType + ALREADY_KNWON)
                return group0120Sensors[sensorType]
            }
            OBDConstants.CMD_0140 -> if (group0140Sensors.containsKey(sensorType)) {
                Timber.tag(TAG).d(AVAILABILITY_FOR + sensorType + ALREADY_KNWON)
                return group0140Sensors[sensorType]
            }
            OBDConstants.CMD_0160 -> {
                if (group0160Sensors.containsKey(sensorType)) {
                    Timber.tag(TAG).d(AVAILABILITY_FOR + sensorType + ALREADY_KNWON)
                    return group0160Sensors[sensorType]
                }
                Timber.wtf("Invalid availability command")
            }
            else -> Timber.wtf("Invalid availability command")
        }
        return null
    }

    private fun getGroup0100Sensors(): List<String> {
        val sensors: MutableList<String> = ArrayList()
        sensors.add(LibraryUtil.SPEED)
        sensors.add(LibraryUtil.RPM)
        return sensors
    }

    private fun getGroup0120Sensors(): List<String> {
        val sensors: MutableList<String> = ArrayList()
        sensors.add(LibraryUtil.FUEL_TANK_LEVEL_INPUT)
        sensors.add(LibraryUtil.FUEL_CONSUMPTION_RATE)
        return sensors
    }

    private fun getGroup0140Sensors(): List<String> {
        val sensors: MutableList<String> = ArrayList()
        sensors.add(LibraryUtil.FUEL_TYPE)
        return sensors
    }

    private fun getGroup0160Sensors(): List<String> {
        val sensors: MutableList<String> = ArrayList()
        sensors.add(LibraryUtil.ENGINE_TORQUE)
        return sensors
    }

    /**
     * after a failure was corrected, reset the number of failures
     */
    protected fun resetFailures() {
        failure.resetFailures()
    }

    /**
     * method used whenever the OBD needs a delay to compute its current operation
     * @param ms
     */
    protected fun delay(ms: Int) {
        var waitCondition = false
        synchronized(this) {
            try {
                while (!waitCondition) {
                    Thread.sleep(ms.toLong())
                    waitCondition = true
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }//01001 availability group
    //01201 availability group
    //01401 availability group
    //01601 availability group
    /**
     * retrieves a list containing one sensor for each availability group
     * @return
     */
    val listOfAvailabilitySensorGroups: List<String>
        get() {
            val result: MutableList<String> = ArrayList()
            result.add(LibraryUtil.SPEED) //01001 availability group
            result.add(LibraryUtil.RPM)
            result.add(LibraryUtil.FUEL_TANK_LEVEL_INPUT) //01201 availability group
            result.add(LibraryUtil.FUEL_CONSUMPTION_RATE)
            result.add(LibraryUtil.FUEL_TYPE) //01401 availability group
            result.add(LibraryUtil.ENGINE_TORQUE) //01601 availability group
            return result
        }

    private fun getPositionMap(): Map<String, Int> {
        val resultMap: MutableMap<String, Int> = HashMap()
        resultMap[LibraryUtil.RPM] = OBDConstants.RPM_INDEX
        resultMap[LibraryUtil.SPEED] = OBDConstants.SPEED_INDEX
        resultMap[LibraryUtil.FUEL_TANK_LEVEL_INPUT] = OBDConstants.FUEL_TANK_LEVEL_INPUT_INDEX
        resultMap[LibraryUtil.FUEL_CONSUMPTION_RATE] = OBDConstants.FUEL_CONSUMPTION_INDEX
        resultMap[LibraryUtil.FUEL_TYPE] = OBDConstants.FUEL_TYPE_INDEX
        resultMap[LibraryUtil.ENGINE_TORQUE] = OBDConstants.ENGINE_TORQUE_INDEX
        resultMap[LibraryUtil.VEHICLE_ID] = OBDConstants.VIN_INDEX
        return resultMap
    }

    /**
     * inner class which holds information regarding groups of 32 sensors
     * for example, if isCmd0120Relevant is false, this means no sensor in the group can be retrieved
     * This class is used because when a sensor returns the message NO DATA, there is no point in attempting retries
     *
     *
     * More information abut availability groups can be found at: http://spaces.telenav.com:8080/display/TELENAVEU/Finding+out+which+sensors+are+available
     */
    internal class NoDataDetector private constructor() {
        //initially, we assume all groups contain useful sensor information
        private var isCmd0120Relevant = true
        private var isCmd0140Relevant = true
        private var isCmd0160Relevant = true
        private var isCmd0180Relevant = true

        /**
         * method that retrieves, for a sensor, whether or not it makes sense
         * to attempt collecting its value.
         * @param command - the command that we are checking for a NO DATA response
         * @return - true if the command would respond with NO DATA, false otherwise
         */
        fun wasNoDataDetected(command: String?): Boolean {
            return when (command) {
                OBDConstants.CMD_0120 -> !isCmd0120Relevant
                OBDConstants.CMD_0140 -> !isCmd0140Relevant
                OBDConstants.CMD_0160 -> !isCmd0160Relevant
                OBDConstants.CMD_0180 -> !isCmd0180Relevant
                else -> false
            }
        }

        /**
         * notifies when the next availability group will return NO DATA
         * example: noDataDetected("4100") means that that the NEXT command("0120") will return NO DATA
         * @param responsePrefix - the prefix of the availability command response
         */
        fun noDataDetected(responsePrefix: String?) {
            when (responsePrefix) {
                OBDConstants.PREFIX_RESPONSE_0100 -> isCmd0120Relevant = false
                OBDConstants.PREFIX_RESPONSE_0120 -> isCmd0140Relevant = false
                OBDConstants.PREFIX_RESPONSE_0140 -> isCmd0160Relevant = false
                OBDConstants.PREFIX_RESPONSE_0160 -> isCmd0180Relevant = false
                else -> Timber.tag(TAG).d("Invalid response prefix on noDataDetected callback")
            }
        }

        companion object {
            val instance = NoDataDetector()

        }
    }

    companion object {
        val TAG = AbstractSensorAvailability::class.java.simpleName

        /**
         * the number of tries to have in case of failure
         */
        private const val TOTAL_NUMBER_OF_ALLOWED_FAILURES = 5
        private const val AVAILABILITY_FOR = "Availability for "
        private const val ALREADY_KNWON = " already known"
    }
}