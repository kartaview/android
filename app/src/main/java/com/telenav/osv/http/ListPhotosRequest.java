package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.telenav.osv.listener.network.GenericResponseListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class ListPhotosRequest extends KvVolleyStringRequestJarvisAuthorization {

    private static final String PARAM_SEQUENCE_ID = "sequenceId";

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private final long mSequenceId;

    private final String mToken;

    public ListPhotosRequest(String url, GenericResponseListener listener, long sequenceId, String token, boolean isJarvisAuthorization, String jarvisAccessToken) {
        super(Method.POST, url, listener, listener, isJarvisAuthorization, jarvisAccessToken);
        mToken = token;
        mSequenceId = sequenceId;
        mListener = listener;
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