package com.telenav.osv.network.response;

import androidx.annotation.StringDef;

@StringDef
public @interface NetworkRequestHeaderIdentifiers {
    String HEADER_ACCEPT = "Accept";

    String HEADER_REQUEST_IDENTIFIER = "request-identifier";
}
