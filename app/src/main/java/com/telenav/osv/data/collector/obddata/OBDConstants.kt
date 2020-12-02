package com.telenav.osv.data.collector.obddata

/**
 * Created by ovidiuc2 on 11/3/16.
 */
object OBDConstants {
    /**
     * ELM327 commands sent to the obd2
     * the final 1 is because of this: it will wait till 1 answer comes in, and it will directly send it back
     */
    /**
     * command for speed
     */
    const val CMD_SPEED = "010D1"

    /**
     * command for RPM - Revolutions Per Minute
     */
    const val CMD_RPM = "010C1"

    /**
     * command for the fuel tank level input(represented as a percentage)
     */
    const val CMD_FUEL_TANK_LEVEL_INPUT = "012F1"

    /**
     * command for the fuel type
     */
    const val CMD_FUEL_TYPE = "01511"

    /**
     * command for consumption rate(measured in l/h)
     */
    const val CMD_FUEL_CONSUMPTION_RATE = "015E1"

    /**
     * engine reference torque(measured in Nm)
     */
    const val CMD_ENGINE_TORQUE = "01631"

    /**
     * vehicle identification number
     */
    const val CMD_VIN = "0902"

    /**
     * Prefix of the response that comes from speed sensor
     */
    const val PREFIX_RESPONSE_SPEED = "410D"

    /**
     * Prefix of the response that comes from rpm sensor
     */
    const val PREFIX_RESPONSE_RPM = "410C"

    /**
     * Prefix of the response that comes from tank level sensor
     */
    const val PREFIX_RESPONSE_TANK_LEVEL = "412F"

    /**
     * Prefix of the response that comes from fuel type sensor
     */
    const val PREFIX_RESPONSE_FUEL_TYPE = "4151"

    /**
     * Prefix of the response that comes from fuel consumption sensor
     */
    const val PREFIX_RESPONSE_FUEL_CONSUMPTION = "415E"
    const val PREFIX_RESPONSE_0100 = "4100"
    const val PREFIX_RESPONSE_0120 = "4120"
    const val PREFIX_RESPONSE_0140 = "4140"
    const val PREFIX_RESPONSE_0160 = "4160"
    const val OPTIONAL_VIN_INFO = "014"

    /**
     * message returned by OBD when computation is not completed yet
     */
    const val SEARCHING = "SEARCHING"
    const val CAN_ERROR = "CAN ERROR"
    const val STOPPED = "STOPPED"

    /**
     * Prefix of the response that comes from vehicle identification number requests
     */
    const val PREFIX_RESPONSE_VIN = "0:4902"

    /**
     * Prefix of the response that comes from torque sensor
     */
    const val PREFIX_RESPONSE_TORQUE_VALUE = "4163"
    const val PREFIX_RESPONSE_MODE1_PID = "41"
    const val PREFIX_RESPONSE_MODE9_PID = "49"

    /**
     * commands that retrieve sensor availability
     */
    const val CMD_0100 = "0100"
    const val CMD_0120 = "0120"
    const val CMD_0140 = "0140"
    const val CMD_0160 = "0160"
    const val CMD_0180 = "0180"
    const val CMD_0900 = "09001"
    const val WIFI_DESTINATION_ADDRESS = "192.168.0.10"
    const val DEST_PORT = 35000
    const val RPM_INDEX = 11
    const val SPEED_INDEX = 12
    const val FUEL_TANK_LEVEL_INPUT_INDEX = 14
    const val FUEL_TYPE_INDEX = 16
    const val FUEL_CONSUMPTION_INDEX = 29
    const val ENGINE_TORQUE_INDEX = 2
    const val VIN_INDEX = 1
    val FUEL_TYPES = arrayOf(
            "Not available",  //0
            "Gasoline",  //1
            "Methanol",  //2
            "Ethanol",  //3
            "Diesel",  //4
            "LPG",  //5
            "CNG",  //6
            "Propane",  //7
            "Electric",  //8
            "Bifuel running Gasoline",  //9
            "Bifuel running Methanol",  //10
            "Bifuel running Ethanol",  //11
            "Bifuel running LPG",  //12
            "Bifuel running CNG",  //13
            "Bifuel running Propane",  //14
            "Bifuel running Electricity",  //15
            "Bifuel running electric and combustion engine",  //16
            "Hybrid gasoline",  //17
            "Hybrid Ethanol",  //18
            "Hybrid Diesel",  //19
            "Hybrid electric",  //20
            "Hybrid running electric and combustion engine",  //21
            "Hybrid Regenerative",  //22
            "Bifuel running diesel" //23
    )
}