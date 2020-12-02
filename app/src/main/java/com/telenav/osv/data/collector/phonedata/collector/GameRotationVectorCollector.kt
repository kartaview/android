package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.RotationVectorRawObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * GameRotationVectorCollector class collects the orientation of a device by providing
 * the three elements of the device's rotation vector. It has NOT north as a reference point
 */
class GameRotationVectorCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    private val mRotationMatrixS = FloatArray(16)
    private val mOrientationS = FloatArray(3)
    fun registerGameRotationVectorListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var gameRotationVectorSensor: Sensor? = null
            if (sensorManager != null) {
                gameRotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            }
            if (gameRotationVectorSensor != null) {
                sensorManager!!.registerListener(this, gameRotationVectorSensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(RotationVectorRawObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
            }
        }
    }

    fun unregisterListener() {
        if (sensorManager != null && isSensorRegistered) {
            sensorManager!!.unregisterListener(this)
            isSensorRegistered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrixS, event.values)
        SensorManager.getOrientation(mRotationMatrixS, mOrientationS)
        val gameRotationVectorObject: ThreeAxesObject = RotationVectorRawObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        gameRotationVectorObject.setxValue(-mOrientationS[1]) //pitch
        gameRotationVectorObject.setyValue(mOrientationS[2]) //roll
        gameRotationVectorObject.setzValue(-mOrientationS[0]) //yaw
        determineTimestamp(event.timestamp, gameRotationVectorObject)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}