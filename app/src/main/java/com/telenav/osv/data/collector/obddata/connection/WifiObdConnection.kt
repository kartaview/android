package com.telenav.osv.data.collector.obddata.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import androidx.annotation.NonNull
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.telenav.osv.data.collector.datatype.ObdConnectionListener
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.AbstractClientDataTransmission
import com.telenav.osv.data.collector.obddata.ClientDataTransmissionWifi
import com.telenav.osv.data.collector.obddata.OBDConstants
import com.telenav.osv.data.collector.obddata.manager.OBDSensorManager
import com.telenav.osv.data.collector.obddata.manager.OBDServiceManager
import com.telenav.osv.data.collector.obddata.obdinitializer.ATConstants
import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 *
 */
class WifiObdConnection(private val context: Context) : AbstractObdConnection() {
    private val wifiManager: WifiManager?

    /**
     * Broadcast receiver that is used to determine if the connection was dropped
     */
    private val wifiStatusReceiver: BroadcastReceiver = WifiStatusReceiver()

    /**
     * Current wifi socket used for communication
     */
    private var socket: Socket? = null

    /**
     * Thread used by client to initiate a wifi connection
     */
    private var clientRequestConnectionThread: ClientRequestConnectionThread
    private val mThreadPoolExecutor: ScheduledThreadPoolExecutor
    private var isCollectionRunning = false
    private var isStoppedByClient = false
    override fun connect() {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(wifiStatusReceiver, filter)
    }

    fun stopWifiObdConnection() {
        OBDSensorManager.instance.obdDataListener.clearListeners()
        isCollectionRunning = false
        isStoppedByClient = true
        Timber.tag(TAG).d("stopWifiObdConnection() called. isConnecting=" + clientRequestConnectionThread.isConnecting + " isConnected=" + isCollectionRunning)
        OBDServiceManager.instance.unbindService()
        clientRequestConnectionThread.cancel()
        try {
            context.unregisterReceiver(wifiStatusReceiver)
        } catch (e: IllegalArgumentException) {
            Timber.tag(TAG).e(e)
        }
        onConnectionStoppedNotification()
        val obdConnectionListener = OBDSensorManager.instance.getObdConnectionListeners()
        val abstractClientDataTransmission = OBDSensorManager.instance.getAbstractClientDataTransmission()
        if (!obdConnectionListener.isNullOrEmpty() && abstractClientDataTransmission != null) {
            obdConnectionListener.remove(abstractClientDataTransmission as ObdConnectionListener)
        }
    }

    private fun startConnectionThread() {
        // cancels any thread attempting to make a connection
        Timber.tag(TAG).d("connect() called. isConnecting=" + clientRequestConnectionThread.isConnecting + " isConnected=" + isCollectionRunning)
        if (clientRequestConnectionThread.isConnecting || isCollectionRunning) {
            return
        }
        if (!clientRequestConnectionThread.isAlive && wifiManager != null) {
            clientRequestConnectionThread = ClientRequestConnectionThread(wifiManager)
            clientRequestConnectionThread.start()
        }
    }

    /**
     * Sends notification to all listeners when the connection is stopped
     */
    private fun onConnectionStoppedNotification() {
        for (wifiObdListener in obdConnectionListeners) {
            wifiObdListener.onConnectionStopped(LibraryUtil.OBD_WIFI_SOURCE)
        }

        //notify the transmission class
        if (isStoppedByClient) {
            for (obdConnectionListener in OBDSensorManager.instance.getObdConnectionListeners()) {
                obdConnectionListener.onConnectionStopped(LibraryUtil.OBD_WIFI_SOURCE)
            }
        }
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.activeNetworkInfo != null && manager.activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
    }

    private val isWifiObd: Boolean
        get() {
            val name = wifiManager!!.connectionInfo.ssid
            return wifiManager.isWifiEnabled && (name.contains("OBD") || name.contains("obd") || name.contains("link") || name.contains("LINK"))
        }

    private fun reconnect() {
        clientRequestConnectionThread.cancel()
        onConnectionStoppedNotification()
        startConnectionThread()
    }

    private fun reset() {
        try {
            if (socket != null && socket!!.isConnected && !socket!!.isClosed) {
                Timber.tag(TAG).d("reset: ")
                writeInitCommand(ATConstants.D)
                writeInitCommand(ATConstants.Z)
                writeInitCommand(ATConstants.E0)
                writeInitCommand(ATConstants.L0)
                writeInitCommand(ATConstants.S0)
                setAuto()
            }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun setAuto() {
        mThreadPoolExecutor.execute {
            try {
                if (socket != null && socket!!.isConnected && !socket!!.isClosed) {
                    Timber.tag(TAG).d("setAuto: ")
                    writeInitCommand("AT SP 00")
                    writeInitCommand("AT SS")
                }
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeInitCommand(atCommand: String): String? {
        val rawData: String
        var b: Byte
        val `in` = socket!!.getInputStream()
        val out = socket!!.getOutputStream()
        out.write((atCommand + '\r').toByteArray())
        out.flush()
        val res = StringBuilder()
        delay(100)
        //read until ">" arrives
        val start = System.currentTimeMillis()
        while ((`in`.read().toByte().also { b = it }.toChar() != '>') && res.length < 60 && System.currentTimeMillis() - start < 500) {
            res.append(b.toChar())
        }
        rawData = res.toString().trim { it <= ' ' }
        return if (rawData.contains("CAN ERROR") || rawData.contains("STOPPED")) {
            reconnect()
            null
        } else {
            res.toString()
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a device - client side
     */
    private inner class ClientRequestConnectionThread(@NonNull wifiManager: WifiManager) : Thread("WifiRequestConnectionThread") {
        /**
         * Allows to keep radio wifi on
         */
        @NonNull
        private val wifiLock: WifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, NAME_WIFI_LOCK)
        private val retryIntervalMilliseconds = 4000
        var isConnecting = false
            private set

        override fun run() {
            connectToWifi()
        }

        /**
         * Connects the phone to a wifi obd device
         */
        fun connectToWifi() {
            isConnecting = true
            var success = false
            if (!isConnectedToWifi(context)) {
                if (!wifiManager!!.isWifiEnabled) {
                    isConnecting = false
                    sendErrorOccurredNotification(LibraryUtil.OBD_WIFI_NOT_ENABLED)
                } else {
                    sendErrorOccurredNotification(LibraryUtil.OBD_ERROR_WHILE_CONNECTING)
                }
            } else if (isWifiObd) {
                wifiLock.acquire()
                do {
                    try {
                        socket = Socket(OBDConstants.WIFI_DESTINATION_ADDRESS, OBDConstants.DEST_PORT)
                        delay(500)
                        if (!socket!!.isClosed) {
                            isConnecting = false
                            sendDeviceConnectedNotification()
                            success = true
                            Timber.tag(TAG).d("conntag: Successfully connected to obd")
                            return
                        } else {
                            Timber.tag(TAG).e("Could not open socket")
                        }
                    } catch (e: IOException) {
                        Timber.tag(TAG).e(e)
                        sendErrorOccurredNotification(LibraryUtil.OBD_ERROR_WHILE_CONNECTING)
                    }
                    sendErrorOccurredNotification(LibraryUtil.OBD_REATTEMPT_CONNECTION)
                    delay(retryIntervalMilliseconds)
                } while (!success && !isInterrupted)
            } else {
                sendErrorOccurredNotification(LibraryUtil.OBD_DEVICE_NOT_REACHABLE)
            }
        }

        fun cancel() {
            wifiLock.release()
            isConnecting = false
            try {
                if (socket != null && socket!!.isConnected) {
                    socket!!.close()
                    Timber.tag(TAG).d("socket closed %s", socket.toString())
                }
            } catch (e: IOException) {
                Timber.tag(TAG).e(e)
                sendErrorOccurredNotification(LibraryUtil.OBD_ERROR_WHILE_CLOSING_CONNECTION)
            } finally {
                interrupt()
            }
        }

        /**
         * Sends notification to all listeners when an error occurs
         * @param errorCode Error code sent to client in case of an error
         */
        private fun sendErrorOccurredNotification(errorCode: Int) {
            for (wifiObdListener in obdConnectionListeners) {
                wifiObdListener.onConnectionStateChanged(null, LibraryUtil.OBD_WIFI_SOURCE, errorCode)
            }
        }

        private fun sendDeviceConnectedNotification() {
            for (wifiObdListener in obdConnectionListeners) {
                wifiObdListener?.onDeviceConnected(context, LibraryUtil.OBD_WIFI_SOURCE)
            }

            //create a transmission object and start collection service
            if (!isCollectionRunning) {
                val wifiTransmission: AbstractClientDataTransmission = ClientDataTransmissionWifi(socket, OBDSensorManager.instance.obdDataListener)
                OBDSensorManager.instance.setAbstractClientDataTransmission(wifiTransmission)
            }
            OBDServiceManager.instance.init(context)
            OBDServiceManager.instance.bindService()
            isCollectionRunning = true
        }

        init {
            wifiLock.setReferenceCounted(false)
        }
    }

    private inner class WifiStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.tag(TAG).d("BroadcastReceiver for wifi status")
            if (isConnectedToWifi(context) && isWifiObd) {
                Timber.tag(TAG).d("BroadcastReceiver for wifi status is connected: true")
                startConnectionThread()
            } else {
                isCollectionRunning = false
                OBDSensorManager.instance.getAbstractClientDataTransmission()?.let {
                    it.closeCollectionThread()
                    val obdConnectionListeners = OBDSensorManager.instance.getObdConnectionListeners()
                    if (!obdConnectionListeners.isNullOrEmpty()) {
                        obdConnectionListeners.remove(it as ObdConnectionListener)
                    }
                    onConnectionStoppedNotification()
                }
                OBDServiceManager.instance.unbindService()
                Timber.tag(TAG).d("BroadcastReceiver for wifi status is connected: false")
            }
        }
    }

    private companion object {
        /**
         * Tag used for logging
         */
        private const val TAG = "WifiObdManager"

        private const val NAME_THREAD_POOL_EXECUTOR = "OBDThreadPool"

        private const val NAME_WIFI_LOCK = "HighPerf wifi lock"
    }

    init {
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        mThreadPoolExecutor = ScheduledThreadPoolExecutor(1, ThreadFactoryBuilder().setDaemon(false).setNameFormat(NAME_THREAD_POOL_EXECUTOR)
                .setPriority(Thread.MAX_PRIORITY).build())
        clientRequestConnectionThread = ClientRequestConnectionThread(wifiManager)
    }
}