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
public class ListSequencesRequest extends StringRequest {

    private static final String PARAM_USER_ID = "externalUserId";

    private static final String PARAM_USERNAME = "userName";

    private static final String PARAM_USER_TYPE = "userType";

    private static final String PARAM_PAGE_NUMBER = "page";

    private static final String PARAM_NUMBER_OF_RESULT = "ipp";

    private static final String PARAM_BB_TOP_LEFT = "bbTopLeft";

    private static final String PARAM_BB_BOTTOM_RIGHT = "bbBottomRight";

    public final String mBbTopLeft;

    public final String mBbBottomRight;

    private final Listener<String> mListener;

    private final String mUserName;

    private final int mPageIndex;

    private final int mResultsToLoad;

    private final String mUserId;

    public ListSequencesRequest(String url, ErrorListener errorListener, Listener<String> listener, String userId, String username, int pageNr, int numberOfResults, String
            bbTopLeft, String bbBottomRight) {
        super(Method.POST, url, listener, errorListener);
        mUserId = userId;
        mUserName = username;
        mListener = listener;
        mPageIndex = pageNr;
        mResultsToLoad = numberOfResults;
        mBbTopLeft = bbTopLeft;
        mBbBottomRight = bbBottomRight;
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
        if (!mUserId.equals("") && !mUserName.equals("")) {
            params.put(PARAM_USER_ID, mUserId);
            params.put(PARAM_USER_TYPE, "osm");
            params.put(PARAM_USERNAME, mUserName);
        }
        params.put(PARAM_PAGE_NUMBER, mPageIndex + "");
        params.put(PARAM_NUMBER_OF_RESULT, mResultsToLoad + "");
        if (mBbBottomRight != null && mBbTopLeft != null) {
            params.put(PARAM_BB_TOP_LEFT, mBbTopLeft);
            params.put(PARAM_BB_BOTTOM_RIGHT, mBbBottomRight);
        }
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}