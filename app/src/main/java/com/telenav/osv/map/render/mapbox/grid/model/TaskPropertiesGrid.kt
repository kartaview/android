package com.telenav.osv.map.render.mapbox.grid.model

import com.google.gson.annotations.SerializedName
import com.telenav.osv.map.render.mapbox.grid.MapBoxRenderGrid

data class TaskPropertiesGrid(@SerializedName(MapBoxRenderGrid.JSON_PROPERTY_GRID_ID) val id: String)