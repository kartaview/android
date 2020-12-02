package com.telenav.osv.data.collector.manager

import android.content.Context
import com.telenav.osv.data.collector.config.Config
import com.telenav.osv.data.collector.datatype.EventDataListener
import com.telenav.osv.data.collector.datatype.ObdConnectionListener
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData
import com.telenav.osv.data.collector.obddata.connection.BleObdConnection
import com.telenav.osv.data.collector.obddata.connection.BluetoothObdConnection
import com.telenav.osv.data.collector.obddata.connection.WifiObdConnection
import com.telenav.osv.data.collector.obddata.manager.OBDSensorManager
import com.telenav.osv.data.collector.phonedata.manager.PhoneSensorsManager
import timber.log.Timber

/**
 * EventDataManager class delegates the responsibility to the managers
 * of each source (PhoneSensorManager, OBDSensorManager, OpenxcDataManager)
 */
class EventDataManager : ObdConnectionListener {
    private val context: Context
    private val obdSensorManager: OBDSensorManager?
    private val phoneSensorsManager: PhoneSensorsManager

    //obd connection variables
    private var wifiObdConnection: WifiObdConnection? = null
    private var bluetoothObdConnection: BluetoothObdConnection? = null
    private var bleObdConnection: BleObdConnection? = null

    internal constructor(context: Context) {
        this.context = context
        phoneSensorsManager = PhoneSensorsManager()
        obdSensorManager = OBDSensorManager.instance
    }

    internal constructor(context: Context, obdSensorManager: OBDSensorManager, phoneSensorsManager: PhoneSensorsManager) {
        this.context = context
        this.obdSensorManager = obdSensorManager
        this.phoneSensorsManager = phoneSensorsManager
    }

    fun startPhoneService() {
        phoneSensorsManager.startService(context)
    }

    /**
     * library should stop, then true, otherwise false
     */
    fun stopPhoneService() {
        phoneSensorsManager.stopService(context)
    }

    fun stopBluetoothObd() {
        bluetoothObdConnection?.stopClientObdBluetoothConnection(true)
        bluetoothObdConnection = null
    }

    fun stopWifiObd() {
        wifiObdConnection?.stopWifiObdConnection()
        wifiObdConnection = null
    }

    fun stopBleObd() {
        bleObdConnection?.stopBleObdConnection()
        bleObdConnection = null
    }

    fun startWifiObd(context: Context, config: Config) {
        obdSensorManager?.addConnectionListener(this)
        if (wifiObdConnection == null) {
            wifiObdConnection = WifiObdConnection(context)
        }
        val wifiObdConnection = WifiObdConnection(context)
        wifiObdConnection.addObdConnectionListener(config.obdConnectionListener)
        wifiObdConnection.connect()
        this.wifiObdConnection = wifiObdConnection
    }

    fun startBluetoothObd(config: Config) {
        obdSensorManager?.addConnectionListener(this)
        config.bluetoothDevice?.let {
            if (bluetoothObdConnection == null) {
                bluetoothObdConnection = BluetoothObdConnection(context, it)
            }
            val bluetoothObdConnection = BluetoothObdConnection(context, it)
            bluetoothObdConnection.addObdConnectionListener(config.obdConnectionListener)
            bluetoothObdConnection.connect()
            this.bluetoothObdConnection = bluetoothObdConnection
        }
    }

    fun startBleObd(context: Context, config: Config) {
        obdSensorManager?.addConnectionListener(this)
        config.bleMacAddress?.let {
            if (bleObdConnection == null) {
                bleObdConnection = BleObdConnection(context, it)
            }
            val bleObdConnection = BleObdConnection(context, it)
            bleObdConnection.addObdConnectionListener(config.obdConnectionListener)
            bleObdConnection.connect()
            this.bleObdConnection = bleObdConnection
        }
    }

    /**
     * subscribes a listener to receive the values collected by a sensor
     * NOTE: this method must be called BEFORE the collection has started
     *
     * @param listener - the class which listens for sensor events
     * @param sensor   - the collected sensor
     */
    fun registerSensorToListener(listener: EventDataListener?, @AvailableData sensor: String) {
        if (isPhoneSensor(sensor)) {
            phoneSensorsManager.registerSensorToListener(listener, sensor)
        }
        listener?.let {
            if (isObdSensor(sensor)) {
                obdSensorManager?.registerSensor(it, sensor)
            }
        }
    }

    /**
     * Removes the listener (class data listener) and call the method
     * for removing the sensor that are not used anymore
     *
     * @param listener The listener that has to be removed
     */
    fun unregisterListener(listener: EventDataListener?) {
        obdSensorManager?.unregisterListener(listener)
        phoneSensorsManager.removeListener(listener)
    }

    fun subscribeSensor(listener: EventDataListener?, @AvailableData sensor: String) {
        if (isPhoneSensor(sensor)) {
            phoneSensorsManager.registerSensorToListener(listener, sensor)
        }
        listener?.let {
            if (isObdSensor(sensor)) {
                obdSensorManager?.subscribeSensor(it, sensor)
            }
        }
    }

    fun unsubscribeSensor(listener: EventDataListener?, @AvailableData sensor: String) {
        if (isPhoneSensor(sensor)) {
            phoneSensorsManager.removeSensorFromListenerList(listener, sensor)
        }
        listener?.let {
            if (isObdSensor(sensor)) {
                obdSensorManager?.unsubscribeSensor(it, sensor)
            }
        }
    }

    fun setSensorFrequency(sensor: String, frequency: Int) {
        if (isPhoneSensor(sensor)) {
            if (isPhoneFrequency(frequency)) {
                phoneSensorsManager.setSensorFrequency(sensor, frequency)
            } else {
                phoneSensorsManager.setSensorFrequency(sensor, LibraryUtil.F_100HZ)
                Timber.tag(LibraryUtil.ERROR_TAG).e("Error: wrong configuration: wrong frequency for %s. The sensor will be collected at default frequency", sensor)
            }
        }
        if (isObdSensor(sensor)) {
            if (isObdFrequency(frequency)) {
                obdSensorManager?.setSensorFrequency(sensor, frequency)
            } else {
                obdSensorManager?.setSensorFrequency(sensor, LibraryUtil.OBD_FAST)
            }
        }
    }

    /**
     * Check if the int value received is a phone frequency
     *
     * @param frequency The int value that has to be checked
     * @return True if it is a phone frequency or false if not
     */
    private fun isPhoneFrequency(frequency: Int): Boolean {
        return when (frequency) {
            LibraryUtil.F_100HZ, LibraryUtil.F_50HZ, LibraryUtil.F_25HZ, LibraryUtil.F_10HZ, LibraryUtil.F_5HZ -> true
            else -> false
        }
    }

    /**
     * checks the frequency chosen by the client
     *
     * @param frequency - the requested sensor frequency
     * @return - true if the frequency corresponds to an OBD frequency, false otherwise
     */
    private fun isObdFrequency(frequency: Int): Boolean {
        return when (frequency) {
            LibraryUtil.OBD_FAST, LibraryUtil.OBD_SLOW -> true
            else -> false
        }
    }

    /**
     * The method returns the source of data for the given sensor
     *
     * @param sensor The sensor used for determie the source of data
     * @return Returns the source of data for the given sensor
     */
    @LibraryUtil.DataSource
    private fun getSourceForSensor(sensor: String): String {
        var source = ""
        when (sensor) {
            LibraryUtil.ACCELEROMETER, LibraryUtil.LINEAR_ACCELERATION, LibraryUtil.BATTERY, LibraryUtil.GYROSCOPE, LibraryUtil.GRAVITY, LibraryUtil.GPS_DATA, LibraryUtil.PHONE_GPS, LibraryUtil.PHONE_GPS_ACCURACY, LibraryUtil.PHONE_GPS_ALTITUDE, LibraryUtil.PHONE_GPS_BEARING, LibraryUtil.PHONE_GPS_SPEED, LibraryUtil.HUMIDITY, LibraryUtil.LIGHT, LibraryUtil.MAGNETIC, LibraryUtil.HEADING, LibraryUtil.PRESSURE, LibraryUtil.PROXIMITY, LibraryUtil.ROTATION_VECTOR_NORTH_REFERENCE, LibraryUtil.ROTATION_VECTOR_RAW, LibraryUtil.STEP_COUNT, LibraryUtil.WIFI, LibraryUtil.TEMPERATURE, LibraryUtil.HARDWARE_TYPE, LibraryUtil.OS_INFO, LibraryUtil.DEVICE_ID, LibraryUtil.APPLICATION_ID, LibraryUtil.MOBILE_DATA, LibraryUtil.CLIENT_APP_NAME, LibraryUtil.CLIENT_APP_VERSION, LibraryUtil.NMEA_DATA -> source = LibraryUtil.PHONE_SOURCE
            LibraryUtil.ENGINE_TORQUE, LibraryUtil.FUEL_CONSUMPTION_RATE, LibraryUtil.FUEL_TANK_LEVEL_INPUT, LibraryUtil.FUEL_TYPE, LibraryUtil.RPM, LibraryUtil.VEHICLE_ID, LibraryUtil.SPEED -> source = LibraryUtil.OBD_SOURCE
            else -> Timber.tag(LibraryUtil.ERROR_TAG).e("Error: wrong configuration, The %s is not a valid sensor", sensor)
        }
        return source
    }

    private fun isPhoneSensor(sensor: String): Boolean {
        return getSourceForSensor(sensor) == LibraryUtil.PHONE_SOURCE
    }

    private fun isObdSensor(sensor: String): Boolean {
        return getSourceForSensor(sensor) == LibraryUtil.OBD_SOURCE
    }

    fun checkSensors(config: Config) {
        checkPhoneSensors(phoneSensorsManager.sensorTypes, config)
        obdSensorManager?.let {
            checkObdSensors(obdSensorManager.listOfDesiredSensors, config)
        }
    }

    /**
     * Check if phone sensors was registered and no phone source was added.
     * In this case a message error will be displayed
     *
     * @param phoneSensors The registered phone sensors
     */
    private fun checkPhoneSensors(phoneSensors: Array<String>?, config: Config) {
        if (phoneSensors != null && !config.getSourceList().contains(LibraryUtil.PHONE_SOURCE)) {
            for (sensor in phoneSensors) {
                Timber.tag(LibraryUtil.ERROR_TAG).e("Error: wrong configuration The Phone sensor: %s %s", sensor, ERROR_MESSAGE)
            }
        }
    }

    /**
     * Check if obd sensors was registered and no obd source was added.
     * In this case a message error will be displayed
     *
     * @param obdSensors The registered obd sensors
     */
    private fun checkObdSensors(obdSensors: List<String>, config: Config) {
        if (!(config.getSourceList().contains(LibraryUtil.OBD_WIFI_SOURCE) ||
                        config.getSourceList().contains(LibraryUtil.OBD_BLUETOOTH_SOURCE) ||
                        config.getSourceList().contains(LibraryUtil.OBD_BLE_SOURCE))) {
            for (sensor in obdSensors) {
                Timber.tag(LibraryUtil.ERROR_TAG).e("Error: wrong configuration The Obd sensor: %s %s", sensor, ERROR_MESSAGE)
            }
        }
    }

    override fun onConnectionStateChanged(context: Context?, source: String?, statusCode: Int) {
        // No implementation needed
    }

    /**
     * this callback method is used for stopping the uploads to LogShed, when
     * the collection process has been stopped
     *
     *
     * Note: This should NOT be used for doing other operations besides LogShed.
     * When the connection has stopped, the other listeners for connection will deal with
     * library specific operations
     *
     * @param source
     */
    override fun onConnectionStopped(source: String?) {
        //stop logshed logging
    }

    /**
     * [EventDataManager] is notified when the connection to a remote OBD device
     * only when the client has enabled LogShed before starting the collecting session
     *
     * @param context - the application context
     * @param source  - the data source
     */
    override fun onDeviceConnected(context: Context?, source: String?) {
        //empty
    }

    companion object {
        private const val ERROR_MESSAGE = " cannot be retrieved from the specified sources"
    }
}