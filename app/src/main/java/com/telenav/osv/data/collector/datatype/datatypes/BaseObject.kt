package com.telenav.osv.data.collector.datatype.datatypes

import androidx.annotation.NonNull
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData

/**
 * This class represents the super class of all data types inside the DataCollector library
 * @param <T>
</T> */
open class BaseObject<T> {
    var timestamp = System.currentTimeMillis()
    protected var actualValue: T? = null
    open var statusCode = -1
    var dataSource = ""
        protected set
    private var sensorType = ""

    constructor() {}
    constructor(value: T, statusCode: Int) {
        actualValue = value
        this.statusCode = statusCode
    }

    protected constructor(value: T, statusCode: Int, @NonNull @AvailableData sensorType: String) : this(value, statusCode) {
        this.sensorType = sensorType
    }

    open fun getSensorType(): String? {
        return sensorType
    }

    val errorCodeDescription: String
        get() {
            var status = " "
            status += when (statusCode) {
                LibraryUtil.OBD_NOT_AVAILABLE, LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE -> LibraryUtil.SENSOR_NOT_AVAILABLE_DESCRIPTION
                LibraryUtil.OBD_READ_SUCCESS, LibraryUtil.PHONE_SENSOR_READ_SUCCESS -> LibraryUtil.SENSOR_READ_SUCCESS_DESCRIPTION
                LibraryUtil.OBD_INITIALIZATION_FAILURE -> LibraryUtil.SENSOR_INITIALIZATION_FAILURE_DESCRIPTION
                LibraryUtil.OBD_AVAILABLE -> LibraryUtil.SENSOR_AVAILABLE_DESCRIPTION
                LibraryUtil.OBD_READ_FAILURE -> LibraryUtil.SENSOR_READ_FAILURE_DESCRIPTION
                else -> LibraryUtil.SENSOR_READ_FAILURE_DESCRIPTION
            }
            return status
        }
}