package com.telenav.osv.data.collector.obddata.connection

import com.telenav.osv.data.collector.datatype.ObdConnectionListener
import java.util.*

/**
 * class that stores the collection listeners for OBD connection events
 * possible events: DEVICE_CONNECTED, ERROR_OCCURRED, CONNECTION_STOPPED
 */
abstract class AbstractObdConnection {
    var obdConnectionListeners: MutableList<ObdConnectionListener> = ArrayList()

    abstract fun connect()
    fun addObdConnectionListener(obdConnectionListener: ObdConnectionListener) {
        if (!obdConnectionListeners.contains(obdConnectionListener)) {
            obdConnectionListeners.add(obdConnectionListener)
        }
    }

    fun removeObdConnectionListener(obdConnectionListener: ObdConnectionListener?) {
        if (obdConnectionListeners.contains(obdConnectionListener)) {
            obdConnectionListeners.remove(obdConnectionListener)
        }
    }

    /**
     * method used for the pause between retries in case of a connection fail
     *
     * @param ms
     */
    protected fun delay(ms: Int) {
        try {
            Thread.sleep(ms.toLong())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}