package com.telenav.osv.tasks.model

private const val GRID_TO_DO = 1
private const val GRID_RECORDING = 2
private const val GRID_MAP_OPS_QC = 4
private const val GRID_DONE = 7
private const val GRID_PAID = 8

enum class GridStatus(val status: Int) {
    TO_DO(GRID_TO_DO),
    RECORDING(GRID_RECORDING),
    MAP_OPS_QC(GRID_MAP_OPS_QC),
    DONE(GRID_DONE),
    PAID(GRID_PAID);

    companion object {
        fun getByStatus(status: Int): GridStatus? = values().firstOrNull { it.status == status }
    }
}