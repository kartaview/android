package com.telenav.osv.tasks.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class OperationLog(
        @SerializedName("id") @Expose val id: Int,
        @SerializedName("resourceType") @Expose val resourceType: String,
        @SerializedName("resourceId") @Expose val resourceId: String,
        @SerializedName("action") @Expose val action: Int,
        @SerializedName("actionValue") @Expose val actionValue: String?,
        @SerializedName("note") @Expose val note: String? = null,
        @SerializedName("createdByName") @Expose val createdByName: String,
        @SerializedName("createdBy") @Expose val createdBy: Int,
        @SerializedName("updatedAt") @Expose val updatedAt: Long
)