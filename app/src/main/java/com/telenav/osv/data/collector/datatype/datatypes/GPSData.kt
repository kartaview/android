package com.telenav.osv.data.collector.datatype.datatypes

import com.telenav.osv.data.collector.datatype.util.LibraryUtil

/**
 * Class which encapsulates all GPS related data in one object: [PositionObject], [AltitudeObject],
 * [SpeedObject], [AccuracyObject] and [BearingObject]
 */
class GPSData(statusCode: Int) : BaseObject<Void?>(null, statusCode, LibraryUtil.GPS_DATA) {
    var positionObject: PositionObject? = null
    var altitudeObject: AltitudeObject? = null
    var speedObject: SpeedObject? = null
    var accuracyObject: AccuracyObject? = null
    var bearingObject: BearingObject? = null

    init {
        dataSource = LibraryUtil.PHONE_SOURCE
    }
}