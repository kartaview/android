package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the north rotation vector value of the device
 */
class RotationVectorNorthObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.ROTATION_VECTOR_NORTH_REFERENCE) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}