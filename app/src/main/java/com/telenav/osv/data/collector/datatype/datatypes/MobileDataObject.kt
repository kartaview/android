package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves mobile data connection type
 */
class MobileDataObject(connectionType: String?, statusCode: Int) : BaseObject<String?>(connectionType, statusCode, LibraryUtil.MOBILE_DATA) {
    /**
     * Returns the type of th mobile data connection
     * @return
     */
    val connectionType: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}