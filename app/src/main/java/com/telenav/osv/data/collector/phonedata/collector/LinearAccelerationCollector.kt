package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.LinearAccelerationObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * LinearAccelerationCollector class collects linear acceleration for all three physical axes (X, Y, Z)
 * The collected data not include gravity
 */
class LinearAccelerationCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    fun registerLinearAccelerationListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var linearAccelerationSensor: Sensor? = null
            if (sensorManager != null) {
                linearAccelerationSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            }
            if (linearAccelerationSensor != null) {
                sensorManager!!.registerListener(this, linearAccelerationSensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(LinearAccelerationObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        val linearAccelerationObject: ThreeAxesObject = LinearAccelerationObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        linearAccelerationObject.setxValue(event.values[0])
        linearAccelerationObject.setyValue(event.values[1])
        linearAccelerationObject.setzValue(event.values[2])
        determineTimestamp(event.timestamp, linearAccelerationObject)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}