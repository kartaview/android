package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the battery level of the device
 */
class BatteryObject(statusCode: Int) : BaseObject<BatteryObject?>(null, statusCode, LibraryUtil.BATTERY) {
    var batteryLevel = 0f
        private set
    var batteryState: String? = null
        private set

    constructor(batteryLevel: Float, batteryState: String?, statusCode: Int) : this(statusCode) {
        this.batteryLevel = batteryLevel
        this.batteryState = batteryState
    }

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}