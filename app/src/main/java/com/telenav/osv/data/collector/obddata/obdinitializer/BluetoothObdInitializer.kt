package com.telenav.osv.data.collector.obddata.obdinitializer

import android.bluetooth.BluetoothSocket
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.ObdHelper
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by ovidiuc2 on 13.04.2017.
 */
class BluetoothObdInitializer(deviceVersion: String?, obdDataListener: ObdDataListener?, private val bluetoothClientSocket: BluetoothSocket?) : AbstractOBDInitializer(obdDataListener, deviceVersion) {
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    /**
     * sends an AT command to setup the OBD interface(spaces off, echo off)
     *
     * @param sendingCommand
     * @return - OK if the command was executed successfully
     */
    override fun getAtResponse(sendingCommand: String): String? {
        try {
            if (bluetoothClientSocket != null) {
                outputStream = bluetoothClientSocket.outputStream
                inputStream = bluetoothClientSocket.inputStream
                ObdHelper.sendCommand(outputStream, sendingCommand)
                delay(AT_WAITING_TIME)
                return ObdHelper.getRawData(inputStream)
            } else {
                obdDataListener!!.onConnectionStateChanged(LibraryUtil.OBD_BLUETOOTH_SOURCE, LibraryUtil.OBD_SOCKET_ERROR)
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e)
            obdDataListener!!.onConnectionStateChanged(LibraryUtil.OBD_BLUETOOTH_SOURCE, LibraryUtil.OBD_SOCKET_ERROR)
        }
        return null
    }
}