package com.telenav.osv.tasks.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GridDetailsResponse(
        @SerializedName("result") @Expose val result: GridDetailsData
)

data class GridDetailsData(
        @SerializedName("data") @Expose val data: Task
)