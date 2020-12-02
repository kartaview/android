package com.telenav.osv.recorder.metadata.model.body

import com.telenav.osv.recorder.metadata.TemplateID

class MetadataBodyGps(timestamp: String, latitude: Double, longitude: Double, elevation: Double, horizonAccuracy: Float, verticalAccuracy: Float, speed: Float) :
        MetadataBodyBase(timestamp, TemplateID.GPS, arrayOf(latitude, longitude, elevation, horizonAccuracy, verticalAccuracy, speed))

class MetadataBodyDevice(timeStamp: String, platform: String, osRawName: String, osVersion: String, deviceRawName: String, appVersion: String, appBuildNumber: String, recordingType: String) :
        MetadataBodyBase(timeStamp, TemplateID.DEVICE, arrayOf(platform, osRawName, osVersion, deviceRawName, appVersion, appBuildNumber, recordingType))

class MetadataBodyObd(timeStamp: String, obdSpeed: Int) :
        MetadataBodyBase(timeStamp, TemplateID.OBD, arrayOf(obdSpeed))

class MetadataBodyPressure(timeStamp: String, pressure: Float) :
        MetadataBodyBase(timeStamp, TemplateID.PRESSURE, arrayOf(pressure))

class MetadataBodyAttitude(timeStamp: String, yaw: Float, pitch: Float, roll: Float) :
        MetadataBodyBase(timeStamp, TemplateID.ATTITUDE, arrayOf(yaw, pitch, roll))

class MetadataBodyAcceleration(timeStamp: String, accelerationX: Float, accelerationY: Float, accelerationZ: Float) :
        MetadataBodyBase(timeStamp, TemplateID.ACCELERATION, arrayOf(accelerationX, accelerationY, accelerationZ))

class MetadataBodyGravity(timeStamp: String, gravityX: Float, gravityY: Float, gravityZ: Float) :
        MetadataBodyBase(timeStamp, TemplateID.GRAVITY, arrayOf(gravityX, gravityY, gravityZ))

class MetadataBodyCompass(timeStamp: String, compass: Float) :
        MetadataBodyBase(timeStamp, TemplateID.COMPASS, arrayOf(compass))

class MetadataBodyCameraExif(timeStamp: String, focalLength: Float, cameraWidth: Int, cameraHeight: Int) :
        MetadataBodyBase(timeStamp, TemplateID.EXIF, arrayOf(focalLength, cameraWidth, cameraHeight))

class MetadataBodyCamera(timeStamp: String, horizontalFiledOfView: Double, verticalFieldOfView: Double, lensAperture: Float) : MetadataBodyBase(timeStamp, TemplateID.CAMERA, arrayOf(horizontalFiledOfView, verticalFieldOfView, lensAperture))

class MetadataPhotoVideo(timeStamp: String, videoIndex: Int, frameIndex: Int, gpsTimestamp: String, latitude: Double, longitude: Double, horizonAccuracy: Float, gpsSpeed: Float, compassTimeStamp: String?, compass: Float?, obdTimestamp: String?, obd2Speed: Int?) :
        MetadataBodyBase(timeStamp, TemplateID.PHOTO, arrayOf(videoIndex, frameIndex, gpsTimestamp, latitude, longitude, horizonAccuracy, gpsSpeed, compassTimeStamp, compass, obdTimestamp, obd2Speed))