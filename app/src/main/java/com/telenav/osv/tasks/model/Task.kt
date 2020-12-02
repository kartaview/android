package com.telenav.osv.tasks.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Task(
        @SerializedName("id") @Expose val id: String,
        @SerializedName("title") @Expose val title: String,
        @SerializedName("campaignId") @Expose val campaignId: String,
        @SerializedName("neLat") @Expose val neLat: Float,
        @SerializedName("neLng") @Expose val neLng: Float,
        @SerializedName("swLat") @Expose val swLat: Float,
        @SerializedName("swLng") @Expose val swLng: Float,
        @SerializedName("status") @Expose val status: Int,
        @SerializedName("assignedUserId") @Expose val assignedUserId: Int? = null,
        @SerializedName("assignedUserName") @Expose val assignedUserName: String? = null,
        @SerializedName("createdBy") @Expose val createdBy: Int,
        @SerializedName("createdAt") @Expose val createdAt: Int,
        @SerializedName("operationLogs") @Expose val operationLogs: List<OperationLog>?,
        @SerializedName("amount") @Expose val amount: Double,
        @SerializedName("ukm") @Expose val ukm: Double? = null,
        @SerializedName("currency") @Expose val currency: String
)