package com.telenav.osv.data.collector.phonedata.util

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi

class NanosecondsConverter(
        /**
         * Flag used to store the fact that the sensor event value represents the current time
         * in nanoseconds
         */
        private val isCurrentTimestampNano: Boolean) : TimestampConverter() {
    /**
     * This method returns the UTC time of the sensor event reading.
     * Use cases:
     * 1) Event timestamp represents the time passed from device reboot in nanoseconds
     * 2) Event timestamp represents the current time in nanoseconds
     *
     * @param eventTimestamp The time of a sensor event
     * @return The UTC time of the sensor event reading
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun getTimestamp(eventTimestamp: Long): Long {
        var timestamp: Long = 0
        val correction = SystemClock.elapsedRealtimeNanos() - eventTimestamp
        val currentTime = System.currentTimeMillis()
        timestamp = if (isCurrentTimestampNano) {
            eventTimestamp / NANO_TO_MILI // convert from nano to mili
        } else {
            if (correction > 0) {
                currentTime - correction / NANO_TO_MILI
            } else {
                currentTime
            }
        }
        return timestamp
    }

    companion object {
        /**
         * Value used for converting nanoseconds to milliseconds
         */
        private const val NANO_TO_MILI: Long = 1000000
    }
}