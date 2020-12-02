package com.telenav.osv.recorder.metadata.callback

import android.location.Location

interface MetadataPhotoVideoCallback {
    fun onPhotoVideoCallback(timestamp:Long, frameIndex: Int, videoIndex: Int, location: Location)
}