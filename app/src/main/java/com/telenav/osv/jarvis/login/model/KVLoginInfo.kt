package com.telenav.osv.jarvis.login.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * This data class maps API response for Login
 * Provides data such as access token, id, username, full name, etc for KV user
 */
data class KVLoginInfo(
        @SerializedName("osv") @Expose val kv: KV
)

data class KV(
        @SerializedName("access_token") @Expose val accessToken: String,
        @SerializedName("id") @Expose val id: String,
        @SerializedName("username") @Expose val username: String,
        @SerializedName("full_name") @Expose val fullName: String? = null,
        @SerializedName("type") @Expose val type: String,
        @SerializedName("driver_type") @Expose val driverType: String? = null
)