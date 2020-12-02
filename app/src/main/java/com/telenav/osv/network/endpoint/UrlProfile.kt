package com.telenav.osv.network.endpoint

enum class UrlProfile(val value: String) {
    DELETE_SEQUENCE("1.0/sequence/remove/"),
    LIST_PHOTOS("1.0/sequence/photo-list/"),
    LIST_MY_SEQUENCES("1.0/list/my-list/"),
    DOWNLOAD_PHOTO(""),
    LEADERBOARD("gm-leaderboard"),
    PROFILE_DETAILS("1.0/user/details/");
}