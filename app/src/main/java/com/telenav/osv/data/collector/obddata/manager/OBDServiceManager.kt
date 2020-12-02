package com.telenav.osv.data.collector.obddata.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.telenav.osv.data.collector.obddata.service.OBDService
import com.telenav.osv.data.collector.obddata.service.OBDService.ObdServiceBinder
import timber.log.Timber

/**
 *
 */
class OBDServiceManager private constructor() {
    private var mContext: Context? = null
    private var mServiceIntent: Intent? = null
    private var obdService: OBDService? = null
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val obdServiceBinder: ObdServiceBinder = service as ObdServiceBinder
            obdService = obdServiceBinder.obdService
            obdService!!.startSendingCommands()
            Timber.tag(TAG).d("OBD service connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Timber.tag(TAG).d("OBD service disconnected")
        }
    }

    fun init(context: Context?) {
        mContext = context
        mServiceIntent = Intent(mContext, OBDService::class.java)
    }

    fun bindService() {
        if (mContext != null) {
            mContext!!.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (mContext != null) {
            mContext!!.unbindService(mServiceConnection)
            mServiceIntent = null
            obdService = null
            mContext = null
        }
    }

    companion object {
        private const val TAG = "ObdServiceManager"
        val instance = OBDServiceManager()
    }
}