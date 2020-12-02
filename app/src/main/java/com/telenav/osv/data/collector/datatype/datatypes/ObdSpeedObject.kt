package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

class ObdSpeedObject(speed: Int, statusCode: Int) : SpeedObject(speed, statusCode, LibraryUtil.SPEED) {
    constructor(statusCode: Int) : this(0, statusCode)

    init {
        dataSource = LibraryUtil.OBD_SOURCE
    }
}