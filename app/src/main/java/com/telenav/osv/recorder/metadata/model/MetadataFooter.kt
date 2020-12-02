package com.telenav.osv.recorder.metadata.model

import androidx.annotation.VisibleForTesting

class MetadataFooter() {
    override fun toString(): String {
        return IDENTIFIER_END
    }

    @VisibleForTesting
    private companion object {
        const val IDENTIFIER_END = "END"
    }
}