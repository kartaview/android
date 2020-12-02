package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which retrieves the following values from the NMEA service:
 * HDOP, VDOP, PDOP, satellites in view, satellites in use, geo ID height, age of DGPS data, DGPS ID
 */
class NmeaObject : BaseObject<Void?>(null, LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE, LibraryUtil.NMEA_DATA) {
    /**
     * the different mes
     */
    var sentenceType: String? = null
    var hdopValue = 0.0
        set(hdopValue) {
            field = hdopValue
            containsInfo = true
        }
    var vdopValue = 0.0
        set(vdopValue) {
            field = vdopValue
            containsInfo = true
        }
    var pdopValue = 0.0
        set(pdopValue) {
            field = pdopValue
            containsInfo = true
        }
    var numberOfSatellitesInView = 0
        set(numberOfSatellitesInView) {
            field = numberOfSatellitesInView
            containsInfo = true
        }
    var numberOfSatellitesInUse = 0
        set(numberOfSatellitesInUse) {
            field = numberOfSatellitesInUse
            containsInfo = true
        }
    var geoIdHeight: Double? = null
        set(geoIdHeight) {
            field = geoIdHeight
            containsInfo = true
        }
    var ageOfDgpsData: Double? = null
        set(ageOfDgpsData) {
            field = ageOfDgpsData
            containsInfo = true
        }
    var dgpsId: Double? = null
        set(dgpsId) {
            field = dgpsId
            containsInfo = true
        }

    /**
     * if no field is set inside this object, that this field's salue is false
     */
    private var containsInfo = false
    override var statusCode: Int
        get() = if (containsInfo) {
            LibraryUtil.PHONE_SENSOR_READ_SUCCESS
        } else {
            super.statusCode
        }
        set(statusCode) {
            super.statusCode = statusCode
        }

    fun containsInfo(): Boolean {
        return containsInfo
    }

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}