package com.telenav.osv.data.collector.obddata.obdinitializer

import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.ObdHelper
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 * Created by ovidiuc2 on 13.04.2017.
 */
class WifiObdInitializer(deviceVersion: String?, obdDataListener: ObdDataListener?,
                         /**
                          * Wifi socket
                          */
                         private val wifiSocket: Socket?) : AbstractOBDInitializer(obdDataListener, deviceVersion) {
    /**
     * sends an AT command to setup the OBD interface(spaces off, echo off)
     *
     * @param sendingCommand
     * @return - OK if the command was executed successfully
     */
    override fun getAtResponse(sendingCommand: String): String? {
        val outputStream: OutputStream
        val inputStream: InputStream
        return try {
            if (wifiSocket != null) {
                outputStream = wifiSocket.getOutputStream()
                inputStream = wifiSocket.getInputStream()
            } else {
                obdDataListener!!.onConnectionStateChanged(LibraryUtil.OBD_WIFI_SOURCE, LibraryUtil.OBD_SOCKET_ERROR)
                return null
            }
            ObdHelper.sendCommand(outputStream, sendingCommand)
            delay(AT_WAITING_TIME)
            ObdHelper.getRawData(inputStream)
        } catch (e: IOException) {
            Timber.tag(TAG).e(e, "IOException when sending AT command")
            obdDataListener!!.onConnectionStateChanged(LibraryUtil.OBD_WIFI_SOURCE, LibraryUtil.OBD_SOCKET_ERROR)
            null
        }
    }
}