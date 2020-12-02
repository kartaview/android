package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.ProximityObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * ProximityCollector collects the proximity of an object in cm relative to the view screen of a device
 */
class ProximityCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var isSensorRegistered = false
    fun registerProximityListener(context: Context, frequency: Int) {
        if (!isSensorRegistered) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            var proximitySensor: Sensor? = null
            if (sensorManager != null) {
                proximitySensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            }
            if (proximitySensor != null) {
                sensorManager!!.registerListener(this, proximitySensor, frequency, notifyHandler)
                isSensorRegistered = true
                setUpFrequencyFilter(frequency)
            } else {
                sendSensorUnavailabilityStatus(ProximityObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
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
        val bool = event.values[0] == 0F
        determineTimestamp(event.timestamp, ProximityObject(bool, LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // No implementation needed
    }
}