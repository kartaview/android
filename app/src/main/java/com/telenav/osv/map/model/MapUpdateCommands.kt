package com.telenav.osv.map.model

import com.mapbox.mapboxsdk.geometry.LatLng
import com.telenav.osv.tasks.model.Task

/**
 * Base class for MapRender command, from this all concrete implementation emerge
 */
open class MapUpdateBase

open class MapUpdateBaseSequences(open var sequences: List<List<LatLng>>) : MapUpdateBase()

class MapUpdateRecording(sequences: List<List<LatLng>> = mutableListOf()) : MapUpdateBaseSequences(sequences)

class MapUpdateDefault(sequences: List<List<LatLng>> = mutableListOf()) : MapUpdateBaseSequences(sequences)

data class MapUpdateGrid(val tasks: List<Task> = arrayListOf(), val jarvisUserId: Int = 0, val includeLabels: Boolean = false, override var sequences: List<List<LatLng>> = mutableListOf()) : MapUpdateBaseSequences(sequences)

data class MapUpdatePreview(val localSequence: List<LatLng>, val symbolLocation: LatLng? = null) : MapUpdateBase()
