package com.telenav.osv.recorder.metadata

import android.location.Location
import com.telenav.osv.data.collector.datatype.datatypes.ObdSpeedObject
import com.telenav.osv.data.collector.datatype.datatypes.PressureObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.recorder.metadata.model.body.*
import com.telenav.osv.utils.FormatUtils

class MetadataConverterLog {
    fun convertAcceleration(accelerationObject: ThreeAxesObject): MetadataBodyAcceleration {
        return MetadataBodyAcceleration(FormatUtils.getMetadataFormatTimestampFromLong(accelerationObject.timestamp),
                FormatUtils.transformSquareMetersPerSecondIntoGravity(accelerationObject.getxValue()),
                FormatUtils.transformSquareMetersPerSecondIntoGravity(accelerationObject.getyValue()),
                FormatUtils.transformSquareMetersPerSecondIntoGravity(accelerationObject.getzValue()))
    }

    fun convertGravity(gravityObject: ThreeAxesObject): MetadataBodyGravity {
        return MetadataBodyGravity(FormatUtils.getMetadataFormatTimestampFromLong(gravityObject.timestamp),
                FormatUtils.transformSquareMetersPerSecondIntoGravity(gravityObject.getxValue()),
                FormatUtils.transformSquareMetersPerSecondIntoGravity(gravityObject.getyValue()),
                FormatUtils.transformSquareMetersPerSecondIntoGravity(gravityObject.getzValue()))
    }

    fun convertAttitude(attitudeObject: ThreeAxesObject): MetadataBodyAttitude {
        return MetadataBodyAttitude(FormatUtils.getMetadataFormatTimestampFromLong(attitudeObject.timestamp),
                attitudeObject.getzValue(),
                attitudeObject.getxValue(),
                attitudeObject.getyValue())
    }

    fun convertPressure(presureObject: PressureObject): MetadataBodyPressure {
        return MetadataBodyPressure(FormatUtils.getMetadataFormatTimestampFromLong(presureObject.timestamp),
                presureObject.pressure)
    }

    fun convertObdO(timestamp: Long, speed: Int): MetadataBodyObd {
        return MetadataBodyObd(FormatUtils.getMetadataFormatTimestampFromLong(timestamp),
                speed)
    }

    fun convertGps(location: Location): MetadataBodyGps {
        return MetadataBodyGps(FormatUtils.getMetadataFormatTimestampFromLong(location.time),
                location.latitude,
                location.longitude,
                location.altitude,
                location.accuracy,
                location.accuracy,
                location.speed)
    }

    fun convertCompass(compassObject: ThreeAxesObject): MetadataBodyCompass {
        return MetadataBodyCompass(FormatUtils.getMetadataFormatTimestampFromLong(compassObject.timestamp),
                compassObject.getzValue())
    }

    fun convertPhoto(timestamp: Long, videoIndex: Int, frameIndex: Int, location: Location, compassObject: ThreeAxesObject?, obdSpeedObject: ObdSpeedObject?): MetadataPhotoVideo {
        return MetadataPhotoVideo(FormatUtils.getMetadataFormatTimestampFromLong(timestamp),
                videoIndex,
                frameIndex,
                FormatUtils.getMetadataFormatTimestampFromLong(location.time),
                location.latitude,
                location.longitude,
                location.accuracy,
                location.speed,
                if (compassObject != null) {
                    FormatUtils.getMetadataFormatTimestampFromLong(compassObject.timestamp)
                } else null,
                compassObject?.getzValue(),
                if (obdSpeedObject != null) {
                    FormatUtils.getMetadataFormatTimestampFromLong(obdSpeedObject.timestamp)
                } else null,
                obdSpeedObject?.speed)
    }

    fun convertExif(timestamp: Long, focalLength: Float, cameraWidth: Int, cameraHeight: Int): MetadataBodyCameraExif {
        return MetadataBodyCameraExif(FormatUtils.getMetadataFormatTimestampFromLong(timestamp), focalLength, cameraWidth, cameraHeight)
    }

    fun convertCamera(timestamp: Long, horizontalFieldOfView: Double, verticalFieldOfView: Double, lensAperture: Float): MetadataBodyCamera {
        return MetadataBodyCamera(FormatUtils.getMetadataFormatTimestampFromLong(timestamp), horizontalFieldOfView, verticalFieldOfView, lensAperture)
    }

    fun convertDevice(timestamp: Long, platform: String, osRawName: String, osVersion: String, deviceRawName: String, appVersion: String, appBuildNumber: String, recordingType: String): MetadataBodyDevice {
        return MetadataBodyDevice(FormatUtils.getMetadataFormatTimestampFromLong(timestamp), platform, osRawName, osVersion, deviceRawName, appVersion, appBuildNumber, recordingType)
    }
}