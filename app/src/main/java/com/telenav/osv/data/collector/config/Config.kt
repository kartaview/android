package com.telenav.osv.data.collector.config

import android.bluetooth.BluetoothDevice
import androidx.annotation.NonNull
import com.telenav.osv.data.collector.datatype.EventDataListener
import com.telenav.osv.data.collector.datatype.ObdConnectionListener
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.SensorsFrequency
import java.util.*
import kotlin.collections.ArrayList

/**
 * Config class stores the information needed for library configuration.
 * It allows the user to specify what data should be collected.
 */
class Config private constructor(
        /**
         * Bluetooth device used for connection
         */
        val bluetoothDevice: BluetoothDevice?,
        /**
         * Needed in order to establish a ble obd connection
         */
        val bleMacAddress: String?,
        val sourceList: List<String> = ArrayList(),
        val obdConnectionListener: ObdConnectionListener = ObdConnectionListener.EMPTY,
        val dataListeners: Map<EventDataListener, MutableList<String>> = HashMap(),
        val datafrequency: Map<String, Int>) {

    /**
     * Map that contains the sensor and the collection frequency
     */
    var sensorsFrequencies: Map<String, Int> = HashMap()

    /**
     * List of the sources that should be used in order to collect data
     */
    @NonNull
    fun getSourceList(): Collection<String> {
        return sourceList
    }

    class Builder {
        private val obdSources = listOf(LibraryUtil.OBD_BLE_SOURCE, LibraryUtil.OBD_BLUETOOTH_SOURCE, LibraryUtil.OBD_WIFI_SOURCE)
        private var bluetoothDevice: BluetoothDevice? = null
        private var bleMacAddress: String? = null
        private var isProductionEnabled = false
        private var logAllPhoneDataFlag = false
        private val sourceList: MutableList<String> = ArrayList()
        private var obdConnectionListener: ObdConnectionListener = ObdConnectionListener.EMPTY
        private val dataListeners: MutableMap<EventDataListener, MutableList<String>> = HashMap()
        private val sensorFrequencies: MutableMap<String, Int> = HashMap()
        fun setBluetoothDevice(@NonNull bluetoothDevice: BluetoothDevice?): Builder {
            this.bluetoothDevice = bluetoothDevice
            return this
        }

        fun setBleMacAddress(@NonNull bleMacAddress: String?): Builder {
            this.bleMacAddress = bleMacAddress
            return this
        }

        fun setProductionEnabled(isProductionEnabled: Boolean): Builder {
            this.isProductionEnabled = isProductionEnabled
            return this
        }

        fun setLogAllPhoneData(logAllPhoneDataFlag: Boolean): Builder {
            this.logAllPhoneDataFlag = logAllPhoneDataFlag
            return this
        }

        fun addSource(@NonNull @LibraryUtil.DataSource source: String): Builder {
            if (!sourceList.contains(source)) {
                if (obdSources.contains(source)) {
                    removeExistingObdSource()
                }
                sourceList.add(source)
            }
            return this
        }

        fun setObdConnectionListener(@NonNull obdConnectionListener: ObdConnectionListener): Builder {
            this.obdConnectionListener = obdConnectionListener
            return this
        }

        fun addDataListener(@NonNull listener: EventDataListener, @NonNull @AvailableData sensor: String): Builder {
            var sensors = dataListeners[listener]
            if (sensors == null) {
                sensors = ArrayList()
            }
            if (!sensors.contains(sensor)) {
                sensors.add(sensor)
            }
            dataListeners[listener] = sensors
            return this
        }

        fun addSensorFrequency(@NonNull @AvailableData sensor: String, @SensorsFrequency frequency: Int): Builder {
            sensorFrequencies[sensor] = frequency
            return this
        }

        fun build(): Config {
            validateInput()
            return Config(bluetoothDevice, bleMacAddress, sourceList, obdConnectionListener, dataListeners, sensorFrequencies)
        }

        private fun validateInput() {
            if (sourceList.isEmpty()) {
                throwException("Please add at least one data source")
            }
            if (bluetoothDevice == null && sourceList.contains(LibraryUtil.OBD_BLUETOOTH_SOURCE)) {
                throwException("Please set the bluetooth device")
            }
            if (sourceList.contains(LibraryUtil.OBD_BLE_SOURCE)
                    && (bleMacAddress == null || bleMacAddress!!.isEmpty())) {
                throwException("Please set the BLE mac address")
            }
        }

        private fun throwException(message: String) {
            throw IllegalArgumentException(message)
        }

        private fun removeExistingObdSource() {
            val iterator = sourceList.iterator()
            while (iterator.hasNext()) {
                val source = iterator.next()
                if (obdSources.contains(source)) {
                    iterator.remove()
                }
            }
        }
    }

    init {
        sensorsFrequencies = datafrequency
    }
}