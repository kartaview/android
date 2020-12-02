package com.telenav.osv.data.collector.phonedata.collector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.BatteryObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * BatteryCollector collects the information about battery status
 * It registers a broadcastReceiver in order to monitoring the battery state
 */
class BatteryCollector(private val context: Context, phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * BroadcastReceiver used for detecting any changes on battery
     */
    private var batteryBroadcastReceiver: BroadcastReceiver? = null

    /**
     * Field used to verify if the battery receiver was registerd or not
     */
    private var isBatteryReceiverRegisterd = false

    /**
     * Register a [BroadcastReceiver] for monitoring the battery state.
     * Every change of battery state will notify the receiver
     */
    fun startCollectingBatteryData() {
        if (!isBatteryReceiverRegisterd) {
            batteryBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val batteryObject = BatteryObject(getBatteryLevel(intent), getBatteryState(intent), LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
                    onNewSensorEvent(batteryObject)
                }
            }
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryBroadcastReceiver, intentFilter, null, notifyHandler)
            isBatteryReceiverRegisterd = true
        }
    }

    /**
     * Retrieve the batery level of the device
     *
     * @param intent Intent used for extracting battery information
     * @return The batery level
     */
    fun getBatteryLevel(intent: Intent): Float {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return level / scale.toFloat() * 100
    }

    /**
     * Retrieve the batery state (charging or not) of the device
     *
     * @param intent Intent used for extracting battery information
     * @return The battery state
     */
    fun getBatteryState(intent: Intent): String {
        val state = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = state == BatteryManager.BATTERY_STATUS_CHARGING || state == BatteryManager.BATTERY_STATUS_FULL
        val charger = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val usbCharger = charger == BatteryManager.BATTERY_PLUGGED_USB
        val accCharger = charger == BatteryManager.BATTERY_PLUGGED_AC
        val response = StringBuilder()
        if (isCharging) {
            response.append(CHARGING_MODE_ON)
            if (usbCharger) {
                response.append(USB_CHARGING)
            } else if (accCharger) {
                response.append(ACC_CHARGING)
            }
        } else {
            response.append(CHARGING_MODE_OFF)
        }
        return response.toString()
    }

    fun unregisterReceiver() {
        if (batteryBroadcastReceiver != null && isBatteryReceiverRegisterd) {
            context.unregisterReceiver(batteryBroadcastReceiver)
            isBatteryReceiverRegisterd = false
        }
    }

    companion object {
        /**
         * BleConstants used for determine the battery state
         */
        const val CHARGING_MODE_ON = "Battery is charging via "
        const val CHARGING_MODE_OFF = "Battery is not charging"
        const val USB_CHARGING = "USB"
        const val ACC_CHARGING = "ACC"
    }
}