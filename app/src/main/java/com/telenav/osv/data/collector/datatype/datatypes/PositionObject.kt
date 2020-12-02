package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil.AvailableData

/**
 * Class which retrieves the geographical position, in latitude and longitude
 */
open class PositionObject : BaseObject<PositionObject?> {
    /**
     * Latitude and longitude in degrees
     */
    private var lon = 0.0
    private var lat = 0.0
    private var isLatitudeAvailable = false
    private var isLongitudeAvailable = false
    var isLocationAvailable = false
        get() {
            if (isLatitudeAvailable && isLongitudeAvailable) {
                field = true
            }
            return field
        }
        private set

    constructor(lat: Double, lon: Double) {
        this.lon = lon
        this.lat = lat
    }

    protected constructor(statusCode: Int, @AvailableData sensorType: String?) : super(null, statusCode, sensorType!!) {}


    fun getLat(): Double {
        return lat
    }

    fun setLat(lat: Double) {
        this.lat = lat
        isLatitudeAvailable = true
    }

    fun getLon(): Double {
        return lon
    }

    fun setLon(lon: Double) {
        this.lon = lon
        isLongitudeAvailable = true
    }

}