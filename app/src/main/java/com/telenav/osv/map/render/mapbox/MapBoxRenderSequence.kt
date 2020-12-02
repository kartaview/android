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
import com.telenav.osv.utils.LogUtils

/**
 * The render related to sequence for [MapboxMap]. This entry method for this is the [render] method while for updates the [update] method which requires a list of lists of [LatLng] which will represent a list of sequences in point format.
 */
class MapBoxRenderSequence(
        context: Context,
        mapboxMap: MapboxMap) : MapBoxRenderBase(context, mapboxMap) {

    /**
     * Update the given parameters for the source which was added while implementing the [render] method.
     * @param sequences a list containing a list of points which encapsulated a sequence.
     */
    fun update(sequences: List<List<LatLng>> = mutableListOf()) {
        mapboxMap.getStyle { style ->
            style.getSource(ID_SOURCE_SEQUENCES)?.let {
                val features = mutableListOf<Feature>()
                for (sequence in sequences) {
                    features.add(
                            Feature.fromGeometry(
                                    LineString.fromLngLats(sequence.map { Point.fromLngLat(it.longitude, it.latitude) }))
                    )
                }
                val featureCollection = FeatureCollection.fromFeatures(features)
                LogUtils.logDebug("MapBoxRenderSequence", "feature collection size: ${featureCollection.features()?.size}")
                val geoJsonSource = it as GeoJsonSource
                geoJsonSource.setGeoJson(featureCollection)
            }
        }
    }

    override fun render() {
        mapboxMap.getStyle { style ->
            style.addSource(GeoJsonSource(ID_SOURCE_SEQUENCES, FeatureCollection.fromFeatures(mutableListOf<Feature>())))
            // The layer properties for our line.
            style.addLayer(
                    LineLayer(ID_LAYER_SEQUENCES, ID_SOURCE_SEQUENCES)
                            .withProperties(
                                    PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                    PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                    PropertyFactory.lineWidth(LINE_WIDTH),
                                    PropertyFactory.lineColor(context.resources.getColor(R.color.sequence_local))
                            ))
        }
    }


    override fun clear() {
        mapboxMap.getStyle {
            it.removeLayer(ID_LAYER_SEQUENCES)
            it.removeSource(ID_SOURCE_SEQUENCES)
        }
    }

    companion object {
        private const val ID_SOURCE_SEQUENCES = "kv-sequences-source"
        private const val LINE_WIDTH = 2f
        const val ID_LAYER_SEQUENCES = "kv-sequences-layer"
    }
}