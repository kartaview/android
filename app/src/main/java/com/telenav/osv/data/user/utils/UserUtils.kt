package com.telenav.osv.data.user.utils

import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.data.user.model.User
import com.telenav.osv.item.AccountData
import com.telenav.osv.jarvis.login.model.JarvisLoginResponse
import com.telenav.osv.manager.network.LoginManager

object UserUtils {

    /**
     * @param jarvisLoginResponse the api response for Jarvis login.
     * @return {@code User} representing the user created from Jarvis login response.
     */
    @JvmStatic
    fun getUser(jarvisLoginResponse: JarvisLoginResponse): User {
        val kv = jarvisLoginResponse.kvLoginInfo.kv
        val jarvisUserInfo = jarvisLoginResponse.userInfo
        val driver = "driver"
        var type: String? = kv.type
        if (type == driver) {
            type = kv.driverType
        }
        var userType = PreferenceTypes.USER_TYPE_CONTRIBUTOR
        if (type != null) {
            userType = AccountData.getUserTypeForString(type)
        }
        return User(
                kv.id,
                kv.accessToken,
                kv.fullName,
                LoginManager.LOGIN_TYPE_PARTNER,
                kv.username,
                userType,
                jarvisUserInfo.userId,
                jarvisUserInfo.name,
                jarvisLoginResponse.jwt,
                jarvisLoginResponse.refreshToken,
                null)
    }
}