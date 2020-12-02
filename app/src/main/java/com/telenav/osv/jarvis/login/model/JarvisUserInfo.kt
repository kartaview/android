package com.telenav.osv.jarvis.login.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class JarvisUserInfo(
        @SerializedName("user_id") @Expose val userId: Int,
        @SerializedName("name") @Expose val name: String
)