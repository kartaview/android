package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

class PhoneGpsSpeedObject(speed: Int, statusCode: Int) : SpeedObject(speed, statusCode, LibraryUtil.PHONE_GPS_SPEED) {
    constructor(statusCode: Int) : this(0, statusCode)

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}