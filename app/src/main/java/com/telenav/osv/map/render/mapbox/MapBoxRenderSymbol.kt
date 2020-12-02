package com.telenav.osv.map.render.mapbox

import android.content.Context
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.telenav.osv.R

/**
 * The render related to symbol for [MapboxMap]. This entry method for this is the [render] method while for updates the [update] which requires [LatLng] representing the symbol to be rendered on the map.
 *
 * The symbol with be represented by a [SymbolLayer] with a specific pre-defined icon which currently it will only support.
 */
class MapBoxRenderSymbol(context: Context, mapboxMap: MapboxMap) : MapBoxRenderBase(context, mapboxMap) {

    /**
     * Render the given parameters as a symbol by using a [SymbolLayer] class.
     * @param location a point which encapsulated a symbol as a physical location on the map.
     */
    fun update(location: LatLng) {
        mapboxMap.getStyle { style ->
            style.getSource(ID_SOURCE_SYMBOL)?.let {
                val featureCollection = FeatureCollection.fromFeature(
                        Feature.fromGeometry(
                                Point.fromLngLat(location.longitude, location.latitude)
                        )
                )
                val geoJsonSource = it as GeoJsonSource
                geoJsonSource.setGeoJson(featureCollection)
            }
        }
    }

    override fun clear() {
        mapboxMap.getStyle {
            it.removeLayer(ID_LAYER_SYMBOL)
            it.removeSource(ID_SOURCE_SYMBOL)
        }
    }

    override fun render() {
        mapboxMap.getStyle {
            it.addImage(ID_ICON, context.resources.getDrawable(R.drawable.ic_ccp))
            it.addSource(GeoJsonSource(ID_SOURCE_SYMBOL, FeatureCollection.fromFeatures(mutableListOf())))
            it.addLayer(
                    SymbolLayer(ID_LAYER_SYMBOL, ID_SOURCE_SYMBOL)
                            .withProperties(PropertyFactory.iconImage(ID_ICON),
                                    iconAllowOverlap(true),
                                    iconOffset(ICON_OFFSET)))
        }
    }

    private companion object {
        private const val ID_SOURCE_SYMBOL = "kv-source-symbol"
        private const val ID_LAYER_SYMBOL = "kv-layer-symbol"
        private const val ID_ICON = "kv-icon-symbol"
        private val ICON_OFFSET = arrayOf(0f, -4.5f)
    }
}