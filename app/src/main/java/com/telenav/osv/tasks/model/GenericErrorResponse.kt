package com.telenav.osv.tasks.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class GenericErrorResponse(
        @SerializedName("result") @Expose val result: GenericError
)

data class GenericError(
        @SerializedName("error_code") @Expose val errorCode: String? = null,
        @SerializedName("message") @Expose val message: String? = null,
        @SerializedName("title") @Expose val title: String? = null
)