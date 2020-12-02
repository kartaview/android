package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.HeadingObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * HeadingCollector class collects for all three physical axes (X, Y, Z)
 */
class HeadingCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    private var mGravity: FloatArray? = FloatArray(3)
    private val mHeadingMatrixIn = FloatArray(16)
    private val mHeadingValues = FloatArray(3)
    fun registerHeadingListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var magneticSensor: Sensor? = null
            var gravitySensor: Sensor? = null
            if (sensorManager != null) {
                magneticSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                gravitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            }
            if (magneticSensor != null && gravitySensor != null) {
                sensorManager!!.registerListener(this, magneticSensor, frequency, notifyHandler)
                sensorManager!!.registerListener(this, gravitySensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(HeadingObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values
        } else {
            if (mGravity != null && event.values != null) {
                val success = SensorManager.getRotationMatrix(mHeadingMatrixIn, null, mGravity, event.values)
                if (success) {
                    // This method is used in order to map device coordinates to word coordinates (ex: portrait/landscape)
                    SensorManager.remapCoordinateSystem(mHeadingMatrixIn, SensorManager.AXIS_X, SensorManager.AXIS_Z, mHeadingMatrixIn)
                    SensorManager.getOrientation(mHeadingMatrixIn, mHeadingValues)
                    mHeadingValues[0] = Math.toDegrees(mHeadingValues[0].toDouble()).toFloat()
                    mHeadingValues[1] = Math.toDegrees(mHeadingValues[1].toDouble()).toFloat()
                    mHeadingValues[2] = Math.toDegrees(mHeadingValues[2].toDouble()).toFloat()
                    mHeadingValues[0] = if (mHeadingValues[0] >= 0) mHeadingValues[0] else mHeadingValues[0] + 360
                    val headingObject: ThreeAxesObject = HeadingObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
                    headingObject.setxValue(mHeadingValues[1]) // heading pitch
                    headingObject.setyValue(mHeadingValues[2]) // heading roll
                    headingObject.setzValue(mHeadingValues[0]) // heading azimuth. When this value is 0 the device points to north
                    determineTimestamp(event.timestamp, headingObject)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}