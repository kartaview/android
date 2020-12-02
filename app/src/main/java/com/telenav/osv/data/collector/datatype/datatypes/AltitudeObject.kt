package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Get the altitude if available, in meters above the WGS 84 reference ellipsoid.
 * If this location does not have an altitude then 0.0 is returned.
 */
class AltitudeObject(altitude: Double, statusCode: Int) : BaseObject<Double?>(altitude, statusCode, LibraryUtil.PHONE_GPS_ALTITUDE) {
    constructor(statusCode: Int) : this(Double.MIN_VALUE, statusCode) {}

    /**
     * Returns the altitude in meters above the WGS 84 reference ellipsoid.
     */
    val altitude: Double
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}