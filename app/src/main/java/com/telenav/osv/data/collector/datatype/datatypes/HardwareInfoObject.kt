package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the following hardware information: device model and manufacturer
 */
class HardwareInfoObject(var model: String, statusCode: Int) : BaseObject<HardwareInfoObject?>(null, statusCode, LibraryUtil.HARDWARE_TYPE) {

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}