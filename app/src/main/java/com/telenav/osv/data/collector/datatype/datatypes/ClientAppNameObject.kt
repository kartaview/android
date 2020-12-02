package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class used for returning the name of this application's package.
 */
class ClientAppNameObject(clientAppName: String?, statusCode: Int) : BaseObject<String?>(clientAppName, statusCode, LibraryUtil.CLIENT_APP_NAME) {
    /**
     * Returns client app name
     * @return
     */
    val clientAppName: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}