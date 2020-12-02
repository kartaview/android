package com.telenav.osv.tasks.utils

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.telenav.osv.R
import com.telenav.osv.map.render.mapbox.grid.MapBoxRenderStatus
import com.telenav.osv.tasks.model.GridStatus

@ColorRes
fun getTaskStatusColor(gridStatus: GridStatus): Int {
    return when (gridStatus) {
        GridStatus.TO_DO -> R.color.default_grab_grey
        GridStatus.RECORDING -> R.color.default_purple
        GridStatus.MAP_OPS_QC -> R.color.color_ffb500
        GridStatus.DONE -> R.color.color_00b14f
        GridStatus.PAID -> R.color.color_878C96
    }
}

@ColorRes
fun getMapBackgroundMap(renderStatus: MapBoxRenderStatus): Int {
    return when (renderStatus) {
        MapBoxRenderStatus.TO_DO -> R.color.default_grab_grey
        MapBoxRenderStatus.RECORDING -> R.color.default_purple
        MapBoxRenderStatus.REVIEW -> R.color.color_ffb500
        MapBoxRenderStatus.DONE -> R.color.color_00b14f
        MapBoxRenderStatus.PAID -> R.color.color_878C96
        MapBoxRenderStatus.UNAVAILABLE -> R.color.color_de000000
    }
}

@DrawableRes
fun getTaskStatusIcon(gridStatus: GridStatus): Int {
    return when (gridStatus) {
        GridStatus.TO_DO -> R.drawable.vector_grid_status_to_do
        GridStatus.RECORDING -> R.drawable.vector_grid_status_recording
        GridStatus.MAP_OPS_QC -> R.drawable.vector_grid_status_map_ops_qc
        GridStatus.DONE -> R.drawable.vector_grid_status_done
        GridStatus.PAID -> R.drawable.vector_grid_status_paid
    }
}

@StringRes
fun getTaskStatusContent(gridStatus: GridStatus): Int {
    return when (gridStatus) {
        GridStatus.TO_DO -> R.string.grid_status_to_do
        GridStatus.RECORDING -> R.string.grid_status_recording
        GridStatus.MAP_OPS_QC -> R.string.grid_status_map_ops_qc
        GridStatus.DONE -> R.string.grid_status_done
        GridStatus.PAID -> R.string.grid_status_paid
    }
}

@DrawableRes
fun getTaskStatusBackground(gridStatus: GridStatus): Int {
    return when (gridStatus) {
        GridStatus.TO_DO -> R.drawable.bg_grid_status_to_do
        GridStatus.RECORDING -> R.drawable.bg_grid_status_recording
        GridStatus.MAP_OPS_QC -> R.drawable.bg_grid_status_map_ops_qc
        GridStatus.DONE -> R.drawable.bg_grid_status_done
        GridStatus.PAID -> R.drawable.bg_grid_status_paid
    }
}

@DrawableRes
fun getMapLabelBackgroundMap(mapBoxRenderStatus: MapBoxRenderStatus): Int {
    return when (mapBoxRenderStatus) {
        MapBoxRenderStatus.TO_DO -> R.drawable.ic_jarvis_map_available
        MapBoxRenderStatus.RECORDING -> R.drawable.ic_jarvis_map_recording
        MapBoxRenderStatus.REVIEW -> R.drawable.ic_jarvis_map_waiting
        MapBoxRenderStatus.DONE -> R.drawable.ic_jarvis_map_approved
        MapBoxRenderStatus.PAID -> R.drawable.ic_jarvis_map_paid
        MapBoxRenderStatus.UNAVAILABLE -> R.drawable.ic_jarvis_map_locked
    }
}

@DrawableRes
fun getTaskCurrencyBackground(): Int {
    return R.drawable.ic_bg_currency
}