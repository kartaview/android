package com.telenav.osv.data.collector.phonedata.util

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi

class MicrosecondsConverter : TimestampConverter() {
    /**
     * This method returns the UTC time of the sensor event reading.
     * Use case: event timestamp represents the time passed from device reboot in microseconds
     *
     * @param eventTimestamp The time of a sensor event
     * @return The UTC time of the sensor event reading
     */
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun getTimestamp(eventTimestamp: Long): Long {
        var timestamp: Long = 0
        val correction = SystemClock.elapsedRealtimeNanos() - eventTimestamp * MICRO_TO_NANO
        val currentTimestamp = System.currentTimeMillis()
        timestamp = if (correction > 0) {
            currentTimestamp - correction / NANO_TO_MILI
        } else {
            currentTimestamp
        }
        return timestamp
    }

    companion object {
        /**
         * Value used for converting nanoseconds to milliseconds
         */
        private const val NANO_TO_MILI: Long = 1000000

        /**
         * Value used for converting microseconds to nanoseconds
         */
        private const val MICRO_TO_NANO: Long = 1000
    }
}