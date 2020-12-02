package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the raw rotation vector value of the device
 */
class RotationVectorRawObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.ROTATION_VECTOR_RAW) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}