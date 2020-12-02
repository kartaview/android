package com.telenav.osv.data.collector.phonedata.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.telenav.osv.data.collector.datatype.EventDataListener
import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.PhoneSensors
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.PhoneSensorsFrequency
import com.telenav.osv.data.collector.phonedata.service.PhoneService
import com.telenav.osv.data.collector.phonedata.service.PhoneService.PhoneServiceBinder
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * PhoneSensorsManager class handle phone sensors and listeners in order
 * to provide useful information to the service which handle process collections
 */
class PhoneSensorsManager {
    /**
     * Used to determine which sensors was registered by which listener
     */
    private val classListeners: MutableMap<EventDataListener?, MutableList<String>?>? = ConcurrentHashMap()

    /**
     * Used to determine the frequency for each sensor
     */
    private val sensorsFrequency: MutableMap<String, Int?>? = ConcurrentHashMap()
    private var mServiceIntent: Intent? = null
    private var phoneService: PhoneService? = null
    private var isServiceStarted = false

    /**
     * The manager will be notified via this interface when a new sensor value is read
     */
    private val phoneDataListener: PhoneDataListener = object : PhoneDataListener {
        override fun onSensorChanged(baseObject: BaseObject<*>) {
            for ((listener) in classListeners!!) {
                if (isSensorListened(listener, baseObject.getSensorType())) {
                    listener!!.onNewEvent(baseObject)
                }
            }
        }
    }
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val phoneServiceBinder: PhoneServiceBinder = service as PhoneServiceBinder
            phoneService = phoneServiceBinder.phoneService
            phoneService!!.startCollecting(sensorTypes, sensorsFrequency, phoneDataListener)
            Timber.tag(TAG).d("Phone service connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.tag(TAG).d("Phone service disconnected")
        }
    }

    fun startService(context: Context?) {
        if (context != null && !isServiceStarted) {
            mServiceIntent = Intent(context, PhoneService::class.java)
            context.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
            isServiceStarted = true
        }
    }

    fun stopService(context: Context?) {
        if (context != null && isServiceStarted) {
            context.unbindService(mServiceConnection)
            mServiceIntent = null
            phoneService = null
            isServiceStarted = false
        }
    }

    /**
     * Adds the listener (class data listener) and add the sensor type that
     * has to be registered to listenerTypes
     *
     * @param listener The listener that has to be added
     * @param sensor   The type of sensor that has to be registered
     */
    fun registerSensorToListener(listener: EventDataListener?, @PhoneSensors sensor: String?) {
        if (listener != null && sensor != null) {
            sensorsFrequency?.let {
                if (!it.containsKey(sensor)) {
                    setDefaultFrequency(sensor)
                }
                addSensorsToListenerList(listener, sensor)
            }
        }
    }

    /**
     * Removes the listener (class data listener) and call the method
     * for removing the sensor that are used anymore
     *
     * @param listener The listener that has to be removed
     */
    fun removeListener(listener: EventDataListener?) {
        if (classListeners != null && listener != null) {
            unregisterSensors(listener, classListeners[listener])
            classListeners.remove(listener)
        }
    }

    /**
     * Removes a part of the sensors that was registered from some class listener
     *
     * @param listener The class listener
     * @param sensor   The type of sensor that has to be removed
     */
    fun removeSensorFromListenerList(listener: EventDataListener?, @PhoneSensors sensor: String) {
        if (classListeners != null && classListeners[listener] != null && listener != null) {
            val sensorTypes = classListeners[listener]
            sensorTypes?.remove(sensor)
            if (!isSensorRegistered(sensor, listener)) {
                unregisterSensor(sensor)
                removeSensorFrequency(sensor)
            }
            classListeners[listener] = sensorTypes
        }
    }

    /**
     * Set the frequency for a specific sensor
     *
     * @param type      The type of the sensor
     * @param frequency The frequency that has to be set
     */
    fun setSensorFrequency(@PhoneSensors type: String?, @PhoneSensorsFrequency frequency: Int) {
        if (type != null) {
            sensorsFrequency?.let {
                it[type] = frequency
            }
            if (isServiceStarted) {
                phoneService?.setSensorFrequency(type, frequency)
            }
        }
    }

    fun getSensorsFrequency(): Map<String, Int?>? {
        return sensorsFrequency
    }

    /**
     * Iterates each entry from map and return an array with all types of sensors
     * that have to be registered
     *
     * @return An array with all type of sensors that has to be registered
     */
    val sensorTypes: Array<String>
        get() {
            val allSensors: MutableList<String> = ArrayList()
            if (classListeners != null) for ((_, sensors) in classListeners) {
                if (sensors != null) {
                    for (sensor in sensors) {
                        if (!allSensors.contains(sensor)) {
                            allSensors.add(sensor)
                        }
                    }
                }
            }
            return allSensors.toTypedArray()
        }
    val classListener: Map<EventDataListener?, MutableList<String>?>?
        get() = classListeners

    /**
     * Verify if a sensor os listened
     * It is used for verify if a class(listener) registered a certain sensor
     *
     * @param listener The class that listen for sensors
     * @param sensor   The type of the sensor
     * @return Return true if the sensor was found in the list and false if not
     */
    private fun isSensorListened(listener: EventDataListener?, sensor: String?): Boolean {
        if (listener != null && sensor != null && classListeners != null) {
            val list: List<String>? = classListeners[listener]
            if (list != null && list.contains(sensor)) {
                return true
            }
        }
        return false
    }

    /**
     * Add the type of sensor that has to be registered to a map and call the
     * registerSensor() method.
     * The key used for is the name of the class that listens for phone sensors
     *
     * @param listener The class that listens for phone sensors
     * @param sensor   The type of sensor that has to be registered
     */
    fun addSensorsToListenerList(listener: EventDataListener?, sensor: String) {
        val sensorsTypeList: MutableList<String>?
        if (!classListeners!!.containsKey(listener)) {
            registerSensor(sensor)
            classListeners[listener] = ArrayList(listOf(sensor))
        } else {
            sensorsTypeList = classListeners[listener]
            if (!sensorsTypeList!!.contains(sensor)) {
                sensorsTypeList.add(sensor)
                registerSensor(sensor)
                classListeners[listener] = sensorsTypeList
            }
        }
    }

    /**
     * Unregisters the sensors from a certain class listener that
     * are not used by others class listeners
     *
     * @param listener       The class listener that was removed
     * @param sensorToRemove The list of sensor types that should be removed
     */
    private fun unregisterSensors(listener: EventDataListener?, sensorToRemove: List<String>?) {
        if (listener != null && sensorToRemove != null && classListeners != null) {
            for (sensorType in sensorToRemove) {
                if (!isSensorRegistered(sensorType, listener)) {
                    unregisterSensor(sensorType)
                    removeSensorFrequency(sensorType)
                }
            }
        }
    }

    /**
     * Verifies if a certain sensor is registered by some class listeners
     *
     * @param sensorToRemove The sensor type that has to be verified
     */
    private fun isSensorRegistered(sensorToRemove: String?, listener: EventDataListener?): Boolean {
        if (sensorToRemove != null && classListeners != null && listener != null) {
            for ((key) in classListeners) {
                if (isSensorListened(key, sensorToRemove) && key !== listener) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Sets a default frequency value to sensors
     *
     * @param sensor The type of the sensor
     */
    private fun setDefaultFrequency(sensor: String?) {
        if (sensorsFrequency != null && sensor != null) {
            sensorsFrequency[sensor] = DEFAULT_FREQUENCY
        }
    }

    /**
     * Removes frequency for a specific sensor
     *
     * @param sensor The type of the sensor
     */
    private fun removeSensorFrequency(sensor: String?) {
        if (sensorsFrequency != null && sensor != null) {
            sensorsFrequency.remove(sensor)
        }
    }

    /**
     * Call the method for registering a new sensor
     *
     * @param type The type of the sensor that has to be registered
     */
    private fun registerSensor(type: String) {
        phoneService?.let {
            if (isServiceStarted) {
                val sensorType = arrayOf(type)
                phoneService?.registerSensor(sensorType)
            }
        }
    }

    /**
     * Unregister a sensor
     *
     * @param type Type of the sensor that has to be unregistered
     */
    private fun unregisterSensor(type: String) {
        phoneService?.let {
            val sensorType = arrayOf(type)
            phoneService?.unregisterSensor(sensorType)
        }
    }

    companion object {
        /**
         * The default frequency
         */
        private const val DEFAULT_FREQUENCY = 0
        private const val TAG = "PhoneSensorManager"
    }
}