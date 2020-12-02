package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers.HEADER_AUTHORIZATION;
import static com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers.HEADER_AUTH_TYPE;
import static com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers.HEADER_AUTH_TYPE_VALUE_TOKEN;

public abstract class KvVolleyStringRequestJarvisAuthorization extends StringRequest {

    private boolean isJarvisAuthorization;

    private String jarvisAccessToken;

    KvVolleyStringRequestJarvisAuthorization(int method,
                                             String url,
                                             Response.Listener<String> listener,
                                             Response.ErrorListener errorListener,
                                             boolean isJarvisAuthorization,
                                             String jarvisAccessToken) {
        super(method, url, listener, errorListener);
        this.isJarvisAuthorization = isJarvisAuthorization;
        this.jarvisAccessToken = jarvisAccessToken;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = super.getHeaders();

        if (headers == null || headers.equals(Collections.emptyMap())) {
            headers = new HashMap<>();
        }

        if (isJarvisAuthorization) {
            headers.put(HEADER_AUTH_TYPE, HEADER_AUTH_TYPE_VALUE_TOKEN);
            headers.put(HEADER_AUTHORIZATION, jarvisAccessToken);
        }
        headers.put("Accept", "application/json");

        return headers;
    }
}
