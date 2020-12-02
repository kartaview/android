package com.telenav.osv.data.collector.datatype

import android.content.Context
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.HardwareSource
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.ObdStatusCode

/**
 *
 */
interface ObdConnectionListener {
    fun onConnectionStateChanged(context: Context?, @HardwareSource source: String?, @ObdStatusCode statusCode: Int)
    fun onConnectionStopped(@HardwareSource source: String?)
    fun onDeviceConnected(context: Context?, @HardwareSource source: String?)

    companion object {
        val EMPTY: ObdConnectionListener = object : ObdConnectionListener {
            override fun onConnectionStateChanged(context: Context?, source: String?, statusCode: Int) {}
            override fun onConnectionStopped(source: String?) {}
            override fun onDeviceConnected(context: Context?, source: String?) {}
        }
    }
}