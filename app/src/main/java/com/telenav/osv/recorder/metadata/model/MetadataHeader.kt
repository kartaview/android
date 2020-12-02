package com.telenav.osv.recorder.metadata.model

import com.telenav.osv.recorder.metadata.TemplateID

class MetadataHeader() {
    override fun toString(): String {
        var headerAsString = "${IDENTIFIER_METADATA}\n" +
                "${IDENTIFIER_HEADER}\n"

        for (templateID in TemplateID.values()) {
            headerAsString += "${IDENTIFIER_FUNCTION}:${templateID.value}" +
                    "${DELIMITER}${templateID.name}" +
                    "${DELIMITER}${templateID.version}" +
                    "${DELIMITER}${templateID.minimumCompatibleVersion}" +
                    "\n"
        }

        return "${headerAsString}${IDENTIFIER_BODY}\n"
    }

    companion object {
        const val IDENTIFIER_METADATA = "METADATA:2.0"
        const val IDENTIFIER_HEADER = "HEADER"
        const val IDENTIFIER_FUNCTION = "ALIAS"
        const val IDENTIFIER_BODY = "BODY"
        const val DELIMITER = ";"
    }
}