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
@SuppressWarnings("HardCodedStringLiteral")
public class ListPhotosRequest extends StringRequest {

    private static final String PARAM_SEQUENCE_ID = "sequenceId";

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private final int mSequenceId;

    private final String mToken;

    public ListPhotosRequest(String url, GenericResponseListener listener, int sequenceId, String token) {
        super(Method.POST, url, listener, listener);
        mToken = token;
        mSequenceId = sequenceId;
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
        params.put(PARAM_SEQUENCE_ID, mSequenceId + "");
        if (mToken != null && !mToken.equals("")) {
            params.put(PARAM_TOKEN, mToken);
        }
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}