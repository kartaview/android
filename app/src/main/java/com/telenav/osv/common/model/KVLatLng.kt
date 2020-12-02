package com.telenav.osv.common.model

import android.location.Location

data class KVLatLng(var lat: Double = 0.0, var lon: Double = 0.0, var index: Int = 0) {
    constructor(location: Location, index: Int = 0) : this(location.latitude, location.longitude, index)
}