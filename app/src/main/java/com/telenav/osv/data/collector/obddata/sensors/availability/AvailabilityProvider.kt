package com.telenav.osv.data.collector.obddata.sensors.availability

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

/**
 * Class that provides an availability map, indicating which of the requested sensors are available
 */
internal class AvailabilityProvider(private val availability: AbstractSensorAvailability) : Callable<Map<String, Boolean>> {
    @Throws(Exception::class)
    override fun call(): Map<String, Boolean> {
        val result: MutableMap<String, Boolean> = ConcurrentHashMap()
        for (sensorType in availability.listOfAvailabilitySensorGroups) {
            result[sensorType] = availability.isSensorAvailable(sensorType)
        }
        return result
    }
}