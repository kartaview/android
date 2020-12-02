package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the gyroscope values of the device. All values are in radians/second and measure the rate of rotation around the device's local X, Y and Z axis.
 * The coordinate system is the same as is used for the acceleration
 * sensor. Rotation is positive in the counter-clockwise direction. That is, an observer looking from some positive location on the x, y or z axis
 * at a device positioned on the origin would report positive rotation if the device appeared to be rotating counter clockwise.
 */
class GyroscopeObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.GYROSCOPE) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}