package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adrianbostan on 22/07/16.
 */

public class ProfileRequest extends StringRequest {

    private static final String PARAM_USER_ID = "externalUserId";

    private final Response.Listener<String> mListener;

    private final String mUserId;

    public ProfileRequest(String url, Response.ErrorListener errorListener, Response.Listener<String> listener, String userId) {
        super(Request.Method.POST, url, listener, errorListener);
        mUserId = userId;
        mListener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = super.getHeaders();

        if (headers == null
                || headers.equals(Collections.emptyMap())) {
            headers = new HashMap<>();
        }

        headers.put("Accept", "application/json");

        return headers;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = super.getParams();
        if (params == null
                || params.equals(Collections.emptyMap())) {
            params = new HashMap<>();
        }
        params.put(PARAM_USER_ID, mUserId);
        return params;
    }


    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

}
