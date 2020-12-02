package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.GyroscopeObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * GyroCollector class collects the device's rate of rotation around the X, Y and Z axis
 */
class GyroCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    fun registerGyroListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var gyroSensor: Sensor? = null
            if (sensorManager != null) {
                gyroSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            }
            if (gyroSensor != null) {
                sensorManager!!.registerListener(this, gyroSensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(GyroscopeObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        val gyroObject: ThreeAxesObject = GyroscopeObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        gyroObject.setxValue(event.values[0])
        gyroObject.setyValue(event.values[1])
        gyroObject.setzValue(event.values[2])
        determineTimestamp(event.timestamp, gyroObject)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}