package com.telenav.osv.map.render.mapbox.grid

import com.telenav.osv.tasks.model.GridStatus

/**
 * Enum class which offer distinctions between all grids.
 */
enum class MapBoxRenderStatus(val value: Int) {
    TO_DO(GridStatus.TO_DO.status),
    RECORDING(GridStatus.RECORDING.status),
    REVIEW(GridStatus.MAP_OPS_QC.status),
    DONE(GridStatus.DONE.status),
    PAID(GridStatus.PAID.status),

    // The value is 0 since there isn't any value for unavailable
    UNAVAILABLE(0);
}