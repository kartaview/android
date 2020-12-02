package com.telenav.osv.network.endpoint

enum class UrlIssue(val value:String) {
    ISSUE_CREATE("1.0/issue/"),
    ISSUE_UPLOAD_FILE("1.0/upload/issue-file/")
}