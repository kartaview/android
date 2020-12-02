package com.telenav.osv.data.collector.obddata.obdinitializer

/**
 * Created by ovidiuc2 on 10/26/16.
 */
object ATConstants {
    /**
     * Returns the device version
     */
    const val Z = "AT Z"

    /**
     * Spaces off - Removes the spaces from future OBD commands
     */
    const val S0 = "AT S0"

    /**
     * Echo off - Disables the echo setting of OBD responses
     */
    const val E0 = "AT E0"

    /**
     * Describe vehicle protocol
     */
    const val DP = "AT DP"

    /**
     * Headers on
     */
    const val H1 = "AT H1"

    /**
     * Headers off
     */
    const val H0 = "AT H0"
    const val L0 = "AT L0"
    const val D = "AT D"

    /**
     * sets request header. takes as a parameter the CAN ID of the ECU we want to listen to.
     * for example, AT SH 7E0 will set the header of requests to 7E0, so that only responses from ECU#1 will be returned
     */
    const val SH = "AT SH "
    const val PROTOCOL_SAE_J1850 = "SAEJ1850"
    const val PROTOCOL_ISO9141_2 = "ISO9141-2"
    const val PROTOCOL_ISO14230_4_KWP = "ISO14230-4KWP"
    const val PROTOCOL_ISO15765_4 = "ISO15765-4"
    const val PROTOCOL_SAE_J1939 = "SAEJ1939"
    const val PROTOCOL_USER1_CAN = "User1CAN"
    const val PROTOCOL_USER2_CAN = "User2CAN"

    //Connection types
    const val CONNECTION_WIFI = "wifi"
    const val CONNECTION_BLUETOOTH = "bluetooth"
    const val CONNECTION_BLE = "ble"

    //AT versions
    const val V1_5 = "v1.5"
    const val V2_1 = "v2.1"
}