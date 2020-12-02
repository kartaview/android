package com.telenav.osv.report.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.telenav.osv.R
import com.telenav.osv.report.model.ClosedRoadType

@DrawableRes
fun getClosedRoadIcon(closedRoadType: ClosedRoadType): Int {
    return when(closedRoadType) {
        ClosedRoadType.CLOSED -> R.drawable.vector_road_closed_issue
        ClosedRoadType.NARROW -> R.drawable.vector_road_inaccessible_issue
        ClosedRoadType.OTHER -> R.drawable.vector_other_road_issue
    }
}

@StringRes
fun getClosedRoadContent(closedRoadType: ClosedRoadType): Int {
    return when(closedRoadType) {
        ClosedRoadType.CLOSED -> R.string.road_closed
        ClosedRoadType.NARROW -> R.string.road_inaccessible
        ClosedRoadType.OTHER -> R.string.other_road_issue
    }
}