package com.telenav.osv.map.render.mapbox.grid.model

import com.google.gson.annotations.SerializedName
import com.telenav.osv.map.render.mapbox.grid.MapBoxRenderGrid

data class TaskPropertiesLabel(@SerializedName(MapBoxRenderGrid.JSON_PROPERTY_GRID_TITLE) val title: String)