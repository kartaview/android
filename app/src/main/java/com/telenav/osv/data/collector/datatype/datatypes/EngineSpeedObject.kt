package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData

/**
 * Class which retrieves the vehicle engine speed(rotations per minute)
 */
abstract class EngineSpeedObject internal constructor(engineSpeed: Double, statusCode: Int, @AvailableData sensorType: String?) : BaseObject<Double?>(engineSpeed, statusCode, sensorType!!) {
    /**
     * Returns the current engine rotation in rpm (rotation per minute)
     * @return Engine rotation in rotation per minute
     */
    val engineSpeed: Double
        get() = actualValue!!
}