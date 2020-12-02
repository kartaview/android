package com.telenav.osv.utils

import timber.log.Timber

/**
 * This object provides utility methods for logs
 */
object LogUtils {

    /** Log a warning message with tag if available. */
    @JvmStatic
    fun logWarning(tag: String?, message: String) {
        tag?.let { Timber.tag(it) }
        Timber.w(message)
    }

    /** Log an error message with tag if available. */
    @JvmStatic
    fun logError(tag: String?, message: String) {
        tag?.let { Timber.tag(it) }
        Timber.e(message)
    }

    /** Log an assert message with tag if available. */
    @JvmStatic
    fun logCritical(tag: String?, message: String) {
        tag?.let { Timber.tag(it) }
        Timber.wtf(message)
    }

    /** Log an info message with tag if available. */
    @JvmStatic
    fun logInfo(tag: String?, message: String) {
        tag?.let { Timber.tag(it) }
        Timber.i(message)
    }

    /** Log a debug message with tag if available. */
    @JvmStatic
    fun logDebug(tag: String?, message: String) {
        tag?.let { Timber.tag(it) }
        Timber.d(message)
    }
}