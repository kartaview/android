package com.telenav.osv.tasks.model

private const val ERROR_OUT_OF_PICKUP_LIMIT = "OUT_OF_PICKUP_LIMIT"
private const val ERROR_UNKNOWN = "UNKNOWN"

/**
 * This enum stores error codes returned from server for task api
 */
enum class TaskError(val error: String) {
    OUT_OF_PICKUP_LIMIT(ERROR_OUT_OF_PICKUP_LIMIT),
    UNKNOWN(ERROR_UNKNOWN);

    companion object {
        fun getByError(error: String): TaskError {
            val taskError = values().firstOrNull { it.error == error }
            return taskError ?: UNKNOWN
        }
    }
}