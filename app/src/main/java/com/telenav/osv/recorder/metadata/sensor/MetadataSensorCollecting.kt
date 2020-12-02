package com.telenav.osv.recorder.metadata.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Class responsible of collecting sensor related to metadata.
 *
 * This will collect the data related to pressure sensor.
 */
class MetadataSensorCollecting : SensorEventListener{

    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false;

    fun registerPressureListener(context: Context) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var pressureSensor: Sensor? = null
            if (sensorManager != null) {
                pressureSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
            }
            if (pressureSensor != null) {
                //sensorManager!!.registerListener(this, pressureSensor, frequency, notifyHandler)
                isSensorRegistered = true
            } else {
                //sendSensorUnavailabilityStatus(PressureObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
            }
        }
    }

    data class PressureData(val timestamp: Long, val pressure: Float)

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {

    }
}