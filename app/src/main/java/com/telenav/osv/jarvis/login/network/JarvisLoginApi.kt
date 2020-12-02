package com.telenav.osv.jarvis.login.network

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.telenav.osv.jarvis.login.model.JarvisLoginResponse
import com.telenav.osv.jarvis.login.model.JarvisRefreshTokenResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface JarvisLoginApi {

    companion object {
        const val KV_ACCESS_TOKEN = "OSC-Access-Token"
    }

    @POST("login")
    fun login(
            @Header(KV_ACCESS_TOKEN) accessToken: String,
            @Body loginRequest: JarvisLoginRequest
    ): Single<JarvisLoginResponse>

    @POST("refresh_token")
    fun refreshToken(
            @Body refreshTokenRequest: JarvisRefreshTokenRequest
    ): Single<JarvisRefreshTokenResponse>
}

internal data class JarvisLoginRequest(
        @SerializedName("login_method") @Expose val loginMethod: String,
        @SerializedName("password") @Expose val password: String
)

internal data class JarvisRefreshTokenRequest(
        @SerializedName("access_token") @Expose val accessToken: String,
        @SerializedName("refresh_token") @Expose val refreshToken: String
)