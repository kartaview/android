package com.telenav.osv.data.collector.phonedata.collector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.telephony.TelephonyManager
import androidx.annotation.StringDef
import com.telenav.osv.data.collector.datatype.datatypes.MobileDataObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

/**
 * MobileDataCollector class retrieves information about mobile data connection
 */
class MobileDataCollector(private val context: Context, phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Retrieve information about mobile data connection and sent it to the client
     * The method is called once when the service is started
     */
    fun sendMobileDataInformation() {
        val connectionType = StringBuilder()
        if (isMobileConnected) {
            connectionType.append(networkOperatorName).append(" ").append(networkClass)
        } else {
            connectionType.append("Mobile data is not available")
        }
        onNewSensorEvent(MobileDataObject(connectionType.toString(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
    }

    /**
     * Returns the type of the connection: 2G, 3G, 4G
     *
     * @return
     */
    @get:NetworkType
    val networkClass: String
        get() {
            val mTelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkType = mTelephonyManager.networkType
            return when (networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> NETWORK_2G
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> NETWORK_3G
                TelephonyManager.NETWORK_TYPE_LTE -> NETWORK_4G
                else -> UNKNOWN_NETWORK
            }
        }

    /**
     * @return true, if mobile is connected
     */
    private val isMobileConnected: Boolean
        get() {
            var info: NetworkInfo? = null
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (cm != null) {
                info = cm.activeNetworkInfo
            }
            return info != null && info.isConnected && info.type == ConnectivityManager.TYPE_MOBILE
        }

    /**
     * @return network operator name (carrier info)
     */
    private val networkOperatorName: String
        get() {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return manager.networkOperatorName
        }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @StringDef(NETWORK_2G, NETWORK_3G, NETWORK_4G, UNKNOWN_NETWORK)
    internal annotation class NetworkType
    companion object {
        /**
         * BleConstants used for identify the mobile data connection
         */
        const val NETWORK_4G = "4G"
        const val NETWORK_3G = "3G"
        const val NETWORK_2G = "2G"
        const val UNKNOWN_NETWORK = "UNKNOWN_CELLULAR"
    }
}