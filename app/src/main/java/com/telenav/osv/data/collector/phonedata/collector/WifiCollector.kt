package com.telenav.osv.data.collector.phonedata.collector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Handler
import androidx.core.app.ActivityCompat
import com.telenav.osv.data.collector.datatype.datatypes.WifiObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import timber.log.Timber
import java.net.NetworkInterface
import java.util.*
import kotlin.experimental.and

/**
 * WifiCollector class collects information about wifi connection
 */
class WifiCollector(private val context: Context, phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * BroadcastReceiver used for detecting any changes on battery
     */
    private var wifiBroadcastReceiver: BroadcastReceiver? = null

    /**
     * Field used to verify if the wifi receiver was registerd or not
     */
    private var isWifiReceiverRegisterd = false

    /**
     * Register a [BroadcastReceiver] for monitoring the wifi state.
     * Every change of wifi state will notify the receiver
     */
    fun startCollectingWifiData() {
        if (!isWifiReceiverRegisterd && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) === PackageManager.PERMISSION_GRANTED) {
            wifiBroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    onWifiChanged()
                }
            }
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            context.registerReceiver(wifiBroadcastReceiver, intentFilter, null, notifyHandler)
            isWifiReceiverRegisterd = true
        }
    }

    /**
     * Retrieve the wifi information and send the information to the client
     * The method is called every time when the wifi state is changed
     */
    private fun onWifiChanged() {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val wifiObject = WifiObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        wifiObject.wifiState = wifiManager.wifiState
        wifiObject.bssid = wifiInfo.bssid
        wifiObject.ssid = wifiInfo.ssid
        wifiObject.macAddress = macAddress
        wifiObject.ipAddress = getFormatedIpAddres(wifiInfo.ipAddress)
        wifiObject.networkId = wifiInfo.networkId
        onNewSensorEvent(wifiObject)
    }

    /**
     * Converts an integer to the specific IP address
     *
     * @param ip Integer that represents the IP address
     * @return The formated IP address
     */
    fun getFormatedIpAddres(ip: Int): String {
        return String.format(
                Locale.US,
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff)
    }

    fun unregisterReceiver() {
        if (wifiBroadcastReceiver != null && isWifiReceiverRegisterd) {
            context.unregisterReceiver(wifiBroadcastReceiver)
            isWifiReceiverRegisterd = false
        }
    }

    companion object {
        /**
         * Fake mac address used for OS versions > 5
         */
        private const val FAKE_MAC_ADDRESS = "02:00:00:00:00:00"
        private const val TAG = "WifiCollector"
        private const val WLAN_NAME = "wlan0"

        /**
         * Extract the mac address of the device if it is accessible
         *
         * @return The real mac address is the OS version is < 6 or a fake mac address if not
         */
        private val macAddress: String
            private get() {
                try {
                    val networkInterfaceList: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
                    for (networkInterface in networkInterfaceList) {
                        if (!networkInterface.name.equals(WLAN_NAME, ignoreCase = true)) continue
                        val macAddressBytes = networkInterface.hardwareAddress ?: return ""
                        val macAddress = StringBuilder()
                        for (b in macAddressBytes) {
                            macAddress.append(Integer.toHexString((b and 0xFF.toByte()).toInt())).append(":")
                        }
                        if (macAddress.isNotEmpty()) {
                            macAddress.deleteCharAt(macAddress.length - 1)
                        }
                        return macAddress.toString()
                    }
                } catch (ex: Exception) {
                    Timber.tag(TAG).e(ex)
                }
                return FAKE_MAC_ADDRESS
            }
    }
}