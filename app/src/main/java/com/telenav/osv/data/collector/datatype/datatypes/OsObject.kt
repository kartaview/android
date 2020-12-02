package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the operating system version
 */
class OsObject(osVersion: String?, statusCode: Int) : BaseObject<String?>(osVersion, statusCode, LibraryUtil.OS_INFO) {
    val osVersion: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}