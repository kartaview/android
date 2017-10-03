package com.telenav.osv.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;

/**
 * Created by Kalman on 10/6/2015.
 */
public class SequenceFinishedRequest<T> extends StringRequest {

    private static final String PARAM_USER_ID = "externalUserId";

    private static final String PARAM_CURRENT_COORD = "currentCoordinate";

    private static final String PARAM_SEQUENCE_ID = "sequenceId";

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private String mToken;

    private String sequenceId;

    public SequenceFinishedRequest(String url, GenericResponseListener listener, String token, String sequenceId) {
        super(Method.POST, url, listener, listener);
        mListener = listener;
        this.mToken = token;
        this.sequenceId = sequenceId;
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
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TOKEN, mToken);
        params.put(PARAM_SEQUENCE_ID, sequenceId);
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}