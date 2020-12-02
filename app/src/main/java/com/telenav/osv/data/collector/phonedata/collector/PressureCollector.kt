package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.PressureObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * PressureCollector class collects the ambient air pressure in mbar
 */
class PressureCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    fun registerPressureListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var pressureSensor: Sensor? = null
            if (sensorManager != null) {
                pressureSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
            }
            if (pressureSensor != null) {
                sensorManager!!.registerListener(this, pressureSensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(PressureObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        val sensorValues = event.values
        if (sensorValues != null && sensorValues.isNotEmpty()) {
            determineTimestamp(event.timestamp, PressureObject(sensorValues[0] / 10, LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}