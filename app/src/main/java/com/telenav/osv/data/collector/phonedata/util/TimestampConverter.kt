package com.telenav.osv.data.collector.phonedata.util

/**
 * Created by raduh on 7/17/17.
 */
abstract class TimestampConverter {
    abstract fun getTimestamp(eventTimestamp: Long): Long
}