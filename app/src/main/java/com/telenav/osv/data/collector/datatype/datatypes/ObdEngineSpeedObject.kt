package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

class ObdEngineSpeedObject(engineSpeed: Double, statusCode: Int) : EngineSpeedObject(engineSpeed, statusCode, LibraryUtil.RPM) {
    constructor(statusCode: Int) : this(Double.MIN_VALUE, statusCode) {}

    init {
        dataSource = LibraryUtil.OBD_SOURCE
    }
}