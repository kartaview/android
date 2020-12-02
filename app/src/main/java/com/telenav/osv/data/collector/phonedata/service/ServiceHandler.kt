package com.telenav.osv.data.collector.phonedata.service

import android.content.Context
import android.os.*
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.collector.*
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import timber.log.Timber

class ServiceHandler internal constructor(looper: Looper?, private val context: Context?) : Handler(looper) {
    private var accelerometerCollector: AccelerometerCollector? = null
    private var linearAccelerationCollector: LinearAccelerationCollector? = null
    private var gyroCollector: GyroCollector? = null
    private var compassCollector: CompassCollector? = null
    private var headingCollector: HeadingCollector? = null
    private var pressureCollector: PressureCollector? = null
    private var gravityCollector: GravityCollector? = null
    private var rotationVectorCollector: RotationVectorCollector? = null
    private var gameRotationVectorCollector: GameRotationVectorCollector? = null
    private var temperatureCollector: TemperatureCollector? = null
    private var lightCollector: LightCollector? = null
    private var stepCounterCollector: StepCounterCollector? = null
    private var proximityCollector: ProximityCollector? = null
    private var humidityCollector: HumidityCollector? = null
    private var gpsCollector: GPSCollector? = null
    private var batteryCollector: BatteryCollector? = null
    private var wifiCollector: WifiCollector? = null
    private var mobileDataCollector: MobileDataCollector? = null
    private var hardwareInfoCollector: HardwareInfoCollector? = null
    private var osInfoCollector: OSInfoCollector? = null
    private var deviceIDCollector: DeviceIDCollector? = null
    private var applicationIDCollector: ApplicationIDCollector? = null
    private var clientAppNameCollector: ClientAppNameCollector? = null
    private var clientAppVersionCollector: ClientAppVersionCollector? = null

    /**
     * handler thread used for moving phone sensor collection off the UI thread
     */
    private val phoneHandlerThread: HandlerThread = HandlerThread("PhoneHandlerThread")

    /**
     * Default frequency for phone sensors
     */
    private val defaultFrequency = 0
    private var phoneDataListener: PhoneDataListener? = null
    override fun handleMessage(msg: Message) {
        val bundle = msg.data
        if (bundle != null && !bundle.isEmpty) {
            handleSensorOperation(bundle)
        }
    }

    /**
     * Sets the data listener in order to be able to notify about updates
     * when a sensor event occurs
     *
     * @param phoneDataListener The listener that has to be notified
     */
    fun setDataListener(phoneDataListener: PhoneDataListener?) {
        this.phoneDataListener = phoneDataListener
    }

    fun cleanup() {
        unregisterAllSensors()
        phoneHandlerThread.quit()
    }

    /**
     * Unregisters all registered sensors
     */
    fun unregisterAllSensors() {
        if (accelerometerCollector != null) {
            accelerometerCollector!!.unregisterListener()
        }
        if (linearAccelerationCollector != null) {
            linearAccelerationCollector!!.unregisterListener()
        }
        if (compassCollector != null) {
            compassCollector!!.unregisterListener()
        }
        if (gpsCollector != null) {
            gpsCollector!!.unregisterListener()
        }
        if (gravityCollector != null) {
            gravityCollector!!.unregisterListener()
        }
        if (gyroCollector != null) {
            gyroCollector!!.unregisterListener()
        }
        if (humidityCollector != null) {
            humidityCollector!!.unregisterListener()
        }
        if (headingCollector != null) {
            headingCollector!!.unregisterListener()
        }
        if (lightCollector != null) {
            lightCollector!!.unregisterListener()
        }
        if (pressureCollector != null) {
            pressureCollector!!.unregisterListener()
        }
        if (proximityCollector != null) {
            proximityCollector!!.unregisterListener()
        }
        if (rotationVectorCollector != null) {
            rotationVectorCollector!!.unregisterListener()
        }
        if (gameRotationVectorCollector != null) {
            gameRotationVectorCollector!!.unregisterListener()
        }
        if (stepCounterCollector != null) {
            stepCounterCollector!!.unregisterListener()
        }
        if (temperatureCollector != null) {
            temperatureCollector!!.unregisterListener()
        }
        if (batteryCollector != null) {
            batteryCollector!!.unregisterReceiver()
        }
        if (wifiCollector != null) {
            wifiCollector!!.unregisterReceiver()
        }
    }

    /**
     * Handles a later sensor registration/unregistration or a later frequency setting
     *
     * @param b The data sent from the client
     */
    private fun handleSensorOperation(b: Bundle) {
        val operation = b.getString(LibraryUtil.SENSOR_OPERATION_TAG)
        if (operation != null) {
            when (operation) {
                LibraryUtil.REGISTER_SENSOR_TAG -> handleSensorsRegistration(b)
                LibraryUtil.SET_FREQUENCY_TAG -> handleSensorsFrequency(b)
                LibraryUtil.UNREGISTER_SENSOR_TAG -> handleSensorsUnregistration(b)
                else -> Timber.tag(TAG).e("Unknown sensor operation!")
            }
        }
    }

    /**
     * Handles later registration of sensors from client
     *
     * @param bundle The data sent from the client
     */
    private fun handleSensorsRegistration(bundle: Bundle?) {
        if (bundle != null) {
            registerSensors(bundle.getStringArray(LibraryUtil.SENSOR_TYPE_TAG), bundle.getSerializable(LibraryUtil.FREQUENCY_TAG) as Map<String, Int>)
        }
    }

    /**
     * Handles later frequency setting from client
     *
     * @param bundle The data sent from the client
     */
    private fun handleSensorsFrequency(bundle: Bundle) {
        val sensortype = bundle.getString(LibraryUtil.SENSOR_TYPE_TAG)
        val frequency = bundle.getInt(LibraryUtil.FREQUENCY_TAG)
        sensortype?.let { setFrequency(it, frequency) }
    }

    /**
     * Handles later unregistration of sensors from client
     *
     * @param bundle The data sent from the client
     */
    private fun handleSensorsUnregistration(bundle: Bundle) {
        val sensorsType = bundle.getStringArray(LibraryUtil.SENSOR_TYPE_TAG)
        if (sensorsType != null) {
            for (sensor in sensorsType) {
                unregisterSensor(sensor)
            }
        }
    }

    /**
     * Iterates an array of strings in order to register the sensors that should
     * be collected
     *
     * @param sensors          String array with sensors that has to be registered
     * @param sensorsFrequency Map used to extract the registered frequency for each sensor
     */
    private fun registerSensors(sensors: Array<String>?, sensorsFrequency: Map<String, Int>?) {
        if (sensors != null && sensors.size > 0 && !sensors[0].isEmpty() && context != null) {
            for (sensor in sensors) {
                var frequency = 0
                if (sensorsFrequency != null) {
                    frequency = sensorsFrequency[sensor]!!
                }
                registerSensor(sensor, frequency)
            }
        }
    }

    /**
     * Registers a specific sensor at the specified frequency
     *
     * @param sensor    The sensor that should be registered
     * @param frequency The desired frequency of the sensor
     */
    private fun registerSensor(sensor: String, frequency: Int) {
        when (sensor) {
            LibraryUtil.ACCELEROMETER -> {
                if (accelerometerCollector == null) {
                    accelerometerCollector = AccelerometerCollector(phoneDataListener, this)
                }
                accelerometerCollector!!.registerAccelerometerListener(context!!, frequency)
            }
            LibraryUtil.LINEAR_ACCELERATION -> {
                if (linearAccelerationCollector == null) {
                    linearAccelerationCollector = LinearAccelerationCollector(phoneDataListener, this)
                }
                linearAccelerationCollector!!.registerLinearAccelerationListener(context!!, frequency)
            }
            LibraryUtil.GYROSCOPE -> {
                if (gyroCollector == null) {
                    gyroCollector = GyroCollector(phoneDataListener, this)
                }
                gyroCollector!!.registerGyroListener(context!!, frequency)
            }
            LibraryUtil.GRAVITY -> {
                if (gravityCollector == null) {
                    gravityCollector = GravityCollector(phoneDataListener, this)
                }
                gravityCollector!!.registerGravityListener(context!!, frequency)
            }
            LibraryUtil.PHONE_GPS_ACCURACY, LibraryUtil.PHONE_GPS_ALTITUDE, LibraryUtil.PHONE_GPS_BEARING, LibraryUtil.PHONE_GPS_SPEED, LibraryUtil.PHONE_GPS, LibraryUtil.GPS_DATA, LibraryUtil.NMEA_DATA -> {
                if (gpsCollector == null) {
                    gpsCollector = GPSCollector(phoneDataListener, this)
                }
                gpsCollector!!.registerLocationListener(context!!)
            }
            LibraryUtil.HUMIDITY -> {
                if (humidityCollector == null) {
                    humidityCollector = HumidityCollector(phoneDataListener, this)
                }
                humidityCollector!!.registerHumiditytListener(context!!, frequency)
            }
            LibraryUtil.HEADING -> {
                if (headingCollector == null) {
                    headingCollector = HeadingCollector(phoneDataListener, this)
                }
                headingCollector!!.registerHeadingListener(context!!, frequency)
            }
            LibraryUtil.LIGHT -> {
                if (lightCollector == null) {
                    lightCollector = LightCollector(phoneDataListener, this)
                }
                lightCollector!!.registerLightListener(context!!, frequency)
            }
            LibraryUtil.MAGNETIC -> {
                if (compassCollector == null) {
                    compassCollector = CompassCollector(phoneDataListener, this)
                }
                compassCollector!!.registerCompassListener(context!!, frequency)
            }
            LibraryUtil.PRESSURE -> {
                if (pressureCollector == null) {
                    pressureCollector = PressureCollector(phoneDataListener, this)
                }
                pressureCollector!!.registerPressureListener(context!!, frequency)
            }
            LibraryUtil.PROXIMITY -> {
                if (proximityCollector == null) {
                    proximityCollector = ProximityCollector(phoneDataListener, this)
                }
                proximityCollector!!.registerProximityListener(context!!, frequency)
            }
            LibraryUtil.ROTATION_VECTOR_NORTH_REFERENCE -> {
                if (rotationVectorCollector == null) {
                    rotationVectorCollector = RotationVectorCollector(phoneDataListener, this)
                }
                rotationVectorCollector!!.registerRotationVectorListener(context!!, frequency)
            }
            LibraryUtil.ROTATION_VECTOR_RAW -> {
                if (gameRotationVectorCollector == null) {
                    gameRotationVectorCollector = GameRotationVectorCollector(phoneDataListener, this)
                }
                gameRotationVectorCollector!!.registerGameRotationVectorListener(context!!, frequency)
            }
            LibraryUtil.STEP_COUNT -> {
                if (stepCounterCollector == null) {
                    stepCounterCollector = StepCounterCollector(phoneDataListener, this)
                }
                stepCounterCollector!!.registerStepCounterListener(context!!, frequency)
            }
            LibraryUtil.TEMPERATURE -> {
                if (temperatureCollector == null) {
                    temperatureCollector = TemperatureCollector(phoneDataListener, this)
                }
                temperatureCollector!!.registerTemperatureListener(context!!, frequency)
            }
            LibraryUtil.BATTERY -> {
                if (batteryCollector == null) {
                    batteryCollector = BatteryCollector(context!!, phoneDataListener, this)
                }
                batteryCollector!!.startCollectingBatteryData()
            }
            LibraryUtil.HARDWARE_TYPE -> {
                if (hardwareInfoCollector == null) {
                    hardwareInfoCollector = HardwareInfoCollector(phoneDataListener, this)
                }
                hardwareInfoCollector!!.sendHardwareInformation()
            }
            LibraryUtil.OS_INFO -> {
                if (osInfoCollector == null) {
                    osInfoCollector = OSInfoCollector(phoneDataListener, this)
                }
                osInfoCollector!!.sendOSInformation()
            }
            LibraryUtil.DEVICE_ID -> {
                if (deviceIDCollector == null) {
                    deviceIDCollector = DeviceIDCollector(phoneDataListener, this)
                }
                deviceIDCollector!!.sendDeviceID(context!!)
            }
            LibraryUtil.APPLICATION_ID -> {
                if (applicationIDCollector == null) {
                    applicationIDCollector = ApplicationIDCollector(phoneDataListener, this)
                }
                applicationIDCollector!!.sendApplicationID(context!!)
            }
            LibraryUtil.CLIENT_APP_NAME -> {
                if (clientAppNameCollector == null) {
                    clientAppNameCollector = ClientAppNameCollector(phoneDataListener, this)
                }
                clientAppNameCollector!!.sendClientAppName(context!!)
            }
            LibraryUtil.CLIENT_APP_VERSION -> {
                if (clientAppVersionCollector == null) {
                    clientAppVersionCollector = ClientAppVersionCollector(phoneDataListener, this)
                }
                clientAppVersionCollector!!.sendClientVersion(context)
            }
            LibraryUtil.MOBILE_DATA -> {
                if (mobileDataCollector == null) {
                    mobileDataCollector = MobileDataCollector(context!!, phoneDataListener, this)
                }
                mobileDataCollector!!.sendMobileDataInformation()
            }
            LibraryUtil.WIFI -> {
                if (wifiCollector == null) {
                    wifiCollector = WifiCollector(context!!, phoneDataListener, this)
                }
                wifiCollector!!.startCollectingWifiData()
            }
            else -> Timber.tag(TAG).e(LibraryUtil.INVALID_DATA + " : " + sensor)
        }
    }

    /**
     * Unregister a sensor
     *
     * @param sensor The type of the sensor that has to be unregisterd
     */
    private fun unregisterSensor(sensor: String) {
        when (sensor) {
            LibraryUtil.ACCELEROMETER -> if (accelerometerCollector != null) {
                accelerometerCollector!!.unregisterListener()
            }
            LibraryUtil.LINEAR_ACCELERATION -> if (linearAccelerationCollector != null) {
                linearAccelerationCollector!!.unregisterListener()
            }
            LibraryUtil.GYROSCOPE -> if (gyroCollector != null) {
                gyroCollector!!.unregisterListener()
            }
            LibraryUtil.GRAVITY -> if (gravityCollector != null) {
                gravityCollector!!.unregisterListener()
            }
            LibraryUtil.PHONE_GPS_ACCURACY, LibraryUtil.PHONE_GPS_ALTITUDE, LibraryUtil.PHONE_GPS_BEARING, LibraryUtil.PHONE_GPS_SPEED, LibraryUtil.PHONE_GPS -> if (gpsCollector != null) {
                gpsCollector!!.unregisterListener()
            }
            LibraryUtil.HUMIDITY -> if (humidityCollector != null) {
                humidityCollector!!.unregisterListener()
            }
            LibraryUtil.HEADING -> if (headingCollector != null) {
                headingCollector!!.unregisterListener()
            }
            LibraryUtil.LIGHT -> if (lightCollector != null) {
                lightCollector!!.unregisterListener()
            }
            LibraryUtil.MAGNETIC -> if (compassCollector != null) {
                compassCollector!!.unregisterListener()
            }
            LibraryUtil.PRESSURE -> if (pressureCollector != null) {
                pressureCollector!!.unregisterListener()
            }
            LibraryUtil.PROXIMITY -> if (proximityCollector != null) {
                proximityCollector!!.unregisterListener()
            }
            LibraryUtil.ROTATION_VECTOR_NORTH_REFERENCE -> if (rotationVectorCollector != null) {
                rotationVectorCollector!!.unregisterListener()
            }
            LibraryUtil.ROTATION_VECTOR_RAW -> if (gameRotationVectorCollector != null) {
                gameRotationVectorCollector!!.unregisterListener()
            }
            LibraryUtil.STEP_COUNT -> if (stepCounterCollector != null) {
                stepCounterCollector!!.unregisterListener()
            }
            LibraryUtil.TEMPERATURE -> if (temperatureCollector != null) {
                temperatureCollector!!.unregisterListener()
            }
            LibraryUtil.BATTERY -> if (batteryCollector != null) {
                batteryCollector!!.unregisterReceiver()
            }
            LibraryUtil.WIFI -> if (wifiCollector != null) {
                wifiCollector!!.unregisterReceiver()
            }
            LibraryUtil.APPLICATION_ID, LibraryUtil.CLIENT_APP_NAME, LibraryUtil.CLIENT_APP_VERSION, LibraryUtil.DEVICE_ID, LibraryUtil.HARDWARE_TYPE, LibraryUtil.MOBILE_DATA, LibraryUtil.OS_INFO -> {
            }
            else -> Timber.tag(TAG).e("The sensor $sensor is not valid")
        }
    }

    /**
     * Set sensor frequency
     *
     * @param type The type of the sensor that has to be unregisterd
     */
    private fun setFrequency(type: String, frequency: Int) {
        when (type) {
            LibraryUtil.ACCELEROMETER -> if (accelerometerCollector != null && context != null) {
                accelerometerCollector!!.unregisterListener()
                accelerometerCollector!!.registerAccelerometerListener(context, frequency)
            }
            LibraryUtil.LINEAR_ACCELERATION -> if (linearAccelerationCollector != null && context != null) {
                linearAccelerationCollector!!.unregisterListener()
                linearAccelerationCollector!!.registerLinearAccelerationListener(context, frequency)
            }
            LibraryUtil.GYROSCOPE -> if (gyroCollector != null && context != null) {
                gyroCollector!!.unregisterListener()
                gyroCollector!!.registerGyroListener(context, frequency)
            }
            LibraryUtil.GRAVITY -> if (gravityCollector != null && context != null) {
                gravityCollector!!.unregisterListener()
                gravityCollector!!.registerGravityListener(context, frequency)
            }
            LibraryUtil.HUMIDITY -> if (humidityCollector != null && context != null) {
                humidityCollector!!.unregisterListener()
                humidityCollector!!.registerHumiditytListener(context, frequency)
            }
            LibraryUtil.HEADING -> if (headingCollector != null && context != null) {
                headingCollector!!.unregisterListener()
                headingCollector!!.registerHeadingListener(context, frequency)
            }
            LibraryUtil.LIGHT -> if (lightCollector != null && context != null) {
                lightCollector!!.unregisterListener()
                lightCollector!!.registerLightListener(context, frequency)
            }
            LibraryUtil.MAGNETIC -> if (compassCollector != null && context != null) {
                compassCollector!!.unregisterListener()
                compassCollector!!.registerCompassListener(context, frequency)
            }
            LibraryUtil.PRESSURE -> if (pressureCollector != null && context != null) {
                pressureCollector!!.unregisterListener()
                pressureCollector!!.registerPressureListener(context, frequency)
            }
            LibraryUtil.PROXIMITY -> if (proximityCollector != null && context != null) {
                proximityCollector!!.unregisterListener()
                proximityCollector!!.registerProximityListener(context, frequency)
            }
            LibraryUtil.ROTATION_VECTOR_NORTH_REFERENCE -> if (rotationVectorCollector != null && context != null) {
                rotationVectorCollector!!.unregisterListener()
                rotationVectorCollector!!.registerRotationVectorListener(context, frequency)
            }
            LibraryUtil.ROTATION_VECTOR_RAW -> if (gameRotationVectorCollector != null && context != null) {
                gameRotationVectorCollector!!.unregisterListener()
                gameRotationVectorCollector!!.registerGameRotationVectorListener(context, frequency)
            }
            LibraryUtil.STEP_COUNT -> if (stepCounterCollector != null && context != null) {
                stepCounterCollector!!.unregisterListener()
                stepCounterCollector!!.registerStepCounterListener(context, frequency)
            }
            LibraryUtil.TEMPERATURE -> if (temperatureCollector != null && context != null) {
                temperatureCollector!!.unregisterListener()
                temperatureCollector!!.registerTemperatureListener(context, frequency)
            }
            else -> Timber.tag(TAG).e("The sensor $type is not valid")
        }
    }

    /**
     * Register all sensors with a default frequency
     */
    private fun registerAllSensors() {
        if (context != null) {
            if (accelerometerCollector == null) {
                accelerometerCollector = AccelerometerCollector(phoneDataListener, this)
            }
            if (linearAccelerationCollector == null) {
                linearAccelerationCollector = LinearAccelerationCollector(phoneDataListener, this)
            }
            if (gyroCollector == null) {
                gyroCollector = GyroCollector(phoneDataListener, this)
            }
            if (compassCollector == null) {
                compassCollector = CompassCollector(phoneDataListener, this)
            }
            if (headingCollector == null) {
                headingCollector = HeadingCollector(phoneDataListener, this)
            }
            if (pressureCollector == null) {
                pressureCollector = PressureCollector(phoneDataListener, this)
            }
            if (gravityCollector == null) {
                gravityCollector = GravityCollector(phoneDataListener, this)
            }
            if (rotationVectorCollector == null) {
                rotationVectorCollector = RotationVectorCollector(phoneDataListener, this)
            }
            if (gameRotationVectorCollector == null) {
                gameRotationVectorCollector = GameRotationVectorCollector(phoneDataListener, this)
            }
            if (temperatureCollector == null) {
                temperatureCollector = TemperatureCollector(phoneDataListener, this)
            }
            if (lightCollector == null) {
                lightCollector = LightCollector(phoneDataListener, this)
            }
            if (stepCounterCollector == null) {
                stepCounterCollector = StepCounterCollector(phoneDataListener, this)
            }
            if (proximityCollector == null) {
                proximityCollector = ProximityCollector(phoneDataListener, this)
            }
            if (humidityCollector == null) {
                humidityCollector = HumidityCollector(phoneDataListener, this)
            }
            if (gpsCollector == null) {
                gpsCollector = GPSCollector(phoneDataListener, this)
            }
            if (batteryCollector == null) {
                batteryCollector = BatteryCollector(context, phoneDataListener, this)
            }
            if (wifiCollector == null) {
                wifiCollector = WifiCollector(context, phoneDataListener, this)
            }
            if (mobileDataCollector == null) {
                mobileDataCollector = MobileDataCollector(context, phoneDataListener, this)
            }
            if (hardwareInfoCollector == null) {
                hardwareInfoCollector = HardwareInfoCollector(phoneDataListener, this)
            }
            if (osInfoCollector == null) {
                osInfoCollector = OSInfoCollector(phoneDataListener, this)
            }
            if (deviceIDCollector == null) {
                deviceIDCollector = DeviceIDCollector(phoneDataListener, this)
            }
            if (applicationIDCollector == null) {
                applicationIDCollector = ApplicationIDCollector(phoneDataListener, this)
            }
            if (clientAppNameCollector == null) {
                clientAppNameCollector = ClientAppNameCollector(phoneDataListener, this)
            }
            if (clientAppVersionCollector == null) {
                clientAppVersionCollector = ClientAppVersionCollector(phoneDataListener, this)
            }
            accelerometerCollector!!.registerAccelerometerListener(context, defaultFrequency)
            linearAccelerationCollector!!.registerLinearAccelerationListener(context, defaultFrequency)
            gyroCollector!!.registerGyroListener(context, defaultFrequency)
            compassCollector!!.registerCompassListener(context, defaultFrequency)
            headingCollector!!.registerHeadingListener(context, defaultFrequency)
            pressureCollector!!.registerPressureListener(context, defaultFrequency)
            gravityCollector!!.registerGravityListener(context, defaultFrequency)
            rotationVectorCollector!!.registerRotationVectorListener(context, defaultFrequency)
            gameRotationVectorCollector!!.registerGameRotationVectorListener(context, defaultFrequency)
            temperatureCollector!!.registerTemperatureListener(context, defaultFrequency)
            lightCollector!!.registerLightListener(context, defaultFrequency)
            stepCounterCollector!!.registerStepCounterListener(context, defaultFrequency)
            proximityCollector!!.registerProximityListener(context, defaultFrequency)
            humidityCollector!!.registerHumiditytListener(context, defaultFrequency)
            batteryCollector!!.startCollectingBatteryData()
            mobileDataCollector!!.sendMobileDataInformation()
            hardwareInfoCollector!!.sendHardwareInformation()
            osInfoCollector!!.sendOSInformation()
            deviceIDCollector!!.sendDeviceID(context)
            applicationIDCollector!!.sendApplicationID(context)
            clientAppNameCollector!!.sendClientAppName(context)
            clientAppVersionCollector!!.sendClientVersion(context)
            wifiCollector!!.startCollectingWifiData()
            gpsCollector!!.registerLocationListener(context)
        }
    }

    companion object {
        private const val TAG = "PhoneServiceHandler"
    }

    init {
        phoneHandlerThread.start()
    }
}