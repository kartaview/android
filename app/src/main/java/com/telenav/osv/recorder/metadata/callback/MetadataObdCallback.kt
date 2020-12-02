package com.telenav.osv.recorder.metadata.callback

interface MetadataObdCallback {
    fun onObdCallback(timeStamp: Long, speed: Int)
}