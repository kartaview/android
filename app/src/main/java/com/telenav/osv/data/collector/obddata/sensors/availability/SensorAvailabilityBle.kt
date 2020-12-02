package com.telenav.osv.data.collector.obddata.sensors.availability

import com.telenav.osv.data.collector.obddata.ClientDataTransmissionBle

/**
 * Created by adrianb2 on 11/11/16.
 */
/**
 * handles the sensor availability when the connection is ble
 */
class SensorAvailabilityBle(private val clientDataTransmissionBle: ClientDataTransmissionBle) : AbstractSensorAvailability() {
    override fun isSensorAvailable(sensorType: String?): Boolean {
        getCommand(sensorType)?.let {
            //if we know that this sensor belongs to a group that has no sensors available, return false and do not attempt retries
            if (NoDataDetector.instance.wasNoDataDetected(it)) {
                return false
            }
            val wasAvailabilityDetermined = retrieveCalculatedAvailability(it, sensorType!!)
            if (wasAvailabilityDetermined != null) {
                return wasAvailabilityDetermined
            }
            clientDataTransmissionBle.writeCommand(it)
            val response: String? = clientDataTransmissionBle.getCharacteristicResult()
            return if (response != null) {
                getAvailability(it, response, sensorType)
            } else false
        }

        return false
    }

}