package com.telenav.osv.map.model

/**
 * Enum class which enumerates all available map modes.
 *
 * Available values:
 * * [IDLE]
 * * [RECORDING]
 * * [RECORDING_MAP_DISABLED]
 * * [GRID]
 * * [PREVIEW_MAP]
 * * [PREVIEW_MAP_DISABLED]
 * * [DISABLED]
 */
enum class MapModes(val mode: Int) {
    IDLE(0),
    RECORDING(1),
    RECORDING_MAP_DISABLED(2),
    GRID(3),
    PREVIEW_MAP(4),
    PREVIEW_MAP_DISABLED(5),
    DISABLED(6);

    companion object {
        fun getByType(mode: Int): MapModes? = values().firstOrNull { it.mode == mode }
    }
}