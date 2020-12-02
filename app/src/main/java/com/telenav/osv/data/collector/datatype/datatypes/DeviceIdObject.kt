package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the Settings.Secure.ANDROID_ID
 */
class DeviceIdObject(deviceId: String?, statusCode: Int) : BaseObject<String?>(deviceId, statusCode, LibraryUtil.DEVICE_ID) {
    /**
     * Returns the device ID.
     * The device ID is lost when the device is rebooted.
     * @return
     */
    val deviceID: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}