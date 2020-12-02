package com.telenav.osv.network.request.interceptor

import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers.*
import okhttp3.Interceptor
import okhttp3.Response

class JarvisRequestAuthorizationInterceptor(private val applicationPreferences: ApplicationPreferences) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        builder.addHeader(HEADER_AUTH_TYPE, HEADER_AUTH_TYPE_VALUE_TOKEN)
        builder.addHeader(HEADER_AUTHORIZATION, applicationPreferences.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN))
        return chain.proceed(builder.build())
    }
}