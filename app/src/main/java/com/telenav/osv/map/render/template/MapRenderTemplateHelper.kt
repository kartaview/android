package com.telenav.osv.map.render.template

import com.telenav.osv.map.render.mapbox.MapBoxRenderBase
import com.telenav.osv.map.render.mapbox.MapBoxRenderCoverage
import com.telenav.osv.utils.Log

/**
 * Class which represents the helper methods to
 */
class MapRenderTemplateHelper(mapRenderGrid: MapBoxRenderBase,
                              mapRenderGps: MapBoxRenderBase,
                              mapRenderSymbol: MapBoxRenderBase,
                              mapRenderGpsTrail: MapBoxRenderBase,
                              mapRenderSequence: MapBoxRenderBase,
                              mapRenderCoverage: MapBoxRenderBase) {

    init {
        TAG = MapRenderTemplateHelper::class.java.simpleName
    }

    private val defaultTemplate: MapRenderTemplateItem = MapRenderTemplateItem(
            MapRenderTemplateIdentifier.DEFAULT,
            listOf(MapRenderTemplateItemOperation(mapRenderCoverage, true),
                    MapRenderTemplateItemOperation(mapRenderSequence, true),
                    MapRenderTemplateItemOperation(mapRenderGps, true),
                    MapRenderTemplateItemOperation(mapRenderGrid, false),
                    MapRenderTemplateItemOperation(mapRenderSymbol, false),
                    MapRenderTemplateItemOperation(mapRenderGpsTrail, false)))

    private val gridTemplate: MapRenderTemplateItem = MapRenderTemplateItem(
            MapRenderTemplateIdentifier.GRID,
            listOf(MapRenderTemplateItemOperation(mapRenderCoverage, true),
                    MapRenderTemplateItemOperation(mapRenderSequence, true),
                    MapRenderTemplateItemOperation(mapRenderGrid, true),
                    MapRenderTemplateItemOperation(mapRenderGps, true),
                    MapRenderTemplateItemOperation(mapRenderSymbol, false),
                    MapRenderTemplateItemOperation(mapRenderGpsTrail, false)))

    private val previewTemplate: MapRenderTemplateItem = MapRenderTemplateItem(
            MapRenderTemplateIdentifier.PREVIEW,
            listOf(MapRenderTemplateItemOperation(mapRenderSequence, true),
                    MapRenderTemplateItemOperation(mapRenderSymbol, true),
                    MapRenderTemplateItemOperation(mapRenderCoverage, false),
                    MapRenderTemplateItemOperation(mapRenderGps, false),
                    MapRenderTemplateItemOperation(mapRenderGrid, false),
                    MapRenderTemplateItemOperation(mapRenderGpsTrail, false)))

    private val recordingTemplate: MapRenderTemplateItem = MapRenderTemplateItem(
            MapRenderTemplateIdentifier.RECORDING,
            listOf(MapRenderTemplateItemOperation(mapRenderCoverage, true),
                    MapRenderTemplateItemOperation(mapRenderSequence, true),
                    MapRenderTemplateItemOperation(mapRenderGrid, true),
                    MapRenderTemplateItemOperation(mapRenderGpsTrail, true),
                    MapRenderTemplateItemOperation(mapRenderGps, true),
                    MapRenderTemplateItemOperation(mapRenderSymbol, false)))

    private var currentTemplate: MapRenderTemplateIdentifier? = null

    /**
     * Method which will apply the given identified template.
     *
     * Note that this will only set the template when the current one is different than one identified.
     */
    fun applyIfRequired(templateIdentifier: MapRenderTemplateIdentifier) {
        if (currentTemplate == null || currentTemplate != templateIdentifier) {
            Log.d(TAG, "applyIfRequired. Template identifier: $templateIdentifier. Current template: $currentTemplate.")
            /*//edge case since the grid requires started time for coverage it must set it only when applied via applyGridIfRequired method
            if(templateIdentifier != MapRenderTemplateIdentifier.GRID) {
                currentTemplate = templateIdentifier
            }*/
            currentTemplate = templateIdentifier
            val newTemplate: MapRenderTemplateItem = when (templateIdentifier) {
                MapRenderTemplateIdentifier.RECORDING -> recordingTemplate
                MapRenderTemplateIdentifier.DEFAULT -> defaultTemplate
                MapRenderTemplateIdentifier.GRID -> gridTemplate
                MapRenderTemplateIdentifier.PREVIEW -> previewTemplate
            }
            applyTemplate(newTemplate)
            Log.d(TAG, "applyIfRequired. New template: $newTemplate.")
        }
    }

//    /**
//     * Method which will apply the grid template.
//     *
//     */
//    fun applyGridIfRequired(startDate: Int) {
//        if (currentTemplate != MapRenderTemplateIdentifier.GRID) {
//            Log.d(TAG, "applyGridIfRequired. Template identifier: ${MapRenderTemplateIdentifier.GRID}. Current template: $currentTemplate. Start date: $startDate")
//            currentTemplate = MapRenderTemplateIdentifier.GRID
//            gridTemplate.operations.forEach {
//                it.mapBoxRender.clear()
//            }
//            gridTemplate.operations.forEach {
//                if (it.addOperation) {
//                    if (it.mapBoxRender is MapBoxRenderCoverage) {
//                        it.mapBoxRender.render(startDate)
//                    } else {
//                        it.mapBoxRender.render()
//                    }
//                }
//            }
//        }
//    }

    private fun applyTemplate(mapRenderTemplateItem: MapRenderTemplateItem) {
        mapRenderTemplateItem.operations.forEach {
            it.mapBoxRender.clear()
        }
        mapRenderTemplateItem.operations.forEach {
            if (it.addOperation) {
                it.mapBoxRender.render()
            }
        }
    }

    private companion object {
        lateinit var TAG: String
    }
}