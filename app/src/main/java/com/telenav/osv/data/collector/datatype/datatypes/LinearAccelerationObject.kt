package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the linear acceleration of the device
 */
class LinearAccelerationObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.LINEAR_ACCELERATION) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}