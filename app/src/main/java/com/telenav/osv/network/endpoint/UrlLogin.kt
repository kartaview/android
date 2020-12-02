package com.telenav.osv.network.endpoint

/**
 * Enum class used to determine login type.
 */
enum class UrlLogin(val value: String) {
    OSM("auth/openstreetmap/client_auth"),
    Google("auth/google/client_auth"),
    Facebook("auth/facebook/client_auth");
}