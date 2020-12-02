package com.telenav.osv.data.collector.obddata

import com.telenav.osv.data.collector.datatype.ObdConnectionListener
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.manager.OBDFrequencyManager
import com.telenav.osv.data.collector.obddata.manager.OBDSensorManager
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import com.telenav.osv.data.collector.obddata.obdinitializer.ATConstants
import com.telenav.osv.data.collector.obddata.obdinitializer.AbstractOBDInitializer
import com.telenav.osv.data.collector.obddata.sensors.ObdReadFailure
import timber.log.Timber
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by ovidiuc2 on 10/27/16.
 */
abstract class AbstractClientDataTransmission(obdDataListener: ObdDataListener) : ObdConnectionListener {
    /**
     * Data received from wifi and ble
     */
    protected var rawData: String? = null

    /**
     * used for notifying listener about OBD sensor events
     */
    protected var obdDataListener: ObdDataListener
    protected var vinThread: Thread? = null

    @Volatile
    protected var isRetrievingVin = false

    /**
     * object that handles errors that appear during OBD communication
     */
    private val failure: ObdReadFailure
    abstract fun writeCommand(sendingCommand: String)
    abstract fun startSendingSensorCommands()
    abstract fun stopSendingSensorCommands()
    abstract fun initializationFailed()

    /**
     * there are two reasons for the collection to be restarted:
     * 1. the client subscribed to a new sensor during collection
     * 2. the client attempted to change the frequency of a sensor during collection
     */
    abstract fun onCollectionThreadRestartRequired()
    abstract fun closeCollectionThread()

    /**
     * notify the listeners of a specific sensor if it is available
     * @param sensor
     */
    fun sensorNotAvailable(sensor: String?) {
        ObdHelper.notifySensorNotAvailable(sensor, obdDataListener)
    }

    /**
     * sends a command to the OBD, on two conditions:
     * 1. the sensor is available
     * 2. the sensor is listened by at least one listener
     * @param sensorType - the type of sensor requested
     */
    private fun sendObdCommand(sensorType: String?) {
        val command: String? = ObdHelper.getCommandFromSensorType(sensorType)
        if (OBDSensorManager.instance.isSensorListened(sensorType) && command != null) {
            writeCommand(command)
            Timber.tag(TAG).d("sensorExtracted: %s", sensorType)
        }
    }

    /**
     * Returns the version of the obd based on type of connection
     * @param connectionType - Type of connection
     * @return - the OBD device version
     */
    fun getObdDeviceVersion(connectionType: String): String {
        failure.resetFailures()
        return if (connectionType == ATConstants.CONNECTION_BLE) {
            getObdBleDeviceVersion(connectionType)
        } else {
            getObdBtAndWifiDeviceVersion(connectionType)
        }
    }

    /**
     * applies the AT Z command to the ELM327 for wifi and bt connections
     * if the ELM does not respond as expected, the method is called recursively until we have the desired result
     * @return - the OBD device version
     */
    private fun getObdBtAndWifiDeviceVersion(connectionType: String): String {
        val responseSplitter: Array<String>
        writeCommand(ATConstants.Z)
        if (rawData != null) {
            if (!rawData!!.contains(ELM_327) || rawData == ELM_327) {
                //try again
                return handleFailureAndGetResponse(connectionType)
            }
            responseSplitter = rawData!!.split(ELM_327.toRegex()).toTypedArray()
            return responseSplitter[1].trim { it <= ' ' }
        }
        return handleFailureAndGetResponse(connectionType)
    }

    /**
     * applies the AT Z command to the ELM327 for ble connections
     * if the ELM does not respond as expected, the method is called recursively until we have the desired result
     * @return - the OBD device version
     */
    private fun getObdBleDeviceVersion(connectionType: String): String {
        var obdVersion = ""
        val responseSplitter: Array<String>
        writeCommand(ATConstants.Z)
        delay(ATZ_WAITING_TIME)
        if (!characteristicResultForAbstract.contains(ELM_327)) {
            return handleFailureAndGetResponse(connectionType)
        }
        Timber.tag(TAG).d("version control characteristicResult: %s", characteristicResultForAbstract)
        if (!characteristicResultForAbstract.isEmpty()) {
            responseSplitter = characteristicResultForAbstract.split(ELM_327.toRegex()).toTypedArray()
            obdVersion = responseSplitter[1].trim { it <= ' ' }
            if (obdVersion.contains(">")) {
                obdVersion = obdVersion.replace(">", "")
                obdVersion = obdVersion.replace("\r", "")
            }
            Timber.tag(TAG).d("version control responseSplitter: %s", obdVersion)
        }
        return obdVersion
    }

    /**
     * when the response to a command is not the expected one,
     * the failure object will handle it and
     * @return
     */
    fun handleFailureAndGetResponse(connectionType: String): String {
        if (failure.continueTrying()) {
            delay(ATZ_WAITING_TIME)
            failure.incrementFailures()
            return if (connectionType == ATConstants.CONNECTION_BLE) {
                getObdBleDeviceVersion(connectionType)
            } else {
                getObdBtAndWifiDeviceVersion(connectionType)
            }
        }
        return OBD_VERSION_NOT_DETECTED
    }

    /**
     * sets up the OBD interface
     * @param obdVersion - the device version
     * @param abstractOBDInitializer
     */
    fun initializeObd(obdVersion: String?, abstractOBDInitializer: AbstractOBDInitializer) {
        if (obdVersion != null) {
            abstractOBDInitializer.setupObdParser()
        } else {
            Timber.tag(TAG).d("Invalid or unknown OBD version")
        }
    }

    fun collectVehicleId() {
        isRetrievingVin = true
        vinThread = Thread {
            val command: String? = ObdHelper.getCommandFromSensorType(LibraryUtil.VEHICLE_ID)
            if (command != null) {
                writeCommand(command)
            }
            Timber.tag(TAG).d("sensorExtracted: ${LibraryUtil.VEHICLE_ID}. Command: $command")
        }
        vinThread!!.start()
        try {
            vinThread!!.join()
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e(e)
            Thread.currentThread().interrupt()
        }
        isRetrievingVin = false
    }

    /**
     * method used for reading the response from an OBD only after a delay
     * @param sendingCommand - the command sent to the OBD
     * @return - the amount of time to wait, in milliseconds
     */
    fun getDelayForCommand(sendingCommand: String?): Int {
        return when (sendingCommand) {
            OBDConstants.CMD_SPEED, OBDConstants.CMD_RPM, OBDConstants.CMD_FUEL_CONSUMPTION_RATE, OBDConstants.CMD_FUEL_TANK_LEVEL_INPUT, OBDConstants.CMD_ENGINE_TORQUE, ATConstants.S0, ATConstants.E0, ATConstants.DP, ATConstants.H1, ATConstants.H0 -> 100
            OBDConstants.CMD_VIN, ATConstants.Z -> 800
            else -> 200
        }
    }

    /**
     * method used whenever the OBD needs a delay to compute its current operation
     * @param ms
     */
    fun delay(ms: Int) {
        var waitCondition = false
        synchronized(this) {
            try {
                while (!waitCondition) {
                    sleep(ms.toLong())
                    waitCondition = true
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    val defaultMap: Map<String, Boolean>
        get() {
            val map: MutableMap<String, Boolean> = HashMap()
            map[LibraryUtil.SPEED] = true
            map[LibraryUtil.RPM] = true
            map[LibraryUtil.FUEL_TANK_LEVEL_INPUT] = true
            map[LibraryUtil.FUEL_TYPE] = true
            map[LibraryUtil.FUEL_CONSUMPTION_RATE] = true
            map[LibraryUtil.VEHICLE_ID] = true
            map[LibraryUtil.ENGINE_TORQUE] = true
            return map
        }

    /**
     * defines the thread that sends requests to OBD2 dongle in order to take sensor values
     */
    internal inner class ObdCollectionThread : Thread() {
        private var availabilities: Map<String, Boolean>? = null

        @Volatile
        private var requestData = true

        val wasCollectionStopped = AtomicBoolean()
        private var hasFrequencyChanged = false
        override fun run() {
            var sensorFrequencies = frequencyArray
            while (requestData && !this.isInterrupted) {
                if (shouldCollect) {
                    for (sensor in sensorFrequencies) {
                        sendObdCommand(sensor)
                    }
                }
                if (hasFrequencyChanged) {
                    Timber.tag(TAG).d("OVI: Frequency changed")
                    sensorFrequencies = frequencyArray
                    hasFrequencyChanged = false
                }
            }
        }

        private val frequencyArray: Array<String?>
            get() {
                val sensorFrequencies: MutableMap<String, Int> = obdDataListener.requestSensorFrequencies()
                val obdFrequencyManager = OBDFrequencyManager(sensorFrequencies, availabilities)
                return obdFrequencyManager.computeFrequencyArray()
            }

        fun setAvailabilities(availabilities: Map<String, Boolean>) {
            this.availabilities = availabilities
        }

        fun setWasCollectionStopped(wasCollectionStopped: Boolean) {
            this.wasCollectionStopped.set(wasCollectionStopped)
        }

        fun cancel() {
            requestData = false
            Timber.tag(TAG).d(" cancel called () id[" + this.id + "] requestData[" + requestData + "]")
            wasCollectionStopped.set(true)
            interrupt()
        }

        fun onFrequencyChanged() {
            hasFrequencyChanged = true
        }
    }

    companion object {
        /**
         * type of the interface
         */
        const val ELM_327 = "ELM327"

        /**
         * AT Z commands needs 800ms to execute properly
         */
        private const val ATZ_WAITING_TIME = 800

        /**
         * Tag used for testing
         */
        private val TAG = AbstractClientDataTransmission::class.java.simpleName
        private const val TOTAL_NUMBER_OF_ALLOWED_FAILURES = 5
        private const val OBD_VERSION_NOT_DETECTED = "defaultImplRequired"
        var shouldCollect = true

        /**
         * result from the written characteristic
         */
        var characteristicResultForAbstract = ""
    }

    init {
        failure = ObdReadFailure(TOTAL_NUMBER_OF_ALLOWED_FAILURES)
        this.obdDataListener = obdDataListener
    }
}