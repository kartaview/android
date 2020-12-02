package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.telenav.osv.listener.network.GenericResponseListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adrianbostan on 22/07/16.
 */

public class ProfileRequest extends KvVolleyStringRequestJarvisAuthorization {

    private static final String PARAM_USER_NAME = "username";

    private final GenericResponseListener mListener;

    private final String mName;

    public ProfileRequest(String url, GenericResponseListener listener, String name, boolean isJarvisAuthorization, String jarvisAccessToken) {
        super(Request.Method.POST, url, listener, listener, isJarvisAuthorization, jarvisAccessToken);
        mName = name;
        mListener = listener;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = super.getParams();
        if (params == null || params.equals(Collections.emptyMap())) {
            params = new HashMap<>();
        }
        params.put(PARAM_USER_NAME, mName);
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}
