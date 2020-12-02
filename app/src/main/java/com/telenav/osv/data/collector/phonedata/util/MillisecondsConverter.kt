package com.telenav.osv.data.collector.phonedata.util

import android.os.SystemClock

class MillisecondsConverter : TimestampConverter() {
    /**
     * This method returns the UTC time of the sensor event reading.
     * Use case: event timestamp represents the time passed from device reboot in milliseconds
     *
     * @param eventTimestamp The time of a sensor event
     * @return The UTC time of the sensor event reading
     */
    override fun getTimestamp(eventTimestamp: Long): Long {
        var timestamp: Long = 0
        val currentTimeMillis = System.currentTimeMillis()
        val correction = SystemClock.elapsedRealtime() - eventTimestamp
        timestamp = if (correction > 0) {
            currentTimeMillis - correction
        } else {
            currentTimeMillis
        }
        return timestamp
    }
}