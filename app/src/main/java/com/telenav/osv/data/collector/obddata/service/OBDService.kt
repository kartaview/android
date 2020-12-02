package com.telenav.osv.data.collector.obddata.service

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.annotation.Nullable
import com.telenav.osv.data.collector.obddata.manager.OBDSensorManager
import timber.log.Timber

/**
 * Created by raduh on 9/8/2016.
 */
class OBDService : Service() {
    /**
     * The thread on which service runs (different from main thread)
     */
    private var thread: HandlerThread? = null
    private var mServiceHandler: ServiceHandler? = null
    private val obdBinder: IBinder = ObdServiceBinder()

    @Nullable
    override fun onBind(intent: Intent): IBinder {
        return obdBinder
    }

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        thread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND)
        thread!!.start()
        val mServiceLooper = thread!!.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }

    override fun onDestroy() {
        super.onDestroy()
        thread!!.quit()
        thread!!.interrupt()
        Timber.tag(TAG).d("OVI: thread destroy called")
    }

    fun startSendingCommands() {
        val msg = mServiceHandler!!.obtainMessage()
        mServiceHandler!!.sendMessage(msg)
    }

    private inner class ServiceHandler internal constructor(looper: Looper?) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            OBDSensorManager.instance.getAbstractClientDataTransmission()?.startSendingSensorCommands()
        }
    }

    inner class ObdServiceBinder : Binder() {
        val obdService: OBDService
            get() = this@OBDService
    }

    companion object {
        private val TAG = OBDService::class.java.simpleName
    }
}