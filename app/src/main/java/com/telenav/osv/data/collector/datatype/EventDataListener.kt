package com.telenav.osv.data.collector.datatype

import com.telenav.osv.data.collector.datatype.datatypes.BaseObject

/**
 *
 */
interface EventDataListener {
    fun onNewEvent(baseObject: BaseObject<*>?)
}