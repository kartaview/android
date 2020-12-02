package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the number of steps taken by the user since the last reboot while activated.
 * The value is returned as a float (with the fractional part set to zero) and is reset to zero only on a system reboot.
 */
class StepCounterObject(stepCounter: Float, statusCode: Int) : BaseObject<Float?>(stepCounter, statusCode, LibraryUtil.STEP_COUNT) {
    constructor(statusCode: Int) : this(Float.MIN_VALUE, statusCode) {}

    /**
     * Returns the number of steps
     */
    val stepCounter: Float
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}