package com.telenav.osv.jarvis.login.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class JarvisRefreshTokenResponse(
        @SerializedName("jwt") @Expose val jwt: String,
        @SerializedName("refreshToken") @Expose val refreshToken: String
)