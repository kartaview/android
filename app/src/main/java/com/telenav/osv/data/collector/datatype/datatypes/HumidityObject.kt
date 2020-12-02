package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the relative ambient air humidity as a percentage
 */
class HumidityObject(humidity: Float, statusCode: Int) : BaseObject<Float?>(humidity, statusCode, LibraryUtil.HUMIDITY) {
    constructor(statusCode: Int) : this(Float.MIN_VALUE, statusCode) {}

    /**
     * Returns the relative ambient humidity in percent (%).
     */
    val humidity: Float
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}