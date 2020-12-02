package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData

/**
 * This class is used for encapsulating information read from sensors that
 * provide information on three axes.
 *
 *
 * These sensors are: Accelerometer, Magnetic, Raw Rotation Data(Game Rotation sensor),
 * Gravity, Gyroscope, Heading, Linear Acceleration, Rotation Data North Reference(Rotation Vector sensor)
 */
abstract class ThreeAxesObject protected constructor(statusCode: Int, @AvailableData sensorType: String?) : BaseObject<ThreeAxesObject?>(null, statusCode, sensorType!!) {
    /**
     * The acceleration force in m/s^2 on all three axes
     */
    private var xValue = 0f
    private var yValue = 0f
    private var zValue = 0f
    fun getzValue(): Float {
        return zValue
    }

    fun setzValue(zValue: Float) {
        this.zValue = zValue
    }

    fun getyValue(): Float {
        return yValue
    }

    fun setyValue(yValue: Float) {
        this.yValue = yValue
    }

    fun getxValue(): Float {
        return xValue
    }

    fun setxValue(xValue: Float) {
        this.xValue = xValue
    }
}