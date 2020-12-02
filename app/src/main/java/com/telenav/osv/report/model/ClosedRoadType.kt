package com.telenav.osv.report.model

private const val TYPE_CLOSED = 1
private const val TYPE_NARROW = 2
private const val TYPE_OTHER = 3

enum class ClosedRoadType(val type: Int) {
    CLOSED(TYPE_CLOSED),
    NARROW(TYPE_NARROW),
    OTHER(TYPE_OTHER);

    companion object {
        fun getByType(type: Int): ClosedRoadType? = values().firstOrNull { it.type == type }
    }
}