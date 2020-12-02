package com.telenav.osv.data.collector.phonedata.collector

import android.os.Handler
import com.telenav.osv.data.collector.datatype.datatypes.BaseObject
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import com.telenav.osv.data.collector.phonedata.util.MicrosecondsConverter
import com.telenav.osv.data.collector.phonedata.util.MillisecondsConverter
import com.telenav.osv.data.collector.phonedata.util.NanosecondsConverter
import com.telenav.osv.data.collector.phonedata.util.TimestampConverter

open class PhoneCollector(var phoneDataListener: PhoneDataListener?, var notifyHandler: Handler?) {

    /**
     * The time of the first sensor event
     */
    private var firstSensorReadingTime: Long = 0

    /**
     * The time of the system when first value of a sensor is read
     */
    private var firstSystemReadingTime: Long = 0

    /**
     * Flag used in order to determine if the sensor was registered and values are colected
     */
    private var isCollectionStarted = false

    /**
     * Flag used in order to determine if unit time was calculated
     */
    private var isUnitTimeDetermined = false
    private var timestampConverter: TimestampConverter? = null
    private var desiredDelay = Int.MIN_VALUE
    private var previousEventTimestamp: Long = 0

    /**
     * Notify the client when a sensor is not available
     */
    fun sendSensorUnavailabilityStatus(baseObject: BaseObject<*>) {
        phoneDataListener?.let {
            it.onSensorChanged(baseObject)
        }
    }

    @Synchronized
    fun onNewSensorEvent(baseObject: BaseObject<*>) {
        phoneDataListener?.let {
            if (passesFrequencyFilter(baseObject)) {
                previousEventTimestamp = baseObject.timestamp
                it.onSensorChanged(baseObject)
            }
        }
    }

    /**
     * This method determines the timestamp converter for each sensor event based on two readings.
     * The second reading is delayed with one second.
     * @param timeForFirstReading Timestamp of the first sensor value
     * @param timeAfterOneSecond Timestamp of the the sensor value read after one second
     * @return The timestamp converter (MILISECONDS, MICROSECONDS, NANOSECONDS)
     */
    @Synchronized
    private fun getTimestampConverter(timeForFirstReading: Long, timeAfterOneSecond: Long): TimestampConverter {
        val difference = timeAfterOneSecond - timeForFirstReading
        return if (difference / NANO_DIVIDER > 1) { // verifies for nano (using as referece mili)
            NanosecondsConverter(isCurrentTimestampInNano(timeAfterOneSecond))
        } else if (difference / MICRO_DIVIDER > 1) { // verifies for micro (using as referece mili)
            MicrosecondsConverter()
        } else {
            MillisecondsConverter()
        }
    }

    /**
     * This method is called from collectors. It reads the first value of a sensor and then waits one second
     * before reading the second value. This delay is used in order to determine the time unit of a
     * sensor event. After the time unit is determined this method sets the timestamp of a sensor event
     * @param eventTimestamp The timestamp of a sensor event
     * @param baseObject Sensor data
     */
    @Synchronized
    fun determineTimestamp(eventTimestamp: Long, baseObject: BaseObject<*>) {
        if (isCollectionStarted) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - firstSystemReadingTime >= ONE_SECOND_IN_MILLI && !isUnitTimeDetermined) {
                isUnitTimeDetermined = true
                timestampConverter = getTimestampConverter(firstSensorReadingTime, eventTimestamp)
            }
            timestampConverter?.let {
                if (isUnitTimeDetermined) {
                    val timestamp: Long = it.getTimestamp(eventTimestamp)
                    if (timestamp > 0) {
                        baseObject.timestamp = timestamp
                    }
                    onNewSensorEvent(baseObject)
                }
            }

        } else {
            firstSensorReadingTime = eventTimestamp
            firstSystemReadingTime = System.currentTimeMillis()
            isCollectionStarted = true
        }
    }

    /**
     * This method checks if event (sensor) timestamp is the current timestamp in nanoseconds
     * @param eventTimestampInNano Event timestamp
     * @return True if the event timestamp is current timestamp in nanoseconds and false if not
     */
    private fun isCurrentTimestampInNano(eventTimestampInNano: Long): Boolean {
        return eventTimestampInNano / NANO_TO_MILLI > Y2015
    }

    fun setUpFrequencyFilter(delayMicroseconds: Int) {
        desiredDelay = delayMicroseconds / 1000
    }

    private fun passesFrequencyFilter(baseObject: BaseObject<*>): Boolean {
        return if (desiredDelay <= 0) {
            true
        } else baseObject.timestamp - previousEventTimestamp > desiredDelay
    }

    companion object {
        /**
         * The timestamp for end of year 2015. It is used in order to determine if the sensor event timestamp
         * represents the current time in nanoseconds
         */
        private const val Y2015 = 1450000000000L

        /**
         * Value used for converting nanoseconds to milliseconds
         */
        private const val NANO_TO_MILLI: Long = 1000000

        /**
         * Represents the value of a second in milliseconds
         */
        private const val ONE_SECOND_IN_MILLI = 1000

        /**
         * Value used for determining if the sensor event timestamp is in nanoseconds.
         * NOTE: it is used 100_000_000 instead 1_000_000_000 which represents the correct value for transforming
         * nanoseconds to seconds because sometimes the difference between the two readings is like: 999_999_983.
         */
        private const val NANO_DIVIDER: Long = 100000000

        /**
         * Value used for determining if the sensor event timestamp is in microseconds.
         * NOTE: it is used 100_000 instead 1_000_000 which represents the correct value for transforming
         * microseconds to seconds because sometimes the difference between the two readings is like: 999_999.
         */
        private const val MICRO_DIVIDER: Long = 100000
    }

}