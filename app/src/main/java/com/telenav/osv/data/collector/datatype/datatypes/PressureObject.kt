package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the atmospheric pressure in hPa (millibar)
 */
class PressureObject(pressure: Float, statusCode: Int) : BaseObject<Float?>(pressure, statusCode, LibraryUtil.PRESSURE) {
    constructor(statusCode: Int) : this(Float.MIN_VALUE, statusCode) {}

    /**
     * Returns the atmospheric pressure in hPa (millibar)
     */
    val pressure: Float
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}