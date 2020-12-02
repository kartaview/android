package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Created by ovidiuc2 on 12/13/16.
 */
/**
 * Class which retrieves the vehicle identification number(also known as chassis number)
 */
class VinObject(vinValue: String?, statusCode: Int) : BaseObject<String?>(vinValue, statusCode, LibraryUtil.VEHICLE_ID) {
    constructor(statusCode: Int) : this("", statusCode) {}

    val vin: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.OBD_SOURCE
    }
}