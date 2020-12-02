package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.RotationVectorNorthObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * RotationVectorCollector class collects the orientation of a device by providing
 * the three elements of the device's rotation vector. It has north as a reference point
 */
class RotationVectorCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    private val mRotationMatrixS = FloatArray(16)
    private val mOrientationS = FloatArray(3)
    fun registerRotationVectorListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var rotationVectorSensor: Sensor? = null
            if (sensorManager != null) {
                rotationVectorSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            }
            if (rotationVectorSensor != null) {
                sensorManager!!.registerListener(this, rotationVectorSensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(RotationVectorNorthObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        val rotationVectorObject: ThreeAxesObject = RotationVectorNorthObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        rotationVectorObject.setxValue(-mOrientationS[1]) //pitch
        rotationVectorObject.setyValue(mOrientationS[2]) //roll
        rotationVectorObject.setzValue(-mOrientationS[0]) //yaw
        determineTimestamp(event.timestamp, rotationVectorObject)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}