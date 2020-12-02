package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the device gravity.
 * A three dimensional vector indicating the direction and magnitude of gravity. Units are m/s^2.
 * The coordinate system is the same as is used by the acceleration sensor.
 */
class GravityObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.GRAVITY) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}