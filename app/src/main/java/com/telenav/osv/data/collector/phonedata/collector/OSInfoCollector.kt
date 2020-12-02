package com.telenav.osv.data.collector.phonedata.collector

import android.os.Build
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.OsObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * OSInfoCollector class retrieves the OS version of the device
 */
class OSInfoCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Retrieve OS version and send the information to the client
     * The method is called once when the service is started
     */
    fun sendOSInformation() {
        val osObject = OsObject(ANDROID + " " + Build.VERSION.RELEASE, LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        onNewSensorEvent(osObject)
    }

    companion object {
        private const val ANDROID = "Android"
    }
}