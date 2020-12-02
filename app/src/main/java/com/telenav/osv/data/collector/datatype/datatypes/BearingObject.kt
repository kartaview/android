package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class used for retrieving the bearing, in degrees.
 * Bearing is the horizontal direction of travel of this device, and is not related to the device orientation. It is guaranteed to be in the range (0.0, 360.0] if the device has
 * a bearing.
 * If this location does not have a bearing then 0.0 is returned.
 */
class BearingObject(bearing: Float, statusCode: Int) : BaseObject<Float?>(bearing, statusCode, LibraryUtil.PHONE_GPS_BEARING) {
    constructor(statusCode: Int) : this(Float.MIN_VALUE, statusCode)

    /**
     * Returns the bearing values in degrees
     */
    val bearing: Float?
        get() = actualValue

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}