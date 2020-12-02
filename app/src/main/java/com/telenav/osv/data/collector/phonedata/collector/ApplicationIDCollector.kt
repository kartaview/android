package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.ApplicationIdObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import java.util.*

/**
 * ApplicationIDCollector class generates an id used for app identification.
 * The id is removed when the app is uninstalled
 */
class ApplicationIDCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Generate an unique ID and send the information to the client.
     * The ID is stored in shared preferences and it is lost when app is uninstaled
     * The method is called once when the service is started
     */
    fun sendApplicationID(context: Context) {
        val prefs = context
                .getSharedPreferences(PREFS_FILE, 0)
        var id: String? = null
        if (prefs != null) {
            id = prefs.getString(PREFS_DEVICE_ID, null)
        }
        val uuid: UUID
        if (id != null) {
            // Use the ids previously computed and stored in the
            // prefs file
            uuid = UUID.fromString(id)
        } else {
            uuid = UUID.randomUUID()
            prefs?.edit()?.putString(PREFS_DEVICE_ID, uuid.toString())?.apply()
        }
        val applicationIdObject = ApplicationIdObject(uuid.toString(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        onNewSensorEvent(applicationIdObject)
    }

    companion object {
        /**
         * The name of the preferences file used for saving the UUID of the device
         */
        private const val PREFS_FILE = "device_id.xml"

        /**
         * The tag used for retrieve the UUID from the preferences
         */
        private const val PREFS_DEVICE_ID = "device_id"
    }
}