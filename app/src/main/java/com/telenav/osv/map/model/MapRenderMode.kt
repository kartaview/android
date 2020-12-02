package com.telenav.osv.map.model

/**
 * Mode which represents the render status.
 */
enum class MapRenderMode(val value: Int) {
    DEFAULT(0),
    DEFAULT_WITH_GRID(1),
    RECORDING(2),
    PREVIEW(3),
    DISABLED(4);
}