package com.telenav.osv.jarvis.login.usecase

import com.telenav.osv.jarvis.login.model.JarvisLoginResponse
import com.telenav.osv.jarvis.login.model.JarvisRefreshTokenResponse
import com.telenav.osv.jarvis.login.network.JarvisLoginApi
import com.telenav.osv.jarvis.login.network.JarvisLoginRequest
import com.telenav.osv.jarvis.login.network.JarvisRefreshTokenRequest
import io.reactivex.Single

internal interface JarvisLoginUseCase {
    fun jarvisLogin(accessToken: String, loginRequest: JarvisLoginRequest): Single<JarvisLoginResponse>
    fun jarvisRefreshToken(refreshTokenRequest: JarvisRefreshTokenRequest): Single<JarvisRefreshTokenResponse>
}

internal class JarvisLoginUseCaseImpl(private val jarvisLoginApi: JarvisLoginApi) : JarvisLoginUseCase {

    override fun jarvisLogin(accessToken: String, loginRequest: JarvisLoginRequest): Single<JarvisLoginResponse> {
        return jarvisLoginApi.login(accessToken, loginRequest)
    }

    override fun jarvisRefreshToken(refreshTokenRequest: JarvisRefreshTokenRequest): Single<JarvisRefreshTokenResponse> {
        return jarvisLoginApi.refreshToken(refreshTokenRequest)
    }
}