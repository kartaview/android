package com.telenav.osv.map.render.mapbox

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.TileSet
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl
import com.telenav.osv.utils.Log
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class MapBoxRenderCoverage(context: Context,
                           mapboxMap: MapboxMap,
                           private val factoryServerEndpointUrl: FactoryServerEndpointUrl) : MapBoxRenderBase(context, mapboxMap) {

    init {
        TAG = MapBoxRenderCoverage::class.java.simpleName
    }

    private var currentDate: String? = null

    private val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

    @SuppressLint("ResourceAsColor")
    fun render(startDate: Int = 0) {
        mapboxMap.getStyle {
            val source = it.getSource(ID_SOURCE_COVERAGE)
            if (source == null) {
                loadCurrentDateIfSet(startDate)
                Log.d(TAG, "render. Status: render coverage. Current date: $currentDate")
                render()
            } else if (startDate != 0 && currentDate == null) {
                loadCurrentDate(startDate)
                Log.d(TAG, "render. Status: refresh coverage. Current date: $currentDate")
                clearCoverage(it)
                renderCoverage(it)
            }
        }
    }

    override fun clear() {
        mapboxMap.getStyle {
            clearCoverage(it)
            it.removeLayer(ID_LAYER_COVERAGE_ABOVE)
            it.removeSource(ID_SOURCE_COVERAGE_ABOVE)
        }
        currentDate = null
    }

    override fun render() {
        mapboxMap.getStyle {
            it.addSource(GeoJsonSource(ID_SOURCE_COVERAGE_ABOVE))
            it.addLayer(LineLayer(ID_LAYER_COVERAGE_ABOVE, ID_SOURCE_COVERAGE_ABOVE))
            renderCoverage(it)
        }
    }

    private fun renderCoverage(style: Style) {
        style.addSource(
                RasterSource(
                        ID_SOURCE_COVERAGE,
                        TileSet(TILE_SET_NAME, factoryServerEndpointUrl.getCoverageEndpoint(currentDate)),
                        TILE_SET_SIZE
                )
        )
        style.addLayerBelow(RasterLayer(ID_LAYER_COVERAGE, ID_SOURCE_COVERAGE), ID_LAYER_COVERAGE_ABOVE)
    }

    private fun loadCurrentDateIfSet(startDate: Int) {
        if (startDate != 0 && currentDate == null) {
            loadCurrentDate(startDate)
        }
    }

    private fun loadCurrentDate(startDate: Int) {
        currentDate = DateTime(startDate.toLong() * CONVERSION_TO_MS).toString(dateTimeFormat)
    }

    private fun clearCoverage(style: Style) {
        style.removeLayer(ID_LAYER_COVERAGE)
        style.removeSource(ID_SOURCE_COVERAGE)
    }


    companion object {
        private lateinit var TAG: String

        private const val TILE_SET_NAME = "kv_tileset_coverage"

        private const val ID_SOURCE_COVERAGE = "kv_source_coverage"
        private const val ID_SOURCE_COVERAGE_ABOVE = "kv_source_coverage_above"

        private const val TILE_SET_SIZE = 256

        private const val CONVERSION_TO_MS = 1000

        const val ID_LAYER_COVERAGE = "kv_layer_coverage"
        const val ID_LAYER_COVERAGE_ABOVE = "kv_layer_coverage_above"
    }
}