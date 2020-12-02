package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData

/**
 * Class which retrieves the fuel tank level input, as a percentage.
 *
 */
open class FuelLevelObject : BaseObject<Double?> {
    constructor(statusCode: Int) : this(Double.MIN_VALUE, statusCode) {}
    constructor(fuelLevel: Double, statusCode: Int) : super(fuelLevel, statusCode, LibraryUtil.FUEL_TANK_LEVEL_INPUT) {
        dataSource = LibraryUtil.OBD_SOURCE
    }

    protected constructor(fuelLevel: Double, statusCode: Int, @AvailableData sensorType: String?) : super(fuelLevel, statusCode, sensorType!!) {}

    /**
     * Returns the current fuel level in percentages %
     * @return Current fuel level
     */
    val fuelLevel: Double
        get() = actualValue!!
}