package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 *
 */
class HeadingObject(statusCode: Int) : ThreeAxesObject(statusCode, LibraryUtil.HEADING) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}