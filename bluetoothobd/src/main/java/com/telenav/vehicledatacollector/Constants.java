package com.telenav.vehicledatacollector;

/**
 * Created by dianat on 3/18/2016.
 */
public class Constants {

    /**
     * shared preferences used for this module
     */
    public static final String PREF = "OBD2_PREF";

    /**
     * low battery intent action
     */
    public final static String ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW";

    /**
     * okay battery intent action
     */
    public final static String ACTION_BATTERY_OKAY = "android.intent.action.BATTERY_OKAY";

    /**
     * periodic task intent action
     */
    public final static String ACTION_PERIODIC_TASK = "com.bluetooth.le.PERIODIC_TASK";

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
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    /**
     * last connection status shared preference key
     */
    public static final String LAST_CONNECTION_STATUS = "LAST_CONNECTION_STATUS";

    /**
     * connection statuses
     */
    public static final int STATUS_DISCONNECTED = 0;

    public static final int STATUS_CONNECTED = 1;

    public static final int STATUS_CONNECTING = 2;

    /**
     * last datetime of connection status changed shared preference key
     */
    public static final String LAST_CONNECTION_DATETIME = "LAST_CONNECTION_DATETIME";

    /**
     * datetime format of the connection state
     */
    public static final String CONNECTION_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * last rpm value shared preference key
     */
    public static final String LAST_RPM_VALUE = "LAST_RPM_VALUE";

    /**
     * last rpm value received time shared preference key
     */
    public static final String LAST_RPM_VALUE_TIME = "LAST_RPM_VALUE_TIME";

    /**
     * task interval period shared preference key
     */
    public static final String TASK_INTERVAL_PERIOD = "TASK_INTERVAL_PERIOD";

    /**
     * bletooth ble service started shared preference key
     */
    public static final String BLE_SERVICE_STARTED = "BLE_SERVICE_STARTED";

    public static final String SPEED_DATA = "SPEED";
}
