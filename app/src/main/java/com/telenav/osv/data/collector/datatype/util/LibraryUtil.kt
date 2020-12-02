package com.telenav.osv.data.collector.datatype.util

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 *
 */
object LibraryUtil {
    /**
     * The tags presented below are used in order to know what sensor
     * has to be registered in the service .
     */
    const val ACCELEROMETER = "accelerometer"
    const val LINEAR_ACCELERATION = "linear_acceleration"
    const val BATTERY = "battery"
    const val GYROSCOPE = "gyroscope"
    const val GRAVITY = "gravity"
    const val PHONE_GPS = "gps"
    const val PHONE_GPS_ACCURACY = "gps_accuracy"
    const val PHONE_GPS_ALTITUDE = "gps_altitude"
    const val PHONE_GPS_BEARING = "gps_bearing"
    const val PHONE_GPS_SPEED = "gps_speed"
    const val HUMIDITY = "humidity"
    const val LIGHT = "light"
    const val MAGNETIC = "magnetic"
    const val HEADING = "heading"
    const val PRESSURE = "pressure"
    const val PROXIMITY = "proximity"
    const val ROTATION_VECTOR_NORTH_REFERENCE = "rotationVector"
    const val ROTATION_VECTOR_RAW = "gameRotationVector"
    const val STEP_COUNT = "stepCount"
    const val WIFI = "wifi"
    const val TEMPERATURE = "temperature"
    const val HARDWARE_TYPE = "hardware_type"
    const val OS_INFO = "device_OS"
    const val DEVICE_ID = "device_ID"
    const val APPLICATION_ID = "application_ID"
    const val MOBILE_DATA = "mobile_data"
    const val CLIENT_APP_NAME = "client_app_name"
    const val CLIENT_APP_VERSION = "client_app_version"
    const val GPS_DATA = "gps_data"
    const val NMEA_DATA = "nmea_data"

    /**
     * Tags for Openxc Sensors
     */
    const val ACCELERATOR_PEDAL_POSITION = "acceleratorPedalPosition"
    const val BRAKE_PEDAL_STATUS = "brakePedalStatus"
    const val STEERING_WHEEL_ANGLE = "steeringWheelAngle"
    const val TORQUE_AT_TRANSMISSION = "torqueAtTransmission"
    const val ENGINE_SPEED = "engineSpeed"
    const val VEHICLE_SPEED = "vehicleSpeed"
    const val PARKING_BRAKE_STATUS = "parkingBrakeStatus"
    const val TRANSMISSION_GEAR_POSITION = "transmissionGearPosition"
    const val ODOMETER = "odometer"
    const val IGNITION_STATUS = "ignitionStatus"
    const val FUEL_LEVEL = "fuelLevel"
    const val FUEL_CONSUMED_SINCE_RESTART = "fuelConsumedSinceRestart"
    const val DOOR_STATUS = "doorStatus"
    const val HEADLAMP_STATUS = "headlampStatus"
    const val HIGHBEAM_STATUS = "highbeamStatus"
    const val WINDSHIELD_WIPER_STATUS = "winshieldWiperStatus"

    /**
     * Tags for OBD sensors
     */
    const val ENGINE_TORQUE = "engineTorque"
    const val FUEL_CONSUMPTION_RATE = "fuelConsumptionRate"
    const val FUEL_TANK_LEVEL_INPUT = "fuelTankLevelInput"
    const val FUEL_TYPE = "fuelType"
    const val RPM = "rpm"
    const val SPEED = "speed"
    const val VEHICLE_ID = "vehicleId"

    /**
     * Frequencies for phone sensors
     */
    const val F_100HZ = 10000 // delay between readings in microseconds = 10 milliseconds
    const val F_50HZ = 20000 // delay between readings in microseconds = 20 milliseconds
    const val F_25HZ = 40000 // delay between readings in microseconds = 40 milliseconds
    const val F_10HZ = 100000 // delay between readings in microseconds = 100 milliseconds
    const val F_5HZ = 200000 // delay between readings in microseconds = 200 milliseconds

    /**
     * Frequencies for OBD sensors
     */
    const val OBD_FAST = 4
    const val OBD_SLOW = 5

    /**
     * Tags used for late sensor registration
     */
    const val SENSOR_OPERATION_TAG = "operation"
    const val REGISTER_SENSOR_TAG = "registerSensor"
    const val UNREGISTER_SENSOR_TAG = "unregisterSensorFromListener"
    const val SET_FREQUENCY_TAG = "setFrequency"
    const val FREQUENCY_TAG = "frequency"
    const val SENSOR_TYPE_TAG = "sensorType"

    /**
     * Tags used for determine the source of data
     */
    const val OBD_SOURCE = "obd_source"
    const val OBD_BLUETOOTH_SOURCE = "obd_bluetooth_source"
    const val OBD_WIFI_SOURCE = "obd_wifi_source"
    const val OBD_BLE_SOURCE = "obd_ble_source"
    const val PHONE_SOURCE = "phone_source"

    /**
     * Tags used for phone error codes
     */
    const val PHONE_SENSOR_NOT_AVAILABLE = 303
    const val PHONE_SENSOR_READ_SUCCESS = 304

    /**
     * Tags used for OBD error codes
     */
    const val OBD_INITIALIZATION_FAILURE = 201
    const val OBD_AVAILABLE = 202
    const val OBD_NOT_AVAILABLE = 203
    const val OBD_READ_SUCCESS = 204
    const val OBD_READ_FAILURE = 205

    /**
     * Error codes for OBD error codes
     */
    const val OBD_ERROR_WHILE_CLOSING_CONNECTION = 206
    const val OBD_ERROR_WHILE_CONNECTING = 207
    const val OBD_WIFI_NOT_ENABLED = 208
    const val OBD_SOCKET_ERROR = 209
    const val OBD_REATTEMPT_CONNECTION = 210
    const val BLUETOOTH_NOT_ENABLED = 211
    const val BLUETOOTH_ADAPTER_OFF = 213
    const val BLUETOOTH_NOT_SUPPORTED = 214
    const val OBD_DEVICE_NOT_REACHABLE = 215

    /**
     * Tags used for error codes description
     */
    const val SENSOR_NOT_AVAILABLE_DESCRIPTION = "Sensor not available"
    const val SENSOR_AVAILABLE_DESCRIPTION = "Sensor is available"
    const val SENSOR_INITIALIZATION_FAILURE_DESCRIPTION = "OBD initialization failed. In case of invalid data, restart collection"
    const val SENSOR_READ_SUCCESS_DESCRIPTION = "Sensor collected successfully"
    const val SENSOR_READ_FAILURE_DESCRIPTION = "Sensor not read"

    /**
     * Tags used for error/warning messages
     */
    const val NO_SOURCE_ADDED = "Error: incomplete configuration, no source was added"
    const val NO_BLUETOOTH_DEVICE_ADDED = "Error: incomplete configuration no bluetooth device was added"
    const val NO_MAC_ADDRESS_ADDED = "Error: incomplete configuration no mac address was set for BLE"
    const val NO_WIFI_OBD_CONNECTION_LISTENER_ADDED = "Error: incomplete configuration no wifi obd connection listener added"
    const val NO_BLUETOOTH_OBD_CONNECTION_LISTENER_ADDED = "Error: incomplete configuration no bluetooth obd connection listener added"
    const val NO_BLE_OBD_CONNECTION_LISTENER_ADDED = "Error: incomplete configuration No ble obd connection listener added"
    const val BL_ADAPTER_NOT_INITIALIZED = "BluetoothAdapter not initialized or unspecified address."
    const val BL_DEVICE_NOT_FOUND = "Device not found.  Unable to connect."
    const val BL_INVALID_MAC_ADDRESS = "Invalid mac address"
    const val INVALID_DATA = "Invalid data"
    const val ERROR_TAG = "ERROR"
    /**
     * Tags used for LogShed uploading options
     */
    /**
     * the default uploading option
     * Events are only sent to LogShed when there is a WiFi connection available
     */
    const val UPLOAD_ON_WIFI = "uploadOnWifi"

    /**
     * Used for define the tags that have to be added to the strings which
     * contains what kind of sensor data client wants to receive
     */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef(ACCELEROMETER, LINEAR_ACCELERATION, BATTERY, GYROSCOPE, GRAVITY, PHONE_GPS, PHONE_GPS_ACCURACY, PHONE_GPS_ALTITUDE, PHONE_GPS_BEARING, PHONE_GPS_SPEED, HUMIDITY, LIGHT, MAGNETIC, HEADING, PRESSURE, PROXIMITY, ROTATION_VECTOR_NORTH_REFERENCE, ROTATION_VECTOR_RAW, STEP_COUNT, WIFI, TEMPERATURE, HARDWARE_TYPE, OS_INFO, DEVICE_ID, APPLICATION_ID, MOBILE_DATA, CLIENT_APP_NAME, CLIENT_APP_VERSION)
    annotation class PhoneSensors

    /**
     * Defines the tags for obd sensor types
     */
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(ENGINE_TORQUE, FUEL_CONSUMPTION_RATE, FUEL_TANK_LEVEL_INPUT, FUEL_TYPE, RPM, SPEED, VEHICLE_ID)
    annotation class ObdSensors

    /**
     * Used for define the tags for phone sensors frequency
     */
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(F_100HZ, F_50HZ, F_25HZ, F_10HZ, F_5HZ)
    annotation class PhoneSensorsFrequency

    /**
     * Used to define the tags for OBD sensor frequencies
     */
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(OBD_FAST, OBD_SLOW)
    annotation class ObdSensorsFrequency

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(OBD_SOURCE, PHONE_SOURCE)
    annotation class DataSource

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(OBD_BLUETOOTH_SOURCE, OBD_WIFI_SOURCE, OBD_BLE_SOURCE, PHONE_SOURCE)
    annotation class HardwareSource

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(OBD_WIFI_SOURCE, OBD_BLUETOOTH_SOURCE, OBD_BLE_SOURCE)
    annotation class ObdSourceListener

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(ACCELEROMETER, LINEAR_ACCELERATION, BATTERY, GYROSCOPE, GRAVITY, PHONE_GPS, PHONE_GPS_ACCURACY, PHONE_GPS_ALTITUDE, PHONE_GPS_BEARING, PHONE_GPS_SPEED, HUMIDITY, LIGHT, MAGNETIC, HEADING, PRESSURE, PROXIMITY, ROTATION_VECTOR_NORTH_REFERENCE, ROTATION_VECTOR_RAW, STEP_COUNT, WIFI, TEMPERATURE, HARDWARE_TYPE, OS_INFO, DEVICE_ID, APPLICATION_ID, MOBILE_DATA, ACCELERATOR_PEDAL_POSITION, STEERING_WHEEL_ANGLE, TORQUE_AT_TRANSMISSION, ENGINE_SPEED, VEHICLE_SPEED, PARKING_BRAKE_STATUS, BRAKE_PEDAL_STATUS, TRANSMISSION_GEAR_POSITION, ODOMETER, IGNITION_STATUS, FUEL_LEVEL, FUEL_CONSUMED_SINCE_RESTART, DOOR_STATUS, HEADLAMP_STATUS, HIGHBEAM_STATUS, WINDSHIELD_WIPER_STATUS, CLIENT_APP_VERSION, GPS_DATA, NMEA_DATA, ENGINE_TORQUE, FUEL_CONSUMPTION_RATE, FUEL_TANK_LEVEL_INPUT, FUEL_TYPE, RPM, SPEED, VEHICLE_ID, CLIENT_APP_NAME)
    annotation class AvailableData

    /**
     * Used for define the tags for all sensors frequency
     */
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(F_100HZ, F_50HZ, F_25HZ, F_10HZ, F_5HZ, OBD_FAST, OBD_SLOW)
    annotation class SensorsFrequency

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(OBD_ERROR_WHILE_CLOSING_CONNECTION, OBD_ERROR_WHILE_CONNECTING, OBD_WIFI_NOT_ENABLED, OBD_SOCKET_ERROR, OBD_REATTEMPT_CONNECTION, BLUETOOTH_NOT_ENABLED, BLUETOOTH_ADAPTER_OFF, BLUETOOTH_NOT_SUPPORTED, OBD_DEVICE_NOT_REACHABLE)
    annotation class ObdStatusCode
}