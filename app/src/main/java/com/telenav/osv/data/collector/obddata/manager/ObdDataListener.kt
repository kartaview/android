package com.telenav.osv.data.collector.obddata.manager

import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.HardwareSource

/**
 *
 */
interface ObdDataListener {
    fun onSensorChanged(baseObject: BaseObject<*>)
    fun onConnectionStateChanged(@HardwareSource dataSource: String, statusCode: Int)
    fun onConnectionStopped(@HardwareSource source: String)
    fun onInitializationFailedWarning()
    fun requestSensorFrequencies(): MutableMap<String, Int>
    fun clearListeners()
}