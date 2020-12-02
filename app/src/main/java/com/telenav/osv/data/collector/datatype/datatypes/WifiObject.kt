package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves current wifi connection information
 */
class WifiObject(statusCode: Int) : BaseObject<WifiObject?>(null, statusCode, LibraryUtil.WIFI) {
    /**
     * State of wifi: 0 - disabling, 1 - disabled, 2 - enabling, 3 - enabled, 4 - unknown
     */
    var wifiState = 0

    /**
     * Basic service set identifier.BSSID is the MAC address of the wireless access point
     */
    var bssid: String? = null

    /**
     * Service Set Identifier. This is the name given to a wireless network
     */
    var ssid: String? = null

    /**
     * Phisical address of the device
     */
    var macAddress: String? = null

    /**
     * Ip addres of the device
     */
    var ipAddress: String? = null

    /**
     * Unique value, used to identify the network
     */
    var networkId = 0

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}