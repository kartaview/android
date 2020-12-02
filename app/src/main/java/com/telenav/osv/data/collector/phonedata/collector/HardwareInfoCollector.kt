package com.telenav.osv.data.collector.phonedata.collector

import android.os.Build
import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.HardwareInfoObject
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener

class HardwareInfoCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler) {
    /**
     * Retrieve the producer and the model of the device and send the information to the client
     * The method is called once when the service is started
     */
    fun sendHardwareInformation() {
        onNewSensorEvent(HardwareInfoObject(Build.MODEL, LibraryUtil.PHONE_SENSOR_READ_SUCCESS))
    }
}