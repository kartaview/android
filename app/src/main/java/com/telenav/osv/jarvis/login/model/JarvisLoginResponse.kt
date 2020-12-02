package com.telenav.osv.jarvis.login.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class JarvisLoginResponse(
        @SerializedName("jwt") @Expose val jwt: String,
        @SerializedName("refreshToken") @Expose val refreshToken: String,
        @SerializedName("user_info") @Expose val userInfo: JarvisUserInfo,
        @SerializedName("osc_login_info") @Expose val kvLoginInfo: KVLoginInfo
)