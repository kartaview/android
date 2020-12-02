package com.telenav.osv.report.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

internal data class ClosedRoadResponse(
        @SerializedName("result") @Expose val result: ClosedRoadData
)

internal data class ClosedRoadData(
        @SerializedName("data") @Expose val data: ClosedRoad
)

internal data class ClosedRoad(
        @SerializedName("coordinates") @Expose val coordinates: List<Double>,
        @SerializedName("createdAt") @Expose val createdAt: String,
        @SerializedName("createdBy") @Expose val createdBy: Int,
        @SerializedName("createdByName") @Expose val createdByName: String,
        @SerializedName("id") @Expose val id: Int,
        @SerializedName("note") @Expose val note: String? = null,
        @SerializedName("type") @Expose val type: Int,
        @SerializedName("updatedAt") @Expose val updatedAt: String
)