package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the ambient (room) temperature in degree Celsius.
 */
class TemperatureObject(temperature: Float, statusCode: Int) : BaseObject<Float?>(temperature, statusCode, LibraryUtil.TEMPERATURE) {
    constructor(statusCode: Int) : this(Float.MIN_VALUE, statusCode) {}

    /**
     * Returns the temperature value in Celsius
     */
    val temperature: Float
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}