package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * The version name of this package, as specified by the manifest tag's versionName attribute.
 */
class ClientAppVersionObject(clientVersionName: String?, statusCode: Int) : BaseObject<String?>(clientVersionName, statusCode, LibraryUtil.CLIENT_APP_VERSION) {
    /**
     * Returns client version
     * @return
     */
    val clientVersionName: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}