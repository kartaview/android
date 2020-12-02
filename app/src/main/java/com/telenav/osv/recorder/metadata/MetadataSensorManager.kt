package com.telenav.osv.recorder.metadata

import android.content.Context
import android.location.Location
import com.telenav.osv.data.collector.config.Config
import com.telenav.osv.data.collector.datatype.EventDataListener
import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.datatype.datatypes.ObdSpeedObject
import com.telenav.osv.data.collector.datatype.datatypes.PressureObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.manager.DataCollectorManager
import com.telenav.osv.item.KVFile
import com.telenav.osv.recorder.metadata.callback.*
import timber.log.Timber

/**
 * The manager for the sensor across the app which are required to be logged by metadata.
 *
 * The class itself follows the Singleton pattern in order to maintain only one instance so all the data will be written in the order it is received.
 *
 * It uses [DataCollectorManager] as a dependency to get phone sensor data in the metadata file.
 *
 * Available lifecycle methods:
 * * [start]
 * * [stop]
 *
 * Available sensor logging methods (the sensors required by the metadata which are not existent in the data collector dependency):
 * * [onPhotoVideoCallback]
 * * [onObdCallback]
 * * [onCameraSensorCallback]
 * * [onDeviceLog]
 * * [onGpsLog]
 */
class MetadataSensorManager private constructor() : EventDataListener, MetadataObdCallback, MetadataPhotoVideoCallback, MetadataCameraCallback, MetadataGpsCallback {
    private val metadataWriter: MetadataWriter = MetadataWriter()

    private val metadataLogger: MetadataLogger = MetadataLogger()

    private var dataCollector: DataCollectorManager? = null

    private var photoCachedData: PhotoCachedData = PhotoCachedData(null, null)

    var listener: MetadataWrittingStatusCallback? = null

    private object HOLDER {
        val INSTANCE = MetadataSensorManager()
    }

    fun create(parentFolder: KVFile, context: Context) {
        if (listener == null) {
            Timber.e("Listener not set!")
            return;
        }
        metadataWriter.createFile(parentFolder, listener!!, metadataLogger.headerWithBody())
        if (dataCollector == null) {
            val configBuilder = Config.Builder()
            configBuilder.addSource(LibraryUtil.PHONE_SOURCE)
                    //heading
                    .addDataListener(this, LibraryUtil.HEADING)
                    .addSensorFrequency(LibraryUtil.HEADING, LibraryUtil.F_10HZ)
                    //pressure
                    .addDataListener(this, LibraryUtil.PRESSURE)
                    .addSensorFrequency(LibraryUtil.HEADING, LibraryUtil.F_10HZ)
                    //gravity
                    .addDataListener(this, LibraryUtil.GRAVITY)
                    .addSensorFrequency(LibraryUtil.GRAVITY, LibraryUtil.F_10HZ)
                    //linear accel
                    .addDataListener(this, LibraryUtil.LINEAR_ACCELERATION)
                    .addSensorFrequency(LibraryUtil.LINEAR_ACCELERATION, LibraryUtil.F_10HZ)
                    //rotation vector
                    .addDataListener(this, LibraryUtil.ROTATION_VECTOR_RAW)
                    .addSensorFrequency(LibraryUtil.ROTATION_VECTOR_RAW, LibraryUtil.F_10HZ)
            dataCollector = DataCollectorManager(context, configBuilder.build())
            Timber.d("start. Status: create data collector manager with specific settings")
        }
    }

    /**
     * Start method which will create the metadata file which and start the phone sensor collection by using internally the [MetadataWriter.createFile].
     */
    fun start() {
        //register for the desired phone sensors, and start the data collector library
        Timber.d("start. Status: start data collector manager")
        dataCollector?.startCollecting()
    }

    /**
     * Stop method which will stop the data collector sensor gathering and signal internally the writter that the file is closed by using [MetadataWriter.finish].
     */
    fun stop() {
        dataCollector?.stopCollectingPhoneData()
        Timber.d("stop. Status: DC stopped collecting. Append footer and close file.")
        metadataWriter.finish(metadataLogger.footer())
    }

    override fun onPhotoVideoCallback(timestamp: Long, frameIndex: Int, videoIndex: Int, location: Location) {
        if (location.latitude == 0.0 && location.longitude == 0.0) {
            Timber.d("onPhotoVideoCallback. Status: error. Message: No gps found")
            listener?.onMetadataLoggingError(Exception("Gps 0.0 found"))
            return
        }
        Timber.d("onPhotoVideoCallback. Status: log photo data")
        metadataWriter.appendInFile(metadataLogger.bodyPhotoVideo(timestamp, videoIndex, frameIndex, location, photoCachedData.compassData, photoCachedData.obdSpeedObject), true)
    }

    override fun onObdCallback(timeStamp: Long, speed: Int) {
        Timber.d("onObdCallback. Status: log obd data. Timestamp: $timeStamp. Speed:$speed.")
        metadataWriter.appendInFile(metadataLogger.bodyObd(timeStamp, speed))
    }

    override fun onCameraSensorCallback(timestamp: Long, focalLength: Float, horizontalFieldOfView: Double, verticalFieldOfView: Double, lensAperture: Float, cameraWidth: Int, cameraHeight: Int) {
        Timber.d("onCameraSensorCallback. Status: log single exif data. Focal length: $focalLength")
        metadataWriter.appendInFile(metadataLogger.bodyExif(timestamp, focalLength, cameraWidth, cameraHeight))
        Timber.d("onCameraSensorCallback. Status: log camera data. Horizontal field of view: $horizontalFieldOfView. Vertical field of view: $verticalFieldOfView.")
        metadataWriter.appendInFile(metadataLogger.bodyCamera(timestamp, horizontalFieldOfView, verticalFieldOfView, lensAperture))
    }

    fun onDeviceLog(timeStamp: Long, platform: String, osRawName: String, osVersion: String, deviceRawName: String, appVersion: String, appBuildNumber: String, isVideoCompression: Boolean) {
        Timber.d("onDeviceLog. Status: log device data. Timestamp: $timeStamp. App Build number:$appBuildNumber. IsVideoCompression: $isVideoCompression")
        metadataWriter.appendInFile(metadataLogger.bodyDevice(timeStamp, platform, osRawName, osVersion, deviceRawName, appVersion, appBuildNumber, if (isVideoCompression) COMPRESSION_VIDEO else COMPRESSION_PHOTO))
    }

    override fun onGpsLog(location: Location) {
        metadataWriter.appendInFile(metadataLogger.bodyGps(location))
    }

    override fun onNewEvent(baseObject: BaseObject<*>?) {
        if (baseObject!!.statusCode != LibraryUtil.PHONE_SENSOR_READ_SUCCESS) {
            Timber.d("onNewEvent. Status: Failed status. Message: New event ignored.")
            return
        }

        Timber.d(String.format("onNewEvent. Status: received sensor. type: %s", baseObject.getSensorType()))
        when (baseObject.getSensorType()) {
            LibraryUtil.ACCELEROMETER -> metadataWriter.appendInFile(metadataLogger.bodyAcceleration(baseObject as ThreeAxesObject))
            LibraryUtil.GRAVITY -> metadataWriter.appendInFile(metadataLogger.bodyGravity(baseObject as ThreeAxesObject))
            LibraryUtil.ROTATION_VECTOR_RAW -> metadataWriter.appendInFile(metadataLogger.bodyAttitude(baseObject as ThreeAxesObject))
            LibraryUtil.LINEAR_ACCELERATION -> metadataWriter.appendInFile(metadataLogger.bodyAcceleration(baseObject as ThreeAxesObject))
            LibraryUtil.PRESSURE -> {
                Timber.d(String.format("onNewEvent. Status: pressure received sensor. "))
                metadataWriter.appendInFile(metadataLogger.bodyPressure(baseObject as PressureObject))
            }
            LibraryUtil.HEADING -> {
                val compassData = baseObject as ThreeAxesObject
                photoCachedData.compassData = compassData
                metadataWriter.appendInFile(metadataLogger.bodyCompass(compassData))
            }
        }
    }

    private data class PhotoCachedData(var compassData: ThreeAxesObject?, var obdSpeedObject: ObdSpeedObject?)

    companion object {
        val INSTANCE: MetadataSensorManager by lazy { HOLDER.INSTANCE }

        private const val SIZE_ONE_VALUE = 1

        private const val COMPRESSION_PHOTO = "photo"
        private const val COMPRESSION_VIDEO = "video"
    }
}
