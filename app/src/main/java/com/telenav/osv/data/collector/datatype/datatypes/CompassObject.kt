package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class used for retrieving the magnetic field.
 * All values are in micro-Tesla (uT) and measure the ambient magnetic field in the X, Y and Z axis.
 */
class CompassObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.MAGNETIC) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}