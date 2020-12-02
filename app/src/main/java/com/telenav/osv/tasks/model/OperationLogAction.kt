package com.telenav.osv.tasks.model

private const val ACTION_CHANGE_STATUS = 1
private const val ACTION_ADD_NOTE = 2
private const val ACTION_PICK_UP_GRID = 3
private const val ACTION_GIVE_UP_GRID = 4
private const val ACTION_UPDATE_AMOUNT = 5
private const val ACTION_GRID_CREATION = 6
private const val ACTION_REPORT_CLOSED_ROAD = 7

enum class OperationLogAction(val action: Int) {
    CHANGE_STATUS(ACTION_CHANGE_STATUS),
    ADD_NOTE(ACTION_ADD_NOTE),
    PICK_UP_GRID(ACTION_PICK_UP_GRID),
    GIVE_UP_GRID(ACTION_GIVE_UP_GRID),
    UPDATE_AMOUNT(ACTION_UPDATE_AMOUNT),
    GRID_CREATION(ACTION_GRID_CREATION),
    REPORT_CLOSED_ROAD(ACTION_REPORT_CLOSED_ROAD);

    companion object {
        fun getByAction(action: Int): OperationLogAction? = values().firstOrNull { it.action == action }
    }
}