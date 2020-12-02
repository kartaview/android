package com.telenav.osv.map.render.mapbox.grid.model

import android.content.Context
import android.graphics.Bitmap
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.telenav.osv.map.render.mapbox.grid.MapBoxRenderStatus
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.utils.getMapBackgroundMap
import com.telenav.osv.tasks.utils.getMapLabelBackgroundMap

class MapBoxGridMapLayer(val gridRenderStatus: MapBoxRenderStatus, var taskFeatureCollection: TaskFeatureCollection) {
    private val BASE: String = "grid-cell"

    fun getIdentifierGridSource(): String {
        return "$BASE${SOURCE_GEO_JSON_GRID}${getIdentifierRenderByStatus(gridRenderStatus)}"
    }

    fun getIdentifierLabelSource(): String {
        return "$BASE${SOURCE_GEO_JSON_LABEL}${getIdentifierRenderByStatus(gridRenderStatus)}"
    }

    fun getIdentifierLayerOutline(): String {
        return "$BASE${LAYER_OUTLINE}${getIdentifierRenderByStatus(gridRenderStatus)}"
    }

    fun getIdentifierLayerBackground(): String? {
        return if (gridRenderStatus.value == GridStatus.TO_DO.status) {
            null
        } else {
            "$BASE${LAYER_BACKGROUND}${getIdentifierRenderByStatus(gridRenderStatus)}"
        }
    }

    fun getIdentifierIconBackground(): String {
        return "$BASE${ICON_BACKGROUND}${getIdentifierRenderByStatus(gridRenderStatus)}"
    }

    fun getIdentifierLayerIconBackground(): String {
        return "$BASE${LAYER_ICON_BACKGROUND}${getIdentifierRenderByStatus(gridRenderStatus)}"
    }

    fun getIconBackground(context: Context): Bitmap {
        return BitmapUtils.getBitmapFromDrawable(context.resources.getDrawable(getMapLabelBackgroundMap(gridRenderStatus), null))!!
    }

    fun getColor(context: Context): Int {
        return context.resources.getColor(getMapBackgroundMap(gridRenderStatus))
    }

    private fun getIdentifierRenderByStatus(gridRenderStatus: MapBoxRenderStatus): String {
        return when (gridRenderStatus) {
            MapBoxRenderStatus.TO_DO -> "open"
            MapBoxRenderStatus.REVIEW -> "in-review"
            MapBoxRenderStatus.DONE -> "approved"
            MapBoxRenderStatus.RECORDING -> "in-progress"
            MapBoxRenderStatus.PAID -> "paid"
            MapBoxRenderStatus.UNAVAILABLE -> "unavailable"
        }
    }

    private companion object {
        //Sources
        private const val SOURCE_GEO_JSON_GRID = "-source-geojson-grid-"
        private const val SOURCE_GEO_JSON_LABEL = "-source-geojson-label-"

        //Layers - outline
        private const val LAYER_OUTLINE = "-outline-layer-"

        //Layers - background
        private const val LAYER_BACKGROUND = "-background-layer-"

        //Layer - icon-background
        private const val ICON_BACKGROUND = "-icon-background-"

        //Layer - icon-background layer
        private const val LAYER_ICON_BACKGROUND = "-icon-background-layer-"
    }
}