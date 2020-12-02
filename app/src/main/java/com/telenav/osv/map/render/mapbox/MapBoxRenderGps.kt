package com.telenav.osv.map.render.mapbox

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.telenav.osv.utils.Log
import com.telenav.osv.utils.LogUtils

/**
 * The render related to GPS for [MapboxMap]. This entry method for this is the [render] method while for update the [updateCameraMode] which will enable the location component from the sdk.
 */
class MapBoxRenderGps(context: Context, mapboxMap: MapboxMap) : MapBoxRenderBase(context, mapboxMap) {

    private lateinit var locationComponent: LocationComponent

    private val TAG by lazy { MapBoxRenderGps::class.java.simpleName }

    /**
     * Render the current GPS location by enabling the location component.
     */
    @SuppressLint("MissingPermission")
    fun updateCameraMode(cameraMode: Int = CameraMode.NONE) {
        mapboxMap.getStyle {
            Log.d(TAG, "updateCameraMode. Camera mode: $cameraMode.")
            mapboxMap.locationComponent.isLocationComponentEnabled = true
            mapboxMap.locationComponent.cameraMode = cameraMode
        }
    }

    @SuppressLint("MissingPermission")
    override fun clear() {
        mapboxMap.getStyle {
            Log.d(TAG, "clear. Status: Clearing location component.")
            val locationComponentActivationOptions = LocationComponentActivationOptions
                    .builder(context, it)
                    .locationComponentOptions(LocationComponentOptions.builder(context).build())
                    .build()
            mapboxMap.locationComponent.activateLocationComponent(locationComponentActivationOptions)
            mapboxMap.locationComponent.isLocationComponentEnabled = false
            clearLayers(it)
        }
    }

    override fun render() {
        mapboxMap.getStyle {
            render(it)
        }
    }

    @SuppressLint("MissingPermission")
    fun render(style: Style, cameraMode: Int = CameraMode.NONE_GPS) {
        Log.d(TAG, "render. Status: Setting location component. Camera mode: $cameraMode")
        val source = style.getSource(ID_SOURCE_GPS)
        if (source == null) {
            style.addSource(GeoJsonSource(ID_SOURCE_GPS, FeatureCollection.fromFeatures(arrayOf())))
            style.addLayer(LineLayer(ID_LAYER_GPS, ID_SOURCE_GPS))
        }
        // Get an instance of the component
        locationComponent = mapboxMap.locationComponent

        // Activate with a built LocationComponentActivationOptions object
        locationComponent.activateLocationComponent(LocationComponentActivationOptions
                .builder(context, style)
                .locationComponentOptions(LocationComponentOptions.builder(context).layerAbove(ID_LAYER_GPS).build())
                .useDefaultLocationEngine(true)
                .build()
        )


        // set the visibility of the location component to true
        locationComponent.isLocationComponentEnabled = true
        // Set the component's camera mode
        locationComponent.cameraMode = cameraMode

        // Set the component's render mode
        locationComponent.renderMode = RenderMode.NORMAL
    }

    private fun clearLayers(style: Style) {
        style.removeLayer(ID_LAYER_GPS)
        style.removeSource(ID_SOURCE_GPS)
    }

    private companion object {
        private const val ID_SOURCE_GPS = "id-gps-source"
        private const val ID_LAYER_GPS = "id-gps-layer"
    }
}