package com.telenav.osv.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;

/**
 * Created by Kalman on 02/05/2017.
 */

public class IssueCreationRequest extends StringRequest {

    private static final String TAG = "IssueRequest";

    private static final String PARAM_TOKEN = "access_token";

    private static final String PARAM_DETAILS = "details";

    private final GenericResponseListener mListener;

    private final String mToken;

    private final String mDetails;

    protected Map<String, String> headers;

    public IssueCreationRequest(String url, GenericResponseListener listener, String details, String token) {
        super(Method.POST, url, listener, listener);
        mListener = listener;
        mDetails = details;
        mToken = token;
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
        if (mToken != null && !mToken.equals("")) {
            params.put(PARAM_TOKEN, mToken);
        }
        params.put(PARAM_DETAILS, mDetails);
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}
