package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the ambient light level in SI lux units
 */
class LightObject(lightValue: Float, statusCode: Int) : BaseObject<Float?>(lightValue, statusCode, LibraryUtil.LIGHT) {
    constructor(statusCode: Int) : this(Float.MIN_VALUE, statusCode) {}

    /**
     * returns the ambient light level (illumination) in lx
     */
    val lightValue: Float
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}