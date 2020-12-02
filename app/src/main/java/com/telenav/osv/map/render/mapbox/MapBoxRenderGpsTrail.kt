package com.telenav.osv.map.render.mapbox

import android.content.Context
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.telenav.osv.R

/**
 * The render related to gps trail which will draw on the map the trail representing a recording session for [MapboxMap]. This entry method for this is the [render] method while
 * for updates the [updateTrail] method which requires a list of [LatLng] which will represent the trail.
 * @param context required for updating ui related resources on line properties
 */
class MapBoxRenderGpsTrail(context: Context, mapBoxMap: MapboxMap) : MapBoxRenderBase(context, mapBoxMap) {

    /**
     * Update the gps trail based on given param.
     * @param gpsTrail the collection of [LatLng] representing the current gps trail.
     */
    fun updateTrail(gpsTrail: List<LatLng>) {
        mapboxMap.getStyle {
            it.getSource(SOURCE_ID_GPS_TRAIL)?.let { source ->
                val featureCollection = FeatureCollection.fromFeature(
                        Feature.fromGeometry(
                                LineString.fromLngLats(gpsTrail.map { gpsTrailLatLng -> Point.fromLngLat(gpsTrailLatLng.longitude, gpsTrailLatLng.latitude) })))
                val sourceGeoJson = source as GeoJsonSource
                sourceGeoJson.setGeoJson(featureCollection)
            }
        }
    }

    /**
     * Clean the gps trail by removing both the layer and the source.
     */
    override fun clear() {
        mapboxMap.getStyle {
            it.removeLayer(LAYER_ID_GPS_TRAIL)
            it.removeSource(SOURCE_ID_GPS_TRAIL)
        }
    }

    override fun render() {
        mapboxMap.getStyle {
            it.addSource(GeoJsonSource(SOURCE_ID_GPS_TRAIL, FeatureCollection.fromFeatures(arrayListOf())))
            // The layer properties for our line.
            it.addLayer(
                    LineLayer(LAYER_ID_GPS_TRAIL, SOURCE_ID_GPS_TRAIL)
                            .withProperties(
                                    PropertyFactory.lineDasharray(
                                            arrayOf(
                                                    DASHED_LINER_SIZE_DISTANCE,
                                                    DASHED_LINE_TRAIL_SIZE
                                            )
                                    ),
                                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                                    PropertyFactory.lineWidth(LINE_WIDTH),
                                    PropertyFactory.lineColor(context.resources.getColor(R.color.default_purple))
                            )
            )
        }
    }

    private companion object {
        private const val SOURCE_ID_GPS_TRAIL = "geojson-gps-trail"
        private const val LAYER_ID_GPS_TRAIL = "layer-gps-trail"
        private const val DASHED_LINER_SIZE_DISTANCE = 0.01f
        private const val DASHED_LINE_TRAIL_SIZE = 2f
        private const val LINE_WIDTH = 5f
    }
}