package com.telenav.osv.map.render.mapbox.grid.model

import com.mapbox.geojson.FeatureCollection

data class TaskFeatureCollection(val gridData: FeatureCollection = FeatureCollection.fromFeatures(arrayListOf()), val labelData: FeatureCollection = FeatureCollection.fromFeatures(arrayListOf()))