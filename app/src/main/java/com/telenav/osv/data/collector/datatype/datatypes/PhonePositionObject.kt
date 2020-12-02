package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

class PhonePositionObject(statusCode: Int) : PositionObject(statusCode, LibraryUtil.PHONE_GPS) {
    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}