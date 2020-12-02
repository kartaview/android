package com.telenav.osv.data.collector.obddata

import com.telenav.osv.data.collector.datatype.datatypes.*
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import com.telenav.osv.data.collector.obddata.sensors.type.*
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.DateFormat
import java.util.*

/**
 * Created by adrianbostan on 06/09/16.
 */
object ObdHelper {
    /**
     * Tag used for debugging
     */
    private const val TAG = "ObdHelper"
    private const val COMMAND_SUBSTRING_LENGTH = 4

    /**
     * Sends a command on the stream
     *
     * @param outputStream   Output stream used for sending commands
     * @param sendingCommand The command to send
     * @throws IOException
     */
    fun sendCommand(outputStream: OutputStream?, sendingCommand: String?) {
        try {
            if (outputStream != null) {
                outputStream.write((sendingCommand + '\r').toByteArray())
                outputStream.flush()
                Timber.tag(TAG).d("writeCommand: command = %s", sendingCommand)
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "Socket is closed")
        }
    }

    /**
     * Returns raw value of the data that comes from obd
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun getRawData(inputStream: InputStream?): String {
        val rawData: String
        var b: Byte
        val res = StringBuilder()

        // read until '>' arrives
        val start = System.currentTimeMillis()
        if (inputStream != null) {
            while (((inputStream.read().toByte().also { b = it }).toChar() != '>') && res.length < 60 && System.currentTimeMillis() - start < 1000) {
                res.append(b.toChar())
            }
        }
        rawData = res.toString().trim { it <= ' ' }
        Timber.tag(TAG).d("getRawData: rawData = %s", rawData)
        return rawData
    }

    /**
     * Convert the data that comes from obd
     *
     * @param responseData response that comes
     */
    fun convertResult(responseData: String?): BaseObject<*>? {
        if (responseData != null && responseData.isNotEmpty()) {
            var prefixResponse = ""
            var response: String = responseData

            //in case of multiline response with spaces, remove spaces
            response = response.replace(" ".toRegex(), "").replace("\r".toRegex(), "")
            response = handleEchoedResponse(response)

            //check if this is a VIN response data
            if (response.contains(OBDConstants.PREFIX_RESPONSE_VIN)) {
                if (response.startsWith(OBDConstants.OPTIONAL_VIN_INFO)) {
                    response = response.replaceFirst(OBDConstants.OPTIONAL_VIN_INFO.toRegex(), "")
                }
                prefixResponse = response.substring(0, OBDConstants.PREFIX_RESPONSE_VIN.length)
            } else if (response.length >= COMMAND_SUBSTRING_LENGTH) {
                //if the command is not a VIN command
                prefixResponse = response.substring(0, COMMAND_SUBSTRING_LENGTH)
            }
            return when (prefixResponse) {
                OBDConstants.PREFIX_RESPONSE_SPEED -> getSpeed(response)
                OBDConstants.PREFIX_RESPONSE_RPM -> getEngineSpeed(response)
                OBDConstants.PREFIX_RESPONSE_TANK_LEVEL -> getFuelLevel(response)
                OBDConstants.PREFIX_RESPONSE_FUEL_TYPE -> getFuelType(response)
                OBDConstants.PREFIX_RESPONSE_FUEL_CONSUMPTION -> getFuelConsumptionRate(response)
                OBDConstants.PREFIX_RESPONSE_TORQUE_VALUE -> getEngineTorque(response)
                OBDConstants.PREFIX_RESPONSE_VIN -> getVehicleId(response)
                else -> null
            }
        }
        return null
    }

    private fun handleEchoedResponse(response: String): String {
        if (response.startsWith(OBDConstants.CMD_SPEED)) {
            return response.replaceFirst(OBDConstants.CMD_SPEED.toRegex(), "")
        }
        if (response.startsWith(OBDConstants.CMD_RPM)) {
            return response.replaceFirst(OBDConstants.CMD_RPM.toRegex(), "")
        }
        if (response.startsWith(OBDConstants.CMD_FUEL_TANK_LEVEL_INPUT)) {
            return response.replaceFirst(OBDConstants.CMD_FUEL_TANK_LEVEL_INPUT.toRegex(), "")
        }
        if (response.startsWith(OBDConstants.CMD_FUEL_CONSUMPTION_RATE)) {
            return response.replaceFirst(OBDConstants.CMD_FUEL_CONSUMPTION_RATE.toRegex(), "")
        }
        return if (response.startsWith(OBDConstants.CMD_FUEL_TYPE)) {
            response.replaceFirst(OBDConstants.CMD_FUEL_TYPE.toRegex(), "")
        } else response
    }

    fun notifySensorNotAvailable(sensorType: String?, obdDataListener: ObdDataListener) {
        when (sensorType) {
            LibraryUtil.SPEED -> {
                val speedObject = ObdSpeedObject(LibraryUtil.OBD_NOT_AVAILABLE)
                obdDataListener.onSensorChanged(speedObject)
            }
            LibraryUtil.RPM -> {
                val engineSpeedObject: EngineSpeedObject = ObdEngineSpeedObject(LibraryUtil.OBD_NOT_AVAILABLE)
                obdDataListener.onSensorChanged(engineSpeedObject)
            }
            LibraryUtil.FUEL_CONSUMPTION_RATE -> {
                val fuelConsumptionObject = FuelConsumptionRateObject(LibraryUtil.OBD_NOT_AVAILABLE)
                obdDataListener.onSensorChanged(fuelConsumptionObject)
            }
            LibraryUtil.FUEL_TANK_LEVEL_INPUT -> {
                val fuelLevelObject = FuelLevelObject(LibraryUtil.OBD_NOT_AVAILABLE)
                obdDataListener.onSensorChanged(fuelLevelObject)
            }
            LibraryUtil.FUEL_TYPE -> {
                val fuelTypeObject = FuelTypeObject(LibraryUtil.OBD_NOT_AVAILABLE)
                obdDataListener.onSensorChanged(fuelTypeObject)
            }
            LibraryUtil.ENGINE_TORQUE -> {
                val engineTorqueObject = EngineTorqueObject(LibraryUtil.OBD_NOT_AVAILABLE)
                obdDataListener.onSensorChanged(engineTorqueObject)
            }
            else -> Timber.tag(TAG).d("Sensor type not recognized")
        }
    }

    fun notifyInitializationFailed(obdDataListener: ObdDataListener) {
        obdDataListener.onInitializationFailedWarning()
    }

    private val utcTime: String
        private get() {
            val df = DateFormat.getTimeInstance()
            df.timeZone = TimeZone.getTimeZone("gmt")
            return df.format(Date())
        }

    /**
     * retrieves the command that needs to be triggered for
     * a specific sensor type
     *
     * @param sensorType
     * @return
     */
    fun getCommandFromSensorType(sensorType: String?): String? {
        return when (sensorType) {
            LibraryUtil.SPEED -> OBDConstants.CMD_SPEED
            LibraryUtil.RPM -> OBDConstants.CMD_RPM
            LibraryUtil.FUEL_TANK_LEVEL_INPUT -> OBDConstants.CMD_FUEL_TANK_LEVEL_INPUT
            LibraryUtil.FUEL_CONSUMPTION_RATE -> OBDConstants.CMD_FUEL_CONSUMPTION_RATE
            LibraryUtil.FUEL_TYPE -> OBDConstants.CMD_FUEL_TYPE
            LibraryUtil.ENGINE_TORQUE -> OBDConstants.CMD_ENGINE_TORQUE
            LibraryUtil.VEHICLE_ID -> OBDConstants.CMD_VIN
            else -> null
        }
    }

    private fun getSpeed(responseData: String): SpeedObject {
        val speedSensor = SpeedObdSensor()
        val speedValueInt: Int? = speedSensor.convertValue(responseData)
        return if (speedValueInt == null) {
            ObdSpeedObject(0, LibraryUtil.OBD_READ_FAILURE)
        } else {
            Timber.tag(TAG).d("speedValueTransformed: km/h: $speedValueInt. Time: $utcTime")
            ObdSpeedObject(speedValueInt, LibraryUtil.OBD_READ_SUCCESS)
        }
    }

    private fun getEngineSpeed(responseData: String): EngineSpeedObject? {
        val rpmObdSensor = RpmObdSensor()
        val rpmValueTransformed: Double? = rpmObdSensor.convertValue(responseData)
        return if (rpmValueTransformed == null) {
            ObdEngineSpeedObject(0.0, LibraryUtil.OBD_READ_FAILURE)
        } else {
            Timber.tag(TAG).d("rpmValueTransformed: $rpmValueTransformed. Time: $utcTime")
            ObdEngineSpeedObject(rpmValueTransformed, LibraryUtil.OBD_READ_SUCCESS)
        }
    }

    private fun getFuelLevel(responseData: String): FuelLevelObject {
        val fuelTankLevel = FuelTankLevelInputObdSensor()
        val fuelTankLevelTransformed: Double? = fuelTankLevel.convertValue(responseData)
        return if (fuelTankLevelTransformed == null) {
            FuelLevelObject(0.0, LibraryUtil.OBD_READ_FAILURE)
        } else {
            Timber.tag(TAG).d("fuelTankValueTransformed: $fuelTankLevelTransformed. Time: $utcTime")
            FuelLevelObject(fuelTankLevelTransformed, LibraryUtil.OBD_READ_SUCCESS)
        }
    }

    private fun getFuelType(responseData: String): FuelTypeObject? {
        val typeObdSensor = FuelTypeObdSensor()
        val fuelTypeValueTransformed: Int? = typeObdSensor.convertValue(responseData)
        return if (fuelTypeValueTransformed == null || fuelTypeValueTransformed <= OBDConstants.FUEL_TYPES.size - 1) {
            FuelTypeObject(null, LibraryUtil.OBD_READ_FAILURE)
        } else {
            Timber.tag(TAG).d("fuelTypeValueTransformed: $fuelTypeValueTransformed. Time: $utcTime")
            FuelTypeObject(OBDConstants.FUEL_TYPES[fuelTypeValueTransformed], LibraryUtil.OBD_READ_SUCCESS)
        }
    }

    private fun getFuelConsumptionRate(responseData: String): FuelConsumptionRateObject? {
        // TODO - To be investigated based on fuel type
        val fuelConsumptionRate = FuelConsumptionRateObdSensor()
        val fuelConsumptionTransformed: Double? = fuelConsumptionRate.convertValue(responseData)
        Timber.tag(TAG).d("fuelConsumptionRateValueTransformed: $fuelConsumptionTransformed. Time: $utcTime")
        return if (fuelConsumptionTransformed == null) {
            FuelConsumptionRateObject(0.0, LibraryUtil.OBD_READ_FAILURE)
        } else {
            FuelConsumptionRateObject(fuelConsumptionTransformed, LibraryUtil.OBD_READ_SUCCESS)
        }
    }

    private fun getEngineTorque(responseData: String): EngineTorqueObject {
        val engineTorque = EngineTorqueObdSensor()
        val engineTorqueValueTransformed: Int? = engineTorque.convertValue(responseData)
        return if (engineTorqueValueTransformed == null) {
            return EngineTorqueObject(0, LibraryUtil.OBD_READ_FAILURE)
        } else {
            Timber.tag(TAG).d("engineTorqueValueTransformed: $engineTorqueValueTransformed. Time: $utcTime")
            EngineTorqueObject(engineTorqueValueTransformed, LibraryUtil.OBD_READ_SUCCESS)
        }
    }

    private fun getVehicleId(responseData: String): VinObject? {
        val vinObdSensor = VinObdSensor()
        val vinValueTransformed: String? = vinObdSensor.convertValue(responseData)
        return if (vinValueTransformed != null && vinValueTransformed.length == 17) {
            Timber.tag(TAG).d("vinValueTransformed: $vinValueTransformed. Time: $utcTime")
            VinObject(vinValueTransformed, LibraryUtil.OBD_READ_SUCCESS)
        } else
            null
    }
}