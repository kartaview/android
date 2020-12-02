package com.telenav.osv.recorder.metadata.model.body

import com.telenav.osv.recorder.metadata.TemplateID
import com.telenav.osv.utils.StringUtils

abstract class MetadataBodyBase(val timeStamp: String, val templateId: TemplateID, val fields: Array<Any?>, val version: Int = METADATA_VERSION, val versionMinCompatible: Int = METADATA_VERSION_COMPATIBLE_MIN) {
    override fun toString(): String {
        if (timeStamp.isEmpty()) {
            return StringUtils.EMPTY_STRING
        }

        var loggedResponse = "$timeStamp$COLON${templateId.value}${COLON}"
        for (field in fields) {
            loggedResponse += "${field?.toString() ?: StringUtils.EMPTY_STRING}${DELIMITER}"
        }
        loggedResponse = loggedResponse.removeRange(loggedResponse.length - 1, loggedResponse.length)

        return "$loggedResponse\n"
    }

    companion object {
        const val METADATA_VERSION = 1
        const val METADATA_VERSION_COMPATIBLE_MIN = 1
        const val DELIMITER = ";"
        const val COLON = ":"
    }
}