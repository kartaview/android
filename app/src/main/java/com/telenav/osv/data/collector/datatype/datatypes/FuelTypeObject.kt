package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the fuel type.
 */
class FuelTypeObject(fuelType: String?, statusCode: Int) : BaseObject<String?>(fuelType, statusCode, LibraryUtil.FUEL_TYPE) {
    constructor(statusCode: Int) : this("", statusCode)

    /**
     * Returns fuel type used for the car
     * @return Fuel type
     */
    val fuelType: String
        get() = actualValue!!

    init {
        dataSource = LibraryUtil.OBD_SOURCE
    }
}