package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.os.Handler
import android.provider.Settings
import com.telenav.osv.data.collector.datatype.datatypes.DeviceIdObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * DeviceIDCollector class retrieve the device ID.
 * The device ID is lost when the device is rebooted
 */
class DeviceIDCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Retrieve the device ID and send the information to the client.The device ID is lost when the device is rebooted
     * The method is called once when the service is started
     */
    fun sendDeviceID(context: Context) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        if (deviceId != null && deviceId.isNotEmpty()) {
            onNewSensorEvent(DeviceIdObject(deviceId, LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
        } else {
            onNewSensorEvent(DeviceIdObject(deviceId, LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
    }
}