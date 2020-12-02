package com.telenav.osv.map.render

import android.content.Context
import android.location.Location
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.telenav.osv.map.render.mapbox.*
import com.telenav.osv.map.render.mapbox.MapBoxRenderBase.Companion.ZOOM_DEFAULT_LEVEL
import com.telenav.osv.map.render.mapbox.grid.MapBoxRenderGrid
import com.telenav.osv.map.render.template.MapRenderTemplateHelper
import com.telenav.osv.map.render.template.MapRenderTemplateIdentifier
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.utils.CurrencyUtil
import com.telenav.osv.utils.Log

/**
 * Render class handling all map related modification. This includes:
 * * coverages - internally using [MapBoxRenderCoverage]
 * * grids - internally using [MapBoxRenderGrid]
 * * gps - internally using [MapBoxRenderGps]
 * * sequences - internally using [MapBoxRenderSequence]
 * * symbol - internally using [MapBoxRenderSymbol]
 * * gps trail - internally using [MapBoxRenderGpsTrail]
 *
 * There are present multiple render modes by using the following public methods:
 * * [updateDefault]
 * * [updatePreview]
 * * [updateRecording]
 * * [clearMap]
 * * [clearGpsTrail]
 * * [updateGrid]
 *
 * Helper methods:
 * * [mapGridClick]
 * * [centerOnCurrentLocation]
 */
class MapRender(context: Context,
                private val mapboxMap: MapboxMap,
                factoryServerEndpointUrl: FactoryServerEndpointUrl,
                currencyUtil: CurrencyUtil) {

    private val TAG by lazy { MapRender::class.java.simpleName }
    private val renderCoverage: MapBoxRenderCoverage = MapBoxRenderCoverage(context, mapboxMap, factoryServerEndpointUrl)
    private val renderGrid: MapBoxRenderGrid = MapBoxRenderGrid(context, mapboxMap, currencyUtil)
    private val renderSequence: MapBoxRenderSequence = MapBoxRenderSequence(context, mapboxMap)
    private val renderGps: MapBoxRenderGps = MapBoxRenderGps(context, mapboxMap)
    private val renderGpsTrail: MapBoxRenderGpsTrail = MapBoxRenderGpsTrail(context, mapboxMap)
    private val renderSymbol: MapBoxRenderSymbol = MapBoxRenderSymbol(context, mapboxMap)
    private val mapTemplateHelper: MapRenderTemplateHelper = MapRenderTemplateHelper(renderGrid, renderGps, renderSymbol, renderGpsTrail, renderSequence, renderCoverage)
    private var lastCenterLocation: LatLng? = null

    /**
     * Renders the current template for the map denoted by the exposed [MapRenderTemplateIdentifier]
     * @param mapTemplateIdentifier the identifier for the template according to the map will be render by
     */
    fun render(mapTemplateIdentifier: MapRenderTemplateIdentifier) {
        mapTemplateHelper.applyIfRequired(mapTemplateIdentifier)
    }

    /**
     * Refresh the coverage for the edge case when the start date will come only with the grid elements.
     * @param startDate the started date for the template according to the map will be render by
     */
    fun refreshCoverage(startDate: Int) {
        renderCoverage.render(startDate)
    }

    /**
     * Default rendering which will render the coverages, sequences and center on the current GPS location.
     */
    fun updateDefault(sequences: List<List<LatLng>> = mutableListOf(), lastKnowLocation: LatLng? = null) {
        renderSequence.update(sequences)
        centerOnCurrentLocation(lastKnowLocation)
    }

    /**
     * Rendering used in the case of the recording.
     */
    fun updateRecording(lastKnowLocation: LatLng? = null, localSequences: List<List<LatLng>> = mutableListOf()) {
        renderSequence.update(localSequences)
        centerOnCurrentLocation(lastKnowLocation, CameraMode.TRACKING)
    }

    /**
     * Rendering used in the case of the recording for the specific case of updating gps trail
     * @param gpsTrail the locations representing an update for the local sequence in locations.
     */
    fun updateRecording(gpsTrail: List<Location>) {
        if (gpsTrail.isEmpty()) {
            renderGpsTrail.updateTrail(arrayListOf())
        } else {
            renderGpsTrail.updateTrail(gpsTrail.map { LatLng(it) })
        }
    }

    /**
     * The grids rendering by using the given parameter and the user identifier.
     */
    fun updateGrid(tasks: List<Task>, jarvisUserId: Int, includeLabels: Boolean, localSequences: List<List<LatLng>> = mutableListOf()) {
        Log.d(TAG, "updateGrid. LocalSequences size: ${localSequences.size}. Task size: ${tasks.size}. Include labels: $includeLabels. Jarvis user id: $jarvisUserId.")
        renderGrid.updateGridView(tasks, jarvisUserId, includeLabels)
        renderSequence.update(localSequences)
    }

    /**
     * The preview rendering which render the local sequence including the symbol. This will center internally around a bounding box.
     */
    fun updatePreview(localSequence: List<LatLng>, symbolLocation: LatLng?) {
        renderSequence.update(listOf(localSequence))
        if (symbolLocation != null) {
            renderSymbol.update(symbolLocation)
        }
        centerOnBoundBox(localSequence)
    }

    fun clearGpsTrail() {
        renderGpsTrail.updateTrail(arrayListOf())
    }

    /**
     * Clears all internal resources.
     */
    fun clearMap() {
        renderGps.clear()
        renderCoverage.clear()
        renderSequence.clear()
        renderSymbol.clear()
        renderGrid.clear()
        renderGpsTrail.clear()
    }

    /**
     * Centers on the last known location or the given parameter.
     */
    fun centerOnCurrentLocation(lastKnowLocation: LatLng? = null, cameraMode: Int = CameraMode.TRACKING_GPS_NORTH, zoomLeveL: Double = ZOOM_DEFAULT_LEVEL) {
        lastCenterLocation = lastKnowLocation
                ?: if (lastCenterLocation != null) {
                    lastCenterLocation
                } else if (mapboxMap.cameraPosition.target.latitude != 0.0 && mapboxMap.cameraPosition.target.longitude != 0.0) {
                    mapboxMap.cameraPosition.target
                } else {
                    CameraPosition.DEFAULT.target
                }
        val cameraPosition = CameraPosition.Builder()
                .target(lastCenterLocation)
                .zoom(zoomLeveL)
                .build()
        mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        // Set a default center on the current position of the camera
        renderGps.updateCameraMode(cameraMode)
    }

    /**
     * Will check if any grid was clicked.
     * @return the identifier for the grid for which the given parameter is inside if there is any.
     */
    fun mapGridClick(point: LatLng): String? {
        return renderGrid.onMapClick(point)
    }

    private fun centerOnBoundBox(latLngs: List<LatLng>) {
        val latLngBounds = LatLngBounds.Builder()
                .includes(latLngs)
                .build()
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 80))
    }
}