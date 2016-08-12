package com.telenav.osv.http;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.StringRequest;

/**
 * Created by Kalman on 10/6/2015.
 */
public class SequenceFinishedRequest<T> extends StringRequest {

    private static final String PARAM_USER_ID = "externalUserId";

    private static final String PARAM_USER_TYPE = "userType";

    private static final String PARAM_USER_NAME = "userName";

    private static final String PARAM_CLIENT_TOKEN = "clientToken";

    private static final String PARAM_CURRENT_COORD = "currentCoordinate";

    private static final String PARAM_SEQUENCE_ID = "sequenceId";

    private final Listener<String> mListener;

    private String userId;

    private String sequenceId;

    public SequenceFinishedRequest(String url, ErrorListener errorListener, Listener<String> listener, String userId, String sequenceId) {
        super(Method.POST, url, listener, errorListener);
        mListener = listener;
        this.userId = userId;
        this.sequenceId = sequenceId;
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
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_USER_ID, userId);
        params.put(PARAM_USER_TYPE, "osm");
        params.put(PARAM_SEQUENCE_ID, sequenceId);
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}