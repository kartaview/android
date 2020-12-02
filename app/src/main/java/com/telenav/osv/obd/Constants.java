package com.telenav.osv.obd;

/**
 * Created by dianat on 3/18/2016.
 */
public class Constants {

    /**
     * shared preferences used for this module
     */
    public static final String PREF = "OBD2_PREF";

    /**
     * ble gatt discovered intent action
     */
    public final static String ACTION_GATT_DISCOVERED = "com.bluetooth.le.ACTION_GATT_DISCOVERED";

    /**
     * ble gatt disconnected intent action
     */
    public final static String ACTION_GATT_DISCONNECTED = "com.bluetooth.le.ACTION_GATT_DISCONNECTED";

    /**
     * ble gatt subscribed intent action
     */
    public final static String ACTION_GATT_SUBSCRIBED = "com.bluetooth.le.ACTION_GATT_SUBSCRIBED";

    /**
     * ble gatt data available intent action
     */
    public final static String ACTION_DATA_AVAILABLE = "com.bluetooth.le.ACTION_DATA_AVAILABLE";

    /**
     * ble gatt extra data
     */
    public final static String EXTRA_DATA = "com.bluetooth.le.EXTRA_DATA";

    /**
     * background service baterry control state
     */
    public static final String BACKGROUND_SERVICE_BATTERY_CONTROL = "BACKGROUND_SERVICE_BATTERY_CONTROL";

    /**
     * address of the device shared preference key
     */
    public static final String EXTRAS_BLE_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";

    /**
     * address of the device shared preference key
     */
    public static final String EXTRAS_BT_DEVICE_ADDRESS = "BT_DEVICE_ADDRESS";

    /**
     * last connection status shared preference key
     */
    public static final String LAST_BLE_CONNECTION_STATUS = "LAST_BLE_CONNECTION_STATUS";

    /**
     * last connection status shared preference key
     */
    public static final String LAST_BT_CONNECTION_STATUS = "LAST_BT_CONNECTION_STATUS";

    public static final int STATUS_CONNECTING = 2;

    /**
     * bletooth ble service started shared preference key
     */
    public static final String BLE_SERVICE_STARTED = "BLE_SERVICE_STARTED";

    public static final String BT_SERVICE_STARTED = "BT_SERVICE_STARTED";

    /**
     * Argument used for pics passing.
     */
    public static final String ARG_PICS = "PICS";

    /**
     * Argument used for points passing.
     */
    public static final String ARG_POINTS = "POINTS";

    /**
     * Argument used for distance passing.
     */
    public static final String ARG_DISTANCE = "DISTANCE";

    public static final String ARG_DISTANCE_LABEL = "DISTANCE_LABEL";

    /**
     * Argument used for recording time passing.
     */
    public static final String ARG_RECORDING_TIME = "TIME";

    public static final String ARG_SIZE = "SIZE";

    public static final String ARG_SIZE_LABEL = "SIZE_LABEL";

    public static final String ARG_GPS_ICON = "GPS_ICON";
}
