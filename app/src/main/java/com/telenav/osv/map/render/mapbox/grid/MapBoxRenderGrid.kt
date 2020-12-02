package com.telenav.osv.map.render.mapbox.grid

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.telenav.osv.map.render.mapbox.MapBoxRenderBase
import com.telenav.osv.map.render.mapbox.grid.model.MapBoxGridMapLayer
import com.telenav.osv.map.render.mapbox.grid.model.TaskFeatureCollection
import com.telenav.osv.map.render.mapbox.grid.model.TaskPropertiesGrid
import com.telenav.osv.map.render.mapbox.grid.model.TaskPropertiesLabel
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.utils.CurrencyUtil
import com.telenav.osv.utils.Log
import com.telenav.osv.utils.concatenate
import timber.log.Timber

/**
 * The render related to grids for [MapboxMap]. This entry method for this is the [render] method while the update method [updateGridView] method which requires a list of [Task] which will represent the data from
 * the Jarvis API also an identifier for the current logged in user. This feature will only work with a logged in user.
 *
 * Further it will provide a method to obtain the identifier of the assigned user if any on the current clicked grid represented by a [LatLng] physical map location. This is being exposed via [onMapClick]
 */
class MapBoxRenderGrid(context: Context, mapboxMap: MapboxMap, private val currencyUtil: CurrencyUtil) : MapBoxRenderBase(context, mapboxMap) {

    init {
        TAG = MapBoxRenderGrid::class.java.simpleName
    }

    private val mapBoxGridLayers: MutableList<MapBoxGridMapLayer> = mutableListOf()

    /**
     * Render the grids based on the given list of [Task] data and the current logged in user identifier.
     *
     * This will convert the data into geoJson format internally by using [updateCache] while rendering them with the help of the internal [prepareGrid] method.
     * @param tasks the data from Jarvis API representing the grids
     * @param jarvisUserId the identifier for the current logged in user
     */
    fun updateGridView(tasks: List<Task>, jarvisUserId: Int, includeLabel: Boolean) {
        mapboxMap.getStyle {
            Log.d(TAG, "updateGridView. Grid layers size: ${mapBoxGridLayers.size}")
            updateCache(tasks, jarvisUserId, includeLabel)
            renderGrids(it)
        }
    }

    /**
     * The map click method which searches through all available grids for the given [LatLng] to be contained in and if found to return the user identifier if there is any set
     * @param point the [LatLng] for which the search will be performed
     * @return the user identifier if there is any set or null otherwise
     */
    fun onMapClick(point: LatLng): String? {
        var features: MutableList<Feature> = mutableListOf()
        val mapBoxGridLayersWithoutUnavailable = mapBoxGridLayers.toMutableList()
        mapBoxGridLayersWithoutUnavailable.removeAt(INDEX_UNAVAILABLE)
        for (gridLayer in mapBoxGridLayersWithoutUnavailable) {
            features = concatenate(features, gridLayer.taskFeatureCollection.gridData.features()!!) as MutableList<Feature>
        }
        if (features.isNotEmpty()) {
            return getSelectedGridId(point, features)
        }

        return null
    }

    override fun clear() {
        mapboxMap.getStyle {
            val copyGridMapLayers = mapBoxGridLayers.toMutableList()
            mapBoxGridLayers.clear()
            for (gridLayer in copyGridMapLayers) {
                val gridLayerBackground = gridLayer.getIdentifierLayerBackground()
                if (gridLayerBackground != null) {
                    it.removeLayer(gridLayerBackground)
                }
                it.removeLayer(gridLayer.getIdentifierLayerOutline())
                it.removeSource(gridLayer.getIdentifierGridSource())
                it.removeLayer(gridLayer.getIdentifierLayerIconBackground())
                it.removeSource(gridLayer.getIdentifierLabelSource())
                it.removeImage(gridLayer.getIdentifierIconBackground())
            }
            Log.d(TAG, "clear. Grid layers size: ${mapBoxGridLayers.size}")
        }
    }

    override fun render() {
        mapboxMap.getStyle {
            initCache()
            renderGrids(it)
        }
    }

    private fun renderGrids(style: Style) {
        for (gridLayer in mapBoxGridLayers) {
            prepareGrid(style, gridLayer)
        }
    }

    private fun initCache() {
        this.mapBoxGridLayers.add(INDEX_TO_DO, MapBoxGridMapLayer(MapBoxRenderStatus.TO_DO, TaskFeatureCollection()))
        this.mapBoxGridLayers.add(INDEX_RECORDING, MapBoxGridMapLayer(MapBoxRenderStatus.RECORDING, TaskFeatureCollection()))
        this.mapBoxGridLayers.add(INDEX_MAP_OPS_QC, MapBoxGridMapLayer(MapBoxRenderStatus.REVIEW, TaskFeatureCollection()))
        this.mapBoxGridLayers.add(INDEX_DONE, MapBoxGridMapLayer(MapBoxRenderStatus.DONE, TaskFeatureCollection()))
        this.mapBoxGridLayers.add(INDEX_PAID, MapBoxGridMapLayer(MapBoxRenderStatus.PAID, TaskFeatureCollection()))
        this.mapBoxGridLayers.add(INDEX_UNAVAILABLE, MapBoxGridMapLayer(MapBoxRenderStatus.UNAVAILABLE, TaskFeatureCollection()))
        Log.d(TAG, "initCache. Grid layers size: ${mapBoxGridLayers.size}")
    }

    private fun prepareGrid(style: Style, mapBoxGridMapLayer: MapBoxGridMapLayer) {
        val gridSource = style.getSource(mapBoxGridMapLayer.getIdentifierGridSource())
        if (gridSource == null) {
            setGridBackground(style, mapBoxGridMapLayer.getIdentifierGridSource(), mapBoxGridMapLayer)
        } else {
            val gridGeoJson = gridSource as GeoJsonSource
            gridGeoJson.setGeoJson(mapBoxGridMapLayer.taskFeatureCollection.gridData)
        }
        val labelSource = style.getSource(mapBoxGridMapLayer.getIdentifierLabelSource())
        if (labelSource == null) {
            style.addImage(mapBoxGridMapLayer.getIdentifierIconBackground(), mapBoxGridMapLayer.getIconBackground(context))
            setGridLabel(mapBoxGridMapLayer, style)
        } else {
            val labelGeoJson = labelSource as GeoJsonSource
            labelGeoJson.setGeoJson(mapBoxGridMapLayer.taskFeatureCollection.labelData)
        }
    }

    private fun setGridLabel(mapBoxGridMapLayer: MapBoxGridMapLayer, style: Style) {
        val sourceIdLabel = mapBoxGridMapLayer.getIdentifierLabelSource()
        style.addSource(GeoJsonSource(sourceIdLabel, mapBoxGridMapLayer.taskFeatureCollection.labelData))
        if (mapBoxGridMapLayer.gridRenderStatus.value == MapBoxRenderStatus.UNAVAILABLE.value) {
            style.addLayer(
                    SymbolLayer(mapBoxGridMapLayer.getIdentifierLayerIconBackground(), sourceIdLabel)
                            .withProperties(
                                    PropertyFactory.iconImage(mapBoxGridMapLayer.getIdentifierIconBackground()),
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconIgnorePlacement(true),
                                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_TOP_LEFT),
                                    PropertyFactory.iconTranslate(arrayOf(11f, 10f))
                            )
            )
        } else {
            style.addLayer(
                    SymbolLayer(mapBoxGridMapLayer.getIdentifierLayerIconBackground(), sourceIdLabel)
                            .withProperties(
                                    PropertyFactory.iconImage(mapBoxGridMapLayer.getIdentifierIconBackground()),
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconIgnorePlacement(true),
                                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_TOP_LEFT),
                                    PropertyFactory.iconTextFit(Property.ICON_TEXT_FIT_BOTH),
                                    PropertyFactory.iconTextFitPadding(arrayOf(1f, 12f, 5f, 4f)),
                                    PropertyFactory.iconTranslate(arrayOf(11f, 10f)),
                                    PropertyFactory.textField(Expression.get(JSON_PROPERTY_GRID_TITLE)),
                                    PropertyFactory.textColor(mapBoxGridMapLayer.getColor(context)),
                                    PropertyFactory.textFont(TEXT_IDENTIFIER_NORMAL_FONT_STACK),
                                    PropertyFactory.textSize(11f),
                                    PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP_LEFT),
                                    PropertyFactory.textTranslate(arrayOf(15f, 12f)),
                                    PropertyFactory.textAllowOverlap(true),
                                    PropertyFactory.textIgnorePlacement(true)
                            )
            )
        }
    }

    private fun setGridBackground(style: Style, sourceIdGrid: String, mapBoxGridMapLayer: MapBoxGridMapLayer) {
        style.addSource(GeoJsonSource(sourceIdGrid, mapBoxGridMapLayer.taskFeatureCollection.gridData))
        val gridColor = mapBoxGridMapLayer.getColor(context)
        val identifierLayerBackground = mapBoxGridMapLayer.getIdentifierLayerBackground()
        if (identifierLayerBackground != null) {
            style.addLayer(
                    FillLayer(identifierLayerBackground, sourceIdGrid)
                            .withProperties(
                                    PropertyFactory.fillColor(gridColor),
                                    PropertyFactory.fillOpacity(0.1f)
                            )
            )
        }
        style.addLayer(
                LineLayer(mapBoxGridMapLayer.getIdentifierLayerOutline(), sourceIdGrid)
                        .withProperties(
                                PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                                PropertyFactory.lineJoin(Property.LINE_JOIN_MITER),
                                PropertyFactory.lineWidth(1f),
                                PropertyFactory.fillOutlineColor(gridColor)))
    }

    private fun updateCache(tasks: List<Task>, currentUserId: Int, includeLabel: Boolean) {
        for (gridLayer in mapBoxGridLayers) {
            gridLayer.taskFeatureCollection.gridData.features()?.clear()
            gridLayer.taskFeatureCollection.labelData.features()?.clear()
        }
        updateGridDataAndLabel(tasks, currentUserId, includeLabel)
    }

    private fun updateGridDataAndLabel(tasks: List<Task>, currentUserId: Int, includeLabel: Boolean) {
        for (task in tasks) {
            //if the assigned user id is != current and the status is not TO_DO it means it was assigned to another user therefore we ignore said tasks
            if (isTaskUnavailable(task, currentUserId)) {
                Timber.d("updateCache. Status: skipped task due to being assigned to other user. Task Id: ${task.id}. Status: ${task.status}. AssignedUser: ${task.assignedUserName}")
                val unavailableFeatureCollection = mapBoxGridLayers[INDEX_UNAVAILABLE].taskFeatureCollection
                unavailableFeatureCollection.gridData.features()!!.add(getGridFeatureFromTask(task))
                if (includeLabel) {
                    unavailableFeatureCollection.labelData.features()!!.add(getLabelFeatureFromTask(task))
                }
                continue
            }
            val newGridFeature = getGridFeatureFromTask(task)
            var newLabelFeature: Feature? = null
            if (includeLabel) {
                newLabelFeature = getLabelFeatureFromTask(task)
            }
            when (task.status) {
                GridStatus.TO_DO.status -> {
                    val openTaskFeatureCollection = mapBoxGridLayers[INDEX_TO_DO].taskFeatureCollection
                    openTaskFeatureCollection.gridData.features()!!.add(newGridFeature)
                    newLabelFeature?.let {
                        openTaskFeatureCollection.labelData.features()!!.add(it)
                    }
                }
                GridStatus.RECORDING.status -> {
                    val recordingTaskFeatureCollection = mapBoxGridLayers[INDEX_RECORDING].taskFeatureCollection
                    recordingTaskFeatureCollection.gridData.features()!!.add(newGridFeature)
                    newLabelFeature?.let {
                        recordingTaskFeatureCollection.labelData.features()!!.add(it)
                    }
                }
                GridStatus.MAP_OPS_QC.status -> {
                    val mapOpsQcTaskFeatureCollection = mapBoxGridLayers[INDEX_MAP_OPS_QC].taskFeatureCollection
                    mapOpsQcTaskFeatureCollection.gridData.features()!!.add(newGridFeature)
                    newLabelFeature?.let {
                        mapOpsQcTaskFeatureCollection.labelData.features()!!.add(it)
                    }
                }
                GridStatus.DONE.status -> {
                    val doneTaskFeatureCollection = mapBoxGridLayers[INDEX_DONE].taskFeatureCollection
                    doneTaskFeatureCollection.gridData.features()!!.add(newGridFeature)
                    newLabelFeature?.let {
                        doneTaskFeatureCollection.labelData.features()!!.add(newLabelFeature)
                    }
                }
                GridStatus.PAID.status -> {
                    val paidTaskFeatureCollection = mapBoxGridLayers[INDEX_PAID].taskFeatureCollection
                    paidTaskFeatureCollection.gridData.features()!!.add(newGridFeature)
                    newLabelFeature?.let {
                        paidTaskFeatureCollection.labelData.features()!!.add(newLabelFeature)
                    }
                }
            }
        }
    }

    private fun isTaskUnavailable(task: Task, currentUserId: Int): Boolean {
        return task.assignedUserId != null && task.assignedUserId != currentUserId
    }

    private fun getGridFeatureFromTask(task: Task): Feature {
        return Feature.fromGeometry(
                MultiPolygon
                        .fromLngLats(mutableListOf(mutableListOf(
                                mutableListOf<Point>(
                                        Point.fromLngLat(task.swLng.toDouble(), task.neLat.toDouble()),
                                        Point.fromLngLat(task.neLng.toDouble(), task.neLat.toDouble()),
                                        Point.fromLngLat(task.neLng.toDouble(), task.swLat.toDouble()),
                                        Point.fromLngLat(task.swLng.toDouble(), task.swLat.toDouble()),
                                        Point.fromLngLat(task.swLng.toDouble(), task.neLat.toDouble()))))),
                JsonParser().parse(Gson().toJson(TaskPropertiesGrid(id = task.id))).asJsonObject)
    }

    private fun getLabelFeatureFromTask(task: Task): Feature {
        val currencyWithAmount = currencyUtil.getAmountWithCurrencySymbol(task.currency, task.amount)
        return Feature.fromGeometry(
                Point.fromLngLat(task.swLng.toDouble() + POINT_LABEL_OFFSET, task.neLat.toDouble() - POINT_LABEL_OFFSET),
                JsonParser().parse(Gson().toJson(TaskPropertiesLabel(currencyWithAmount))).asJsonObject)
    }

    private fun getCoordinatesFromGeometry(geometry: Geometry?): MutableList<Point>? {
        if (geometry == null) {
            return null
        }
        Timber.d("Geometry type = %s", geometry.type())
        when (geometry.type()) {
            GEOMETRY_MULTI_POINT -> {
                return (geometry as MultiPolygon).coordinates()[0][0]
            }
        }
        return null
    }

    private fun getSelectedGridId(point: LatLng, features: List<Feature>): String? {
        for (feature in features) {
            val topLeft =
                    getCoordinatesFromGeometry(feature.geometry())?.get(0)?.coordinates() ?: return null
            val bottomRight =
                    getCoordinatesFromGeometry(feature.geometry())?.get(2)?.coordinates() ?: return null
            if ((point.latitude in bottomRight[1]..topLeft[1])
                    && (point.longitude in topLeft[0]..bottomRight[0])
            ) {
                return feature.properties()?.get(JSON_PROPERTY_GRID_ID)!!.asString
            }
        }
        //null since the point was not found
        return null
    }

    companion object {
        lateinit var TAG: String

        private val TEXT_IDENTIFIER_NORMAL_FONT_STACK = arrayOf("Roboto Bold")

        private const val GEOMETRY_MULTI_POINT = "MultiPolygon"

        private const val POINT_LABEL_OFFSET = 0.0001

        //Properties
        const val JSON_PROPERTY_GRID_ID = "Id"
        const val JSON_PROPERTY_GRID_TITLE = "Title"

        private const val INDEX_TO_DO = 0
        private const val INDEX_RECORDING = 1
        private const val INDEX_MAP_OPS_QC = 2
        private const val INDEX_DONE = 3
        private const val INDEX_PAID = 4
        private const val INDEX_UNAVAILABLE = 5
    }
}