package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData

/**
 * class which retrieves the vehicle speed. Depending on the implementation of this abstract class, speed
 * from OBD or OpenXC will be retrieved
 */
abstract class SpeedObject protected constructor(speed: Int, statusCode: Int, @AvailableData sensorType: String?) : BaseObject<Int?>(speed, statusCode, sensorType!!) {
    /**
     * Returns the current speed in m/s
     * @return Speed in m/s
     */
    open val speed: Int
        get() = actualValue!!
}