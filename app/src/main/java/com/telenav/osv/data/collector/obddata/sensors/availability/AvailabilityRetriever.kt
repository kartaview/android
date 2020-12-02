package com.telenav.osv.data.collector.obddata.sensors.availability

import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

/**
 * retrieves the availability of a given set of OBD sensors
 * the list of requested sensors can be given through the constructor of any [AbstractSensorAvailability]
 */
object AvailabilityRetriever {
    private val TAG = AvailabilityRetriever::class.java.simpleName
    fun retrieveAvailabilityMap(abstractSensorAvailability: AbstractSensorAvailability?): Map<String, Boolean>? {
        val service = Executors.newSingleThreadExecutor()
        val future = service.submit(AvailabilityProvider(abstractSensorAvailability!!))
        try {
            return if (!Thread.currentThread().isInterrupted) {
                future.get()
            } else null
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e(e, "Exception when retrieving availability map")
            Thread.currentThread().interrupt()
        } catch (e: ExecutionException) {
            Timber.tag(TAG).e(e, "Exception when retrieving availability map")
            Thread.currentThread().interrupt()
        }
        return null
    }
}