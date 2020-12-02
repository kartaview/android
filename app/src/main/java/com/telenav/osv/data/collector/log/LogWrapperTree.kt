package com.telenav.osv.data.collector.log

import android.util.Log
import timber.log.Timber

class LogWrapperTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t == null) {
            Log.println(priority, tag, message)
        } else {
            Log.println(priority, tag, """
     $message
     ${Log.getStackTraceString(t)}
     """.trimIndent())
        }
    }
}