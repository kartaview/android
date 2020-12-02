package com.telenav.osv.data.collector.obddata.manager

import com.telenav.osv.data.collector.datatype.EventDataListener
import com.telenav.osv.data.collector.datatype.ObdConnectionListener
import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.datatype.datatypes.VinObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.ObdSensors
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.ObdSensorsFrequency
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.ObdSourceListener
import com.telenav.osv.data.collector.obddata.AbstractClientDataTransmission
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 *
 */
class OBDSensorManager private constructor() {
    private var abstractClientDataTransmission: AbstractClientDataTransmission? = null

    /**
     * vehicle id will always be collected at the beginning of collection,
     * and stored inside this variable
     */
    private var vehicleId: String? = null
    private val obdListenerMap: MutableMap<EventDataListener, MutableList<String>> = ConcurrentHashMap()

    private val obdConnectionListeners: MutableList<ObdConnectionListener> = CopyOnWriteArrayList()

    /**
     * makes sure that if the client subscribes to/un-subscribes from multiple
     * sensors at a time, the threads which register these sensors will run sequentially
     */
    private val multipleSubscribingLock: Lock

    /**
     * Used to determine the frequency for each sensor
     */
    private val sensorFrequencies: MutableMap<String, Int> = HashMap()

    val obdDataListener: ObdDataListener = object : ObdDataListener {
        override fun onSensorChanged(baseObject: BaseObject<*>) {
            notifyListeners(baseObject)
        }

        override fun onConnectionStateChanged(dataSource: String, statusCode: Int) {
            notifyConnectionStateChanged(dataSource, statusCode)
        }

        /**
         * when the connection is stopped, clear the connection and data listener from the sensor manager
         * @param source
         */
        override fun onConnectionStopped(source: String) {
            clearListeners()
        }

        override fun onInitializationFailedWarning() {
            notifyListenersInitializationFailed()
        }

        override fun requestSensorFrequencies(): MutableMap<String, Int> {
            return sensorFrequencies
        }

        override fun clearListeners() {
            obdConnectionListeners.clear()
            obdListenerMap.clear()
        }

        /**
         * when the initialization procedure does not detect the OBD version, a
         * warning should be sent to the listeners
         */
        private fun notifyListenersInitializationFailed() {
            for ((key) in obdListenerMap) {
                val baseObject: BaseObject<*> = BaseObject<Any?>(null, LibraryUtil.OBD_INITIALIZATION_FAILURE)
                key.onNewEvent(baseObject)
            }
        }

        private fun notifyConnectionStateChanged(@ObdSourceListener source: String, statusCode: Int) {
            for (obdListener in obdConnectionListeners) {
                obdListener.onConnectionStateChanged(null, source, statusCode)
            }
        }

        /**
         * when a sensor value was collected, notify all the listeners of that sensor
         *
         * @param sensor - the object containing sensor information
         */
        private fun notifyListeners(sensor: BaseObject<*>) {
            if (obdListenerMap.isNotEmpty()) {
                for ((key) in obdListenerMap) {
                    //the vehicle id will be sent to all sensor listeners
                    if (sensor is VinObject) {
                        key.onNewEvent(sensor)
                        vehicleId = sensor.vin
                    }
                    key.onNewEvent(sensor)
                }
            } else {
                //if there is no listener yet, and vehicle id was collected, store it into a variable
                if (sensor is VinObject) {
                    vehicleId = sensor.vin
                }
            }
        }
    }

    fun addConnectionListener(obdConnectionListener: ObdConnectionListener) {
        val clazz: Class<*> = obdConnectionListener.javaClass
        val iterator: Iterator<ObdConnectionListener> = obdConnectionListeners.iterator()
        while (iterator.hasNext()) {
            val connectionListener = iterator.next()
            if (connectionListener.javaClass == clazz) {
                obdConnectionListeners.remove(connectionListener)
            }
        }
        obdConnectionListeners.add(obdConnectionListener)
    }

    fun getObdConnectionListeners(): MutableList<ObdConnectionListener> {
        return obdConnectionListeners
    }

    /**
     * sets the frequency for a sensor
     * @param type - the type of sensor
     * @param frequency - the desired frequency
     */
    fun setSensorFrequency(@ObdSensors type: String?, @ObdSensorsFrequency frequency: Int) {
        if (type != null && isSensorListened(type)) {
            sensorFrequencies[type] = frequency
            //if the frequency is changed during collection, notify the collecting thread
            abstractClientDataTransmission?.onCollectionThreadRestartRequired()
        }
    }

    /**
     * subscribes a listener to receive the values collected by a sensor
     * NOTE: this method must be called BEFORE the collection has started
     * @param listener - the class which listens for sensor events
     * @param sensor - the collected sensor
     */
    fun registerSensor(listener: EventDataListener, @ObdSensors sensor: String) {
        if (!obdListenerMap.containsKey(listener)) {
            obdListenerMap[listener] = ArrayList(listOf(sensor))
        } else {
            val sensors = obdListenerMap[listener]

            //check if the sensor has already been added to the listener
            if (!sensors!!.contains(sensor)) {
                sensors.add(sensor)
            }
            obdListenerMap[listener] = sensors
        }
    }

    /**
     * subscribes another sensor to the list of sensor to be collected for a listener
     * NOTE: this method must be called AFTER the collection has started
     * @param listener - the class which listens for sensor events
     * @param sensor - the collected sensor
     */
    fun subscribeSensor(listener: EventDataListener, @ObdSensors sensor: String) {
        val subscriberRunnable: Runnable = SubscriberRunnable(listener, sensor, ACTION_SUBSCRIBE)
        Thread(subscriberRunnable).start()
    }

    /**
     * unsubscribes a listener from receiving sensor events
     * @param listener - the class which listens for sensor events
     */
    @Synchronized
    fun unregisterListener(listener: EventDataListener?) {
        obdListenerMap.remove(listener)
    }

    fun unsubscribeSensor(listener: EventDataListener, @ObdSensors sensor: String) {
        val unsubscriberRunnable: Runnable = SubscriberRunnable(listener, sensor, ACTION_UNSUBSCRIBE)
        Thread(unsubscriberRunnable).start()
    }

    /**
     * retrieves all sensor event listeners
     * @return - a list of listeners
     */
    val obdListeners: MutableList<EventDataListener>
        get() {
            val result: MutableList<EventDataListener> = ArrayList()
            for ((key) in obdListenerMap) {
                result.add(key)
            }
            return result
        }

    /**
     * retrieves whether or not a listener is listening for a specific sensor
     * @param listener - the sensor listener
     * @param sensorType - the listened sensor
     * @return - true if listener is registered for sensor, false otherwise
     */
    fun isListenerRegisteredForSensor(listener: EventDataListener?, sensorType: String?): Boolean {
        return if (obdListenerMap.containsKey(listener)) {
            obdListenerMap[listener]!!.contains(sensorType)
        } else {
            false
        }
    }

    /**
     * checks whether or not a specific sensor is listened by any listener
     * @param sensorType - the type of sensor
     * @return true if the sensor is listened by a listener, false otherwise
     */
    fun isSensorListened(sensorType: String?): Boolean {
        for ((_, value) in obdListenerMap) {
            if (value!!.contains(sensorType)) {
                return true
            }
        }
        return false
    }

    /**
     * retrieves a list of the sensor which need to be collected for different listeners
     * @return - the list of desired sensors
     */
    val listOfDesiredSensors: MutableList<String>
        get() {
            val result: MutableList<String> = ArrayList()
            for ((_, value) in obdListenerMap) {
                for (sensor in value!!) {
                    if (!result.contains(sensor)) {
                        result.add(sensor)
                    }
                }
            }
            return result
        }

    fun getAbstractClientDataTransmission(): AbstractClientDataTransmission? {
        return abstractClientDataTransmission
    }

    fun setAbstractClientDataTransmission(abstractClientDataTransmission: AbstractClientDataTransmission) {
        this.abstractClientDataTransmission = abstractClientDataTransmission

        //when the transmission object has been added, automatically add it as a listener for connection events
        //this way, when the client stops the connection, the object knows it has to stop its running service
        addConnectionListener(abstractClientDataTransmission)
    }

    /**
     * inner class that handles the thread which notifies the obd that a new sensor will be collected/a sensor will no longer be collected
     */
    private inner class SubscriberRunnable internal constructor(var listener: EventDataListener, var sensor: String, var action: String) : Runnable {
        override fun run() {
            //acquire lock to prevent other threads from subscribing sensors at the same time
            multipleSubscribingLock.lock()
            pauseCollection()
            when (action) {
                ACTION_SUBSCRIBE -> onSubscribeAction()
                ACTION_UNSUBSCRIBE -> onUnsubscribeAction()
                else -> Timber.tag(TAG).e("Subscribe/unsubscribe invalid action")
            }
            multipleSubscribingLock.unlock()
            //release the lock associated with this thread
            resumeCollection()
            Thread.currentThread().interrupt()
        }

        @Synchronized
        fun pauseCollection() {
            AbstractClientDataTransmission.Companion.shouldCollect = false
        }

        @Synchronized
        fun resumeCollection() {
            AbstractClientDataTransmission.Companion.shouldCollect = true
        }

        fun onSubscribeAction() {
            if (sensor == LibraryUtil.VEHICLE_ID) {
                notifyVehicleIdRequested(listener)
            } else {
                registerSensor(listener, sensor)

                //when a new sensor has been subscribed, the collecting thread should restart, so
                //that the frequency vector will be updated
                if (abstractClientDataTransmission != null) {
                }
                abstractClientDataTransmission?.onCollectionThreadRestartRequired()
            }
        }

        fun onUnsubscribeAction() {
            unregisterSensor(listener, sensor)
        }

        /**
         * called when vehicle id was requested after collection started
         */
        private fun notifyVehicleIdRequested(listener: EventDataListener) {
            if (!obdListenerMap.containsKey(listener)) {
                obdListenerMap[listener] = ArrayList()
            }
            val vinObject = VinObject(vehicleId, LibraryUtil.OBD_READ_SUCCESS)
            obdDataListener.onSensorChanged(vinObject)
        }

        /**
         * un-subscribes a listener to receive the values collected by a sensor
         * NOTE: this method must be called BEFORE the collection has started
         * @param listener - the class which listens for sensor events
         * @param sensor -   the collected sensor
         */
        private fun unregisterSensor(listener: EventDataListener, @ObdSensors sensor: String) {
            if (!obdListenerMap.containsKey(listener)) {
                return
            }
            val sensors = obdListenerMap[listener]

            //check if the list contains the sensor to be unregistered
            if (sensors!!.contains(sensor)) {
                sensors.remove(sensor)
            }
            obdListenerMap[listener] = sensors

            //un-subscribe the sensor from Logshed uploading too
            for ((key, registeredSensors) in obdListenerMap) {
                registeredSensors!!.remove(sensor)
                obdListenerMap[key] = registeredSensors
            }
        }
    }

    companion object {
        private val TAG = OBDSensorManager::class.java.simpleName
        private const val ACTION_SUBSCRIBE = "subscribe"
        private const val ACTION_UNSUBSCRIBE = "unsubscribe"
        val instance = OBDSensorManager()
    }

    init {
        multipleSubscribingLock = ReentrantLock()
    }
}