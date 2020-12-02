package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.ClientAppVersionObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import timber.log.Timber

/**
 * Collects the version of the client application
 */
class ClientAppVersionCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Retrieve the client app name
     */
    fun sendClientVersion(context: Context?) {
        var pinfo: PackageInfo? = null
        var clientVersion: String? = null
        try {
            if (context != null && context.packageManager != null && context.packageName != null) {
                pinfo = context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (pinfo != null) {
                clientVersion = pinfo.versionName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.tag("ClientVersionCollector").e(e)
        }
        if (clientVersion != null && !clientVersion.isEmpty()) {
            onNewSensorEvent(ClientAppVersionObject(clientVersion, LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
        } else {
            onNewSensorEvent(ClientAppVersionObject(clientVersion, LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
    }
}