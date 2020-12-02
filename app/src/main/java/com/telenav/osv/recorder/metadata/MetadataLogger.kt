package com.telenav.osv.recorder.metadata

import android.location.Location
import com.telenav.osv.data.collector.datatype.datatypes.ObdSpeedObject
import com.telenav.osv.data.collector.datatype.datatypes.PressureObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.recorder.metadata.model.MetadataFooter
import com.telenav.osv.recorder.metadata.model.MetadataHeader
import com.telenav.osv.recorder.metadata.model.body.MetadataBodyBase

class MetadataLogger {

    private val metadataConverterLog: MetadataConverterLog = MetadataConverterLog()

    fun headerWithBody(): String {
        return MetadataHeader().toString()
    }

    fun footer(): String {
        return MetadataFooter().toString()
    }

    fun body(metadataBody: MetadataBodyBase): String {
        return metadataBody.toString()
    }

    fun bodyAcceleration(threeAxesObject: ThreeAxesObject): String {
        return body(metadataConverterLog.convertAcceleration(threeAxesObject))
    }

    fun bodyCompass(threeAxesObject: ThreeAxesObject): String {
        return body(metadataConverterLog.convertCompass(threeAxesObject))
    }

    fun bodyGravity(threeAxesObject: ThreeAxesObject): String {
        return body(metadataConverterLog.convertGravity(threeAxesObject))
    }

    fun bodyAttitude(threeAxesObject: ThreeAxesObject): String {
        return body(metadataConverterLog.convertAttitude(threeAxesObject))
    }

    fun bodyObd(timestamp: Long, speed: Int): String {
        return body(metadataConverterLog.convertObdO(timestamp, speed))
    }

    fun bodyPressure(pressureObject: PressureObject): String {
        return body(metadataConverterLog.convertPressure(pressureObject))
    }

    fun bodyGps(location: Location): String {
        return body(metadataConverterLog.convertGps(location))
    }

    fun bodyPhotoVideo(timestamp: Long, videoIndex: Int, frameIndex: Int, location: Location, compassObject: ThreeAxesObject?, obdSpeedObject: ObdSpeedObject?): String {
        return body(metadataConverterLog.convertPhoto(timestamp, videoIndex, frameIndex, location, compassObject, obdSpeedObject))
    }

    fun bodyDevice(timeStamp: Long, platform: String, osRawName: String, osVersion: String, deviceRawName: String, appVersion: String, appBuildNumber: String, recordingType: String): String {
        return body(metadataConverterLog.convertDevice(timeStamp, platform, osRawName, osVersion, deviceRawName, appVersion, appBuildNumber, recordingType))
    }

    fun bodyExif(timestamp: Long, focalLength: Float, cameraWidth: Int, cameraHeight: Int): String {
        return body(metadataConverterLog.convertExif(timestamp, focalLength, cameraWidth, cameraHeight))
    }

    fun bodyCamera(timestamp: Long, horizontalFieldOfView: Double, verticalFieldOfView: Double, lensAperture: Float): String {
        return body(metadataConverterLog.convertCamera(timestamp, horizontalFieldOfView, verticalFieldOfView, lensAperture))
    }
}