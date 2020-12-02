package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the fuel consumption rate through OBD
 * The values range from 0 to 3276.75. Measurement unit: L/h
 */
class FuelConsumptionRateObject(fuelConsumption: Double, statusCode: Int) : BaseObject<Double?>(fuelConsumption, statusCode, LibraryUtil.FUEL_CONSUMPTION_RATE) {
    constructor(statusCode: Int) : this(Double.MIN_VALUE, statusCode)

    val fuelConsumptionRate: Double
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.OBD_SOURCE
    }
}