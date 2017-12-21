package com.telenav.osv.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;

public class DriverPayRatesRequest extends StringRequest {

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private final String mToken;

    public DriverPayRatesRequest(String url, GenericResponseListener listener, String token) {
        super(Method.POST, url, listener, listener);
        mToken = token;
        mListener = listener;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = super.getHeaders();

        if (headers == null || headers.equals(Collections.emptyMap())) {
            headers = new HashMap<>();
        }

        headers.put("Accept", "application/json");

        return headers;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = super.getParams();
        if (params == null || params.equals(Collections.emptyMap())) {
            params = new HashMap<>();
        }
        params.put(PARAM_TOKEN, mToken);
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}
