package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.ClientAppNameObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * Collects the client app name
 */
class ClientAppNameCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Retrieve the client app name
     */
    fun sendClientAppName(context: Context) {
        val appName = context.packageName
        if (appName != null && !appName.isEmpty()) {
            onNewSensorEvent(ClientAppNameObject(appName, LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
        } else {
            onNewSensorEvent(ClientAppNameObject(appName, LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
    }
}