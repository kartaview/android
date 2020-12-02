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
public class ListSequencesRequest extends KvVolleyStringRequestJarvisAuthorization {

    private static final String PARAM_PAGE_NUMBER = "page";

    private static final String PARAM_NUMBER_OF_RESULT = "ipp";

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private final int mPageIndex;

    private final int mResultsToLoad;

    private final String mToken;

    public ListSequencesRequest(String url, GenericResponseListener listener, String token, int pageNr, int numberOfResults, boolean isJarvisAuthorization, String jarvisAccessToken) {
        super(Method.POST, url, listener, listener, isJarvisAuthorization, jarvisAccessToken);
        mToken = token;
        mListener = listener;
        mPageIndex = pageNr;
        mResultsToLoad = numberOfResults;
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
        params.put(PARAM_PAGE_NUMBER, mPageIndex + "");
        params.put(PARAM_NUMBER_OF_RESULT, mResultsToLoad + "");
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}