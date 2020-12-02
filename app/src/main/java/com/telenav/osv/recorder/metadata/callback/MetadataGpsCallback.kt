package com.telenav.osv.recorder.metadata.callback

import android.location.Location

interface MetadataGpsCallback {
    fun onGpsLog(location: Location)
}