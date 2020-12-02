package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the OBD engine torque value
 */
class EngineTorqueObject(engineTorque: Int, statusCode: Int) : BaseObject<Int?>(engineTorque, statusCode, LibraryUtil.ENGINE_TORQUE) {
    constructor(statusCode: Int) : this(Int.MIN_VALUE, statusCode) {}

    /**
     * Returns engine torque in newton metre (Nm)
     * @return Engine torque
     */
    val engineTorque: Int
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.OBD_SOURCE
    }
}