package com.telenav.osv.network

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.data.user.datasource.UserDataSource
import com.telenav.osv.jarvis.login.model.JarvisRefreshTokenResponse
import com.telenav.osv.jarvis.login.network.JarvisRefreshTokenRequest
import com.telenav.osv.jarvis.login.usecase.JarvisLoginUseCase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.HttpURLConnection

/**
 * This handles generic error for Jarvis API
 * It helps in refreshing token on token expiry
 */
interface GenericJarvisApiErrorHandler {
    fun onError(
            throwable: Throwable,
            listener: GenericJarvisApiErrorHandlerListener,
            disposables: CompositeDisposable
    )
}

interface GenericJarvisApiErrorHandlerListener {
    fun onRefreshTokenSuccess()
    fun onError()
    fun reLogin()
}

private const val MAX_RETRY_COUNT = 3

internal class GenericJarvisApiErrorHandlerImpl(
        private val jarvisLoginUseCase: JarvisLoginUseCase,
        private val userDataSource: UserDataSource,
        private val applicationPreferences: ApplicationPreferences
): GenericJarvisApiErrorHandler {

    override fun onError(
            throwable: Throwable,
            listener: GenericJarvisApiErrorHandlerListener,
            disposables: CompositeDisposable
    ) {
        if (throwable is HttpException && throwable.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            disposables.add(userDataSource.user
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { user ->
                                val accessToken = user.jarvisAccessToken
                                val refreshToken = user.jarvisRefreshToken
                                if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                                    listener.reLogin()
                                } else {
                                    val refreshTokenRequest = JarvisRefreshTokenRequest(accessToken, refreshToken)
                                    refreshToken(refreshTokenRequest, listener, disposables, 0)
                                }
                            },
                            {
                                listener.reLogin()
                            },
                            {
                                listener.reLogin()
                            }
                    ))
        } else {
            listener.onError()
        }
    }

    private fun refreshToken(
            refreshTokenRequest: JarvisRefreshTokenRequest,
            listener: GenericJarvisApiErrorHandlerListener,
            disposables: CompositeDisposable,
            retryCount: Int) {
        if (retryCount < MAX_RETRY_COUNT) {
            disposables.add(jarvisLoginUseCase.jarvisRefreshToken(refreshTokenRequest)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { jarvisRefreshTokenResponse ->
                                updateToken(jarvisRefreshTokenResponse)
                                listener.onRefreshTokenSuccess()
                            },
                            {
                                if (it is HttpException && it.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                                    listener.reLogin()
                                } else {
                                    refreshToken(refreshTokenRequest, listener, disposables, retryCount + 1)
                                }
                            }
                    ))
        } else {
            listener.reLogin()
        }
    }

    private fun updateToken(jarvisRefreshTokenResponse: JarvisRefreshTokenResponse) {
        applicationPreferences.saveStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN, jarvisRefreshTokenResponse.jwt)
        applicationPreferences.saveStringPreference(PreferenceTypes.JARVIS_REFRESH_TOKEN, jarvisRefreshTokenResponse.refreshToken)
    }
}