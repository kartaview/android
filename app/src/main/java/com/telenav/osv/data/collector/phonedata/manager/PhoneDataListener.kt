package com.telenav.osv.data.collector.phonedata.manager

import com.telenav.osv.data.collector.datatype.datatypes.BaseObject

/**
 * PhoneDataListener interface is used in order to be able to notify the manager when a new value is read from the sensors
 */
interface PhoneDataListener {
    fun onSensorChanged(baseObject: BaseObject<*>)
}