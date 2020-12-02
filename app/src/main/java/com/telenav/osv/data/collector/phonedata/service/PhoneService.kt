package com.telenav.osv.data.collector.phonedata.service

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.annotation.Nullable
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import java.io.Serializable

/**
 * PhoneService class register/unregister phone sensors.
 * It is used in order to start/stop the collection process
 */
class PhoneService : Service() {
    /**
     * The thread on which service runs (different from main thread)
     */
    private var thread: HandlerThread? = null

    /**
     * Tha class that starts the sensors collection on separate thread
     */
    private var mServiceHandler: ServiceHandler? = null
    private val phoneBinder: IBinder = PhoneServiceBinder()

    @Nullable
    override fun onBind(intent: Intent): IBinder {
        return phoneBinder
    }

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        thread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND)
        thread!!.start()
        val mServiceLooper = thread!!.looper
        mServiceHandler = ServiceHandler(mServiceLooper, applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceHandler!!.cleanup()
        thread!!.quit()
        thread = null
    }

    fun startCollecting(sensors: Array<String>, frequency: Map<String, Int?>?, phoneDataListener: PhoneDataListener?) {
        mServiceHandler!!.setDataListener(phoneDataListener)
        val bundle = Bundle()
        bundle.putStringArray(LibraryUtil.SENSOR_TYPE_TAG, sensors)
        bundle.putString(LibraryUtil.SENSOR_OPERATION_TAG, LibraryUtil.REGISTER_SENSOR_TAG)
        bundle.putSerializable(LibraryUtil.FREQUENCY_TAG, frequency as Serializable?)
        val message: Message = mServiceHandler!!.obtainMessage()
        message.data = bundle
        mServiceHandler!!.sendMessage(message)
    }

    /**
     * Call the method for registering a new sensor
     *
     * @param sensorType The type of the sensor that has to be registered
     */
    fun registerSensor(sensorType: Array<String>?) {
        val bundle = Bundle()
        bundle.putStringArray(LibraryUtil.SENSOR_TYPE_TAG, sensorType)
        bundle.putString(LibraryUtil.SENSOR_OPERATION_TAG, LibraryUtil.REGISTER_SENSOR_TAG)
        val message: Message = mServiceHandler!!.obtainMessage()
        message.data = bundle
        mServiceHandler!!.sendMessage(message)
    }

    /**
     * Unregister a sensor
     *
     * @param sensorType Type of the sensor that has to be unregistered
     */
    fun unregisterSensor(sensorType: Array<String>?) {
        val bundle = Bundle()
        bundle.putStringArray(LibraryUtil.SENSOR_TYPE_TAG, sensorType)
        bundle.putString(LibraryUtil.SENSOR_OPERATION_TAG, LibraryUtil.UNREGISTER_SENSOR_TAG)
        val message: Message = mServiceHandler!!.obtainMessage()
        message.data = bundle
        mServiceHandler!!.sendMessage(message)
    }

    /**
     * Set the frequency for a specific sensor
     *
     * @param type      The type of the sensor
     * @param frequency The frequency that has to be set
     */
    fun setSensorFrequency(type: String?, frequency: Int) {
        val bundle = Bundle()
        bundle.putString(LibraryUtil.SENSOR_TYPE_TAG, type)
        bundle.putInt(LibraryUtil.FREQUENCY_TAG, frequency)
        bundle.putString(LibraryUtil.SENSOR_OPERATION_TAG, LibraryUtil.SET_FREQUENCY_TAG)
        val message: Message = mServiceHandler!!.obtainMessage()
        message.data = bundle
        mServiceHandler!!.sendMessage(message)
    }

    inner class PhoneServiceBinder : Binder() {
        val phoneService: PhoneService
            get() = this@PhoneService
    }
}