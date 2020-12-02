package com.telenav.osv.data.collector.obddata.obdinitializer

import com.telenav.osv.data.collector.obddata.AbstractClientDataTransmission
import com.telenav.osv.data.collector.obddata.ClientDataTransmissionBle
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener

/**
 * Created by ovidiuc2 on 13.04.2017.
 */
class BleObdInitializer(deviceVersion: String?,
                        /**
                         * the transmission object used for sending commands to the OBD
                         */
                        private val abstractClientDataTransmission: AbstractClientDataTransmission, obdDataListener: ObdDataListener?) : AbstractOBDInitializer(obdDataListener, deviceVersion) {

    /**
     * sends an AT command to setup the OBD interface(spaces off, echo off)
     *
     * @param sendingCommand
     * @return - OK if the command was executed successfully
     */
    override fun getAtResponse(sendingCommand: String): String? {
        abstractClientDataTransmission.writeCommand(sendingCommand)
        delay(AT_WAITING_TIME)
        return (abstractClientDataTransmission as ClientDataTransmissionBle).getCharacteristicResult()
    }
}