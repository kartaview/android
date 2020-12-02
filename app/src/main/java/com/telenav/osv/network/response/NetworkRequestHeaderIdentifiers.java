package com.telenav.osv.network.response;

import androidx.annotation.StringDef;

@StringDef
public @interface NetworkRequestHeaderIdentifiers {
    String HEADER_ACCEPT = "Accept";

    String HEADER_REQUEST_IDENTIFIER = "request-identifier";

    String HEADER_AUTH_TYPE = "AuthType";

    String HEADER_AUTHORIZATION = "Authorization";

    String HEADER_AUTH_TYPE_VALUE_TOKEN = "token";
}
