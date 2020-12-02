package com.telenav.osv.map.render.template

import com.telenav.osv.map.render.mapbox.MapBoxRenderBase

/**
 * Data class which holds the render with the operation by which it will be called.
 */
data class MapRenderTemplateItemOperation(val mapBoxRender: MapBoxRenderBase, val addOperation: Boolean)

/**
 * Data class which holds the identifier identifiers and a list of operations
 */
data class MapRenderTemplateItem(val identifier: MapRenderTemplateIdentifier, val operations: List<MapRenderTemplateItemOperation>)