package com.telenav.osv.network.endpoint

import com.telenav.osv.BuildConfig
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.jarvis.login.utils.LoginUtils.isLoginTypePartner
import com.telenav.osv.utils.Log
import com.telenav.osv.utils.StringUtils

/**
 * Factory used in order to fetch server endpoint url. This can be done by calling [.getServerEndpoint].
 *
 *  If any modifications will be done to the persistence where the server url is kept the [.invalidate] can be called to set the correct url.
 *
 *  The method will be called once in the constructor for the current class in order to set the correct url at initialise time.
 *
 * @author horatiuf
 */
class FactoryServerEndpointUrl(private val applicationPreferences: ApplicationPreferences) {

    fun getLoginAuthentication(urlLogin: UrlLogin): String {
        return "${getLoginEndpoint()}${urlLogin.value}"
    }

    fun getProfileEndpoint(profile: UrlProfile): String {
        return "${getServerEndpoint()}${profile.value}"
    }

    fun getGeometryEndpoint(geometry: UrlGeometry): String {
        return "${getServerEndpoint()}${geometry.value}"
    }

    fun getIssueEndpoint(issue: UrlIssue): String {
        return "${getServerEndpoint()}${issue.value}"
    }

    fun getCoverageEndpoint(currentDate: String?): String {
        var appendDate = StringUtils.EMPTY_STRING
        if (currentDate != null) {
            appendDate = "?startDate=$currentDate"
        }
        return "${getServerEndpoint()}$URL_COVERAGE$appendDate"
    }

    /**
     * @return `String` which will have the current server endpoint based on the settings value.
     */
    fun getServerEndpoint(): String {
        val url: String = if (isLoginTypePartner(applicationPreferences)) {
            BuildConfig.GATEWAY_BASE_URL_KV
        } else {
            BuildConfig.KV_BASE_URL
        }
        Log.d(TAG, String.format("invalidate. Status: set environment. Environment: %s.", url))
        return url
    }

    /**
     * @return `String` which will have the current server endpoint for the login.
     */
    private fun getLoginEndpoint(): String {
        val url: String = if (isLoginTypePartner(applicationPreferences)) {
            BuildConfig.GATEWAY_BASE_URL
        } else {
            BuildConfig.KV_BASE_URL
        }
        Log.d(TAG, String.format("invalidate. Status: set environment. Environment: %s.", url))
        return url
    }

    companion object {
        /**
         * The identifier for the current class used in logs.
         */
        private val TAG = FactoryServerEndpointUrl::class.java.simpleName

        private const val URL_COVERAGE = "2.0/sequence/tiles/{x}/{y}/{z}.png"
    }
}