package com.telenav.osv.recorder.gpsTrail

import android.location.Location

/**
 * Listener interface which gives all the capabilities regarding gps trail.
 *
 * Available functionality:
 * * [onGpsTrailChanged]
 */
interface ListenerRecordingGpsTrail {

    /**
     * Gps trail callback to signal a change in the gps trail
     * @param gpsTrail optional parameter to give either an empty list or the collection of [Location] representing the gps trail.
     */
    fun onGpsTrailChanged(gpsTrail: List<Location> = arrayListOf())
}