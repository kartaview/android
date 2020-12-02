package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the accelerometer sensor value.
 * A sensor of this type measures the acceleration applied to the device.
 */
class AccelerometerObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.ACCELEROMETER) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}