package com.telenav.osv.data.collector.obddata

import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.obddata.connection.BleObdDataListener
import com.telenav.osv.data.collector.obddata.connection.OBDCommunication
import com.telenav.osv.data.collector.obddata.manager.OBDServiceManager
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import com.telenav.osv.data.collector.obddata.obdinitializer.ATConstants
import com.telenav.osv.data.collector.obddata.obdinitializer.AbstractOBDInitializer
import com.telenav.osv.data.collector.obddata.obdinitializer.BleObdInitializer
import com.telenav.osv.utils.StringUtils
import timber.log.Timber

/**
 * Created by adrianbostan on 11/10/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class ClientDataTransmissionBle(obdDataListener: ObdDataListener) : AbstractClientDataTransmission(obdDataListener), BleObdDataListener {
    /**
     * ble connection thread instance
     */
    private val bleCommunicationThread: ObdCollectionThread?

    /**
     * result from the written characteristic
     */
    private var characteristicResult = ""

    /**
     * result from the written characteristic and used in entire module
     */
    private var characteristicResultBle: String? = null

    /**
     * Sensors availabilities
     */
    private var availabilities: Map<String, Boolean>? = null
    private var atThread: Thread? = null
    private var isInitializing = false
    override fun onCharacteristicChangedBle(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        formCharacteristicResult(characteristic)
        Timber.tag(TAG).d("Ble status: onCharacteristicChangedBle: %s", characteristic)
    }

    override fun onConnectionStateChanged(context: Context?, @LibraryUtil.HardwareSource source: String?, @LibraryUtil.ObdStatusCode statusCode: Int) {
        if (source == LibraryUtil.OBD_BLE_SOURCE) {
            Timber.tag(TAG).d("Ble status: onBleErrorOccurred: %s", statusCode)
        }
    }

    override fun onConnectionStopped(@LibraryUtil.HardwareSource source: String?) {
        if (source == LibraryUtil.OBD_BLE_SOURCE) {
            Timber.tag(TAG).d("Ble status: onBleConnectionStopped")
            OBDServiceManager.instance.unbindService()
            stopSendingSensorCommands()
            resetBleFields()

            //clear obd listeners
            obdDataListener.onConnectionStopped(LibraryUtil.OBD_BLE_SOURCE)
        }
    }

    private fun resetBleFields() {
        characteristicResult = ""
        characteristicResultBle = ""
    }

    override fun onDeviceConnected(context: Context?, source: String?) {
        throw UnsupportedOperationException("The onDeviceConnected method should not be called from ClientDataTransmissionBle")
    }

    /**
     * initialize and start the communication thread
     */
    override fun startSendingSensorCommands() {
        receiveObdVersion()
        startToWriteCommands()
    }

    /**
     * stop the communication thread
     */
    override fun stopSendingSensorCommands() {
        Timber.tag(TAG).d("OVI: threads state: isInitializing=" + isInitializing + ", isRetrievingVin=" + isRetrievingVin + ", thread id=" + Thread.currentThread().id)
        if (isInitializing && atThread != null) {
            atThread!!.interrupt()
        }
        if (isRetrievingVin && vinThread != null) {
            vinThread!!.interrupt()
        }
        if (bleCommunicationThread != null) {
            bleCommunicationThread.setWasCollectionStopped(true)
            bleCommunicationThread.cancel()
        }
    }

    override fun initializationFailed() {
        ObdHelper.notifyInitializationFailed(obdDataListener)
    }

    override fun onCollectionThreadRestartRequired() {
        bleCommunicationThread?.onFrequencyChanged()
    }

    override fun closeCollectionThread() {
        bleCommunicationThread!!.cancel()
    }

    override fun writeCommand(sendingCommand: String) {
        sendCommandToBle(sendingCommand)
        delay(getDelayForCommand(sendingCommand))
    }

    /**
     * Formats the response that comes on characteristic
     * @param characteristic - Characteristic that is received
     */
    private fun formCharacteristicResult(characteristic: BluetoothGattCharacteristic) {
        characteristicResult += characteristic.getStringValue(0)
        Timber.tag(TAG).d("onCharacteristicChangedBle characteristicResult: %s", characteristicResult)
        if (characteristicResult.trim { it <= ' ' }.endsWith(">")) {
            Timber.tag(TAG).d("onCharacteristicChangedBle characteristicResult >: %s", characteristicResult)
            characteristicResult = formatCharacteristicResult(characteristicResult)
            characteristicResultForAbstract = characteristicResult
            characteristicResultBle = characteristicResult

            //notify listeners of the baseObject read
            val baseObject = ObdHelper.convertResult(characteristicResult)
            if (baseObject != null) {
                obdDataListener.onSensorChanged(baseObject)
            }
            characteristicResult = StringUtils.EMPTY_STRING
        }
    }

    /**
     * Remove unused strings from the characteristic
     * @param characteristicResult - Characteristic that is received
     * @return - formatted characteristic
     */
    fun formatCharacteristicResult(characteristicResult: String): String {
        var result = characteristicResult
        if (result.contains(">")) {
            result = result.replace(">".toRegex(), "")
        }
        if (result.contains("\r")) {
            result = result.replace("\r".toRegex(), "")
        }
        if (result.contains(" ")) {
            result = result.replace(" ".toRegex(), "")
        }
        return result
    }

    /**
     * Sends a command to ble obd
     * @param sendingCommand Command to be sent
     */
    private fun sendCommandToBle(sendingCommand: String): Boolean {
        Timber.tag(TAG).d("Ble status: sendCommandToBle %s", sendingCommand)
        val isWritten: Boolean
        val gatt: BluetoothGatt? = OBDCommunication.instance.bluetoothGatt
        isWritten = if (gatt != null) {
            Timber.tag(TAG).d("Ble status: sendCommandToBle gatt != null")
            val bluetoothGattService = gatt.getService(OBDCommunication.serviceUUID)
            if (bluetoothGattService == null) {
                obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_BLE_SOURCE, LibraryUtil.BLUETOOTH_ADAPTER_OFF)
                Timber.tag(TAG).d("Ble stop start")
                false
            } else {
                val writeChar = bluetoothGattService.getCharacteristic(OBDCommunication.characteristicUUID)
                if (writeChar == null) {
                    obdDataListener.onConnectionStateChanged(LibraryUtil.OBD_BLE_SOURCE, LibraryUtil.BLUETOOTH_ADAPTER_OFF)
                    Timber.tag(TAG).d("Ble stop start")
                    false
                } else {
                    writeChar.setValue(sendingCommand + '\r')
                    gatt.writeCharacteristic(writeChar)
                }
            }
        } else {
            false
        }
        return isWritten
    }

    /**
     * Starts the thread that sends command
     */
    private fun startToWriteCommands() {
        //the vehicle id is only collected once, before the other sensors
        delay(500)
        collectVehicleId()
        if (availabilities != null) {
            bleCommunicationThread?.setAvailabilities(availabilities!!)
            bleCommunicationThread?.start()
        }
    }

    /**
     * Takes the current version of the interface and add the availability of the sensors
     */
    private fun receiveObdVersion() {
        //this thread will run the AT Z command, and will receive the OBD device version
        val initial = System.currentTimeMillis()
        isInitializing = true
        atThread = Thread {
            val obdVersion: String = getObdDeviceVersion(ATConstants.CONNECTION_BLE)
            val abstractOBDInitializer: AbstractOBDInitializer = BleObdInitializer(obdVersion, this@ClientDataTransmissionBle, obdDataListener)
            initializeObd(obdVersion, abstractOBDInitializer)
        }
        atThread!!.start()
        isInitializing = false
        try {
            atThread!!.join()
        } catch (e: InterruptedException) {
            Timber.tag(TAG).e(e, "Exception while receiving OBD version %s", e.message)
            Thread.currentThread().interrupt()
        }
        atThread!!.interrupt()

        //find out which sensors are available
        //availabilities = AvailabilityRetriever.retrieveAvailabilityMap(new SensorAvailabilityBle(this));
        availabilities = defaultMap
        if (availabilities == null) {
            obdDataListener.onConnectionStopped(LibraryUtil.OBD_BLE_SOURCE)
            return
        }

        //notify unavailable sensors
        for ((key, value) in availabilities!!) {
            if (!value) {
                ObdHelper.notifySensorNotAvailable(key, obdDataListener)
            }
        }
        Timber.tag(TAG).d("Init time %s", System.currentTimeMillis() - initial)
    }

    /**
     * Returns the characteristic result
     * @return Resulted characteristic
     */
    fun getCharacteristicResult(): String? {
        return characteristicResultBle
    }

    companion object {
        /**
         * Tag used for logging
         */
        private val TAG = ClientDataTransmissionBle::class.java.simpleName
    }

    /**
     * Private constructor
     */
    init {
        bleCommunicationThread = ObdCollectionThread()
    }
}