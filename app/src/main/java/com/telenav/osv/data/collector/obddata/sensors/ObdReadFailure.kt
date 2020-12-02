package com.telenav.osv.data.collector.obddata.sensors

/**
 * Class that keeps track of obd reading failures and implements a retry logic
 */
class ObdReadFailure(private val totalNumberOfAllowedFailures: Int) {
    private var numberOfFailures = 0
    fun resetFailures() {
        numberOfFailures = 0
    }

    fun incrementFailures() {
        numberOfFailures++
    }

    fun continueTrying(): Boolean {
        return numberOfFailures < totalNumberOfAllowedFailures
    }
}