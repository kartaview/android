package com.telenav.osv.map.render.mapbox

import android.content.Context
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

/**
 * Base class for rendering map box map, this will impose the use of [context] and [mapboxMap] which will be required for every concrete class which implements the base.
 *
 * This will impose the implementation for a [clear] method which will clean up the resources of the concrete implementation.
 */
abstract class MapBoxRenderBase(val context: Context, val mapboxMap: MapboxMap) {

    /**
     * Method for the clean-up for resources for the current implementation which will be on callback basic for style.
     */
    abstract fun clear()

    /**
     * Method for render with empty data used for order purposes.
     */
    abstract fun render()

    companion object {
        const val ZOOM_DEFAULT_LEVEL = 14.9
    }
}