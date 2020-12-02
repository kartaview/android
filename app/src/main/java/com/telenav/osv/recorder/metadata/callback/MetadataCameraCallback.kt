package com.telenav.osv.recorder.metadata.callback

interface MetadataCameraCallback {
    fun onCameraSensorCallback(timestamp: Long, focalLength: Float, horizontalFieldOfView: Double, verticalFieldOfView: Double, lensAperture: Float, cameraWidth: Int, cameraHeight: Int)
}