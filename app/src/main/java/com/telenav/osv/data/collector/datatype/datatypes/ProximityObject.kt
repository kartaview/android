package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which uses the proximity sensor to retrieve whether or not an object is close the it
 */
class ProximityObject(isClose: Boolean, statusCode: Int) : BaseObject<Boolean?>(isClose, statusCode, LibraryUtil.PROXIMITY) {
    constructor(statusCode: Int) : this(false, statusCode) {}

    val isClose: Boolean
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}