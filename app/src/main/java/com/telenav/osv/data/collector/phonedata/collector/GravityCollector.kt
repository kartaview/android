package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.GravityObject
import com.telenav.osv.data.collector.datatype.datatypes.ThreeAxesObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * GravityCollector collects the force of gravity that is applied to a device
 * on all three physical axes (X, Y, Z)
 */
class GravityCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    fun registerGravityListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var gravitySensor: Sensor? = null
            if (sensorManager != null) {
                gravitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_GRAVITY)
            }
            if (gravitySensor != null) {
                sensorManager!!.registerListener(this, gravitySensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(GravityObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        val gravityObject: ThreeAxesObject = GravityObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        gravityObject.setxValue(-event.values[0])
        gravityObject.setyValue(-event.values[1])
        gravityObject.setzValue(-event.values[2])
        determineTimestamp(event.timestamp, gravityObject)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}