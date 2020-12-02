package com.telenav.osv.data.collector.datatype.util

/**
 * Created by denisatrif on 9/19/16.
 * Converts different values of the sensors
 */
object DataTypeUtil {
    /**
     * Constant used for converting km/h to m/s
     */
    private const val METERS_PER_SECOND = 1 / 3.6

    /**
     * Converts speed from km/h to m/s
     *
     * @param speedInKmPerHour - Speed in km/h
     * @return Speed in m/s
     */
    fun convertKilometersPerHourToMetersPerSeconds(speedInKmPerHour: Double): Double {
        return speedInKmPerHour * METERS_PER_SECOND
    }
}