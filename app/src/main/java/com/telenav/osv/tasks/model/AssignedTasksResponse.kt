package com.telenav.osv.tasks.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AssignedTasksResponse (
        @SerializedName("result") @Expose val result: AssignedTasksData
)

data class AssignedTasksData (
        @SerializedName("data") @Expose val data: List<Task>
)