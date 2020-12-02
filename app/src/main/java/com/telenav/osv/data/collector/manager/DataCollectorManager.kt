package com.telenav.osv.data.collector.manager

import android.content.Context
import com.telenav.osv.BuildConfig
import com.telenav.osv.data.collector.config.Config
import com.telenav.osv.data.collector.datatype.EventDataListener
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.log.LogWrapperTree
import com.telenav.osv.data.collector.log.NoOpTree
import timber.log.Timber

/**
 * DataCollectorManager class is the entry point for the client to the library.
 * It allows the user to use library features: start/stop, subscribePhoneSensor/unsubscribePhoneSensor another sensor
 *
 *
 * ATTENTION: Only one instance of this class should be used per collection process.
 * If you start the collection process use the same instance of this class for all the
 * other operations until collection process is stopped.
 */
class DataCollectorManager internal constructor(private val context: Context,
                                                /**
                                                 * TODO The switch statements on LibraryUtil sources indicate toward a possible Factory Pattern.
                                                 * TODO The nested for loop in registerSensorsToListener() should be encapsulated in another class.
                                                 * TODO EventDataManager.init() could be avoided by passing the Context in its constructor
                                                 * TODO Implement Null Object pattern for OpenXcListener
                                                 */
                                                private val config: Config, private val eventDataManager: EventDataManager, tree: Timber.Tree?) {
    constructor(context: Context, config: Config) : this(context, config, EventDataManager(context), if (BuildConfig.DEBUG) LogWrapperTree() else NoOpTree())

    fun startCollecting() {
        val sources = config.getSourceList()
        if (sources.isEmpty()) {
            Timber.tag(LibraryUtil.ERROR_TAG).e(LibraryUtil.NO_SOURCE_ADDED)
        } else {
            registerSensorsToListener()
            setSensorsFrequencies()
            eventDataManager.checkSensors(config)
            for (s in sources) {
                when (s) {
                    LibraryUtil.PHONE_SOURCE -> eventDataManager.startPhoneService()
                    LibraryUtil.OBD_WIFI_SOURCE -> eventDataManager.startWifiObd(context, config)
                    LibraryUtil.OBD_BLUETOOTH_SOURCE -> eventDataManager.startBluetoothObd(config)
                    LibraryUtil.OBD_BLE_SOURCE -> eventDataManager.startBleObd(context, config)
                    else -> Timber.tag(LibraryUtil.ERROR_TAG).e("Invalid source")
                }
            }
        }
    }

    fun stopCollecting() {
        val sources = config.getSourceList()
        for (s in sources) {
            when (s) {
                LibraryUtil.PHONE_SOURCE -> eventDataManager.stopPhoneService()
                LibraryUtil.OBD_WIFI_SOURCE -> eventDataManager.stopWifiObd()
                LibraryUtil.OBD_BLUETOOTH_SOURCE -> eventDataManager.stopBluetoothObd()
                LibraryUtil.OBD_BLE_SOURCE -> eventDataManager.stopBleObd()
                else -> Timber.tag(LibraryUtil.ERROR_TAG).e("Invalid source")
            }
        }
    }

    /**
     * stops the phone collection service
     */
    fun stopCollectingPhoneData() {
        eventDataManager.stopPhoneService()
    }

    /**
     * stops the OBD collection service
     */
    fun stopCollectingObdData() {
        val sources = config.getSourceList()
        for (s in sources) {
            when (s) {
                LibraryUtil.OBD_WIFI_SOURCE -> eventDataManager.stopWifiObd()
                LibraryUtil.OBD_BLUETOOTH_SOURCE -> eventDataManager.stopBluetoothObd()
                LibraryUtil.OBD_BLE_SOURCE -> eventDataManager.stopBleObd()
                else -> Timber.tag(LibraryUtil.ERROR_TAG).e("No OBD sources were registered")
            }
        }
    }

    /**
     * Subscribes another sensor to the list of sensors to be collected for a listener
     * NOTE: this method must be called AFTER the collection has started
     *
     * @param listener - the class which listens for sensor events
     * @param sensor   - the collected sensor
     */
    fun subscribeSensor(listener: EventDataListener?, @LibraryUtil.AvailableData sensor: String?) {
        eventDataManager.subscribeSensor(listener, sensor!!)
    }

    /**
     * Unsubscribes a sensor from the list of collected sensors
     * NOTE: this method must be called AFTER the collection has started
     *
     * @param listener - the class which listens for sensor events
     * @param sensor   - the collected sensor
     */
    fun unsubscribeSensor(listener: EventDataListener?, @LibraryUtil.AvailableData sensor: String?) {
        eventDataManager.unsubscribeSensor(listener, sensor!!)
    }

    /**
     * Sets the frequency for a sensor
     *
     * @param sensor    - the collected sensor
     * @param frequency - the frequency for collected sensor
     */
    fun setSensorFrequency(@LibraryUtil.AvailableData sensor: String?, @LibraryUtil.SensorsFrequency frequency: Int) {
        eventDataManager.setSensorFrequency(sensor!!, frequency)
    }

    /**
     * Iterates the map of listeners from config and register the sensors to each listener
     */
    private fun registerSensorsToListener() {
        val listeners: Map<EventDataListener, List<String>> = config.dataListeners
        for ((key, value) in listeners) {
            for (sensor in value) {
                eventDataManager.registerSensorToListener(key, sensor)
            }
        }
    }

    /**
     * Iterates the map of sensors from config and sets the frequency to each sensor
     */
    private fun setSensorsFrequencies() {
        val sensorsFrequencies = config.sensorsFrequencies
        for ((key, value) in sensorsFrequencies) {
            eventDataManager.setSensorFrequency(key, value)
        }
    }

    init {
        Timber.uprootAll()
        Timber.plant(tree!!)
    }
}