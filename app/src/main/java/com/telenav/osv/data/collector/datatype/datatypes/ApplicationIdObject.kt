package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
 * The UUID is generated using a cryptographically strong pseudo random number generator.
 */
class ApplicationIdObject(applicationId: String?, statusCode: Int) : BaseObject<String?>(applicationId, statusCode, LibraryUtil.APPLICATION_ID) {
    /**
     * Returns the application ID.
     * The application ID s lost when app is uninstaled.
     * @return
     */
    val applicationId: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}