package com.telenav.osv.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;

/**
 * Created by kalmanb on 7/12/17.
 */
public class ListPaymentsRequest extends StringRequest {
    private static final String PARAM_PAGE_NUMBER = "page";

    private static final String PARAM_NUMBER_OF_RESULT = "ipp";

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private final int mPageIndex;

    private final int mResultsToLoad;

    private final String mToken;

    public ListPaymentsRequest(String url, GenericResponseListener listener, String accessToken, int pageNumber, int
            itemsPerPage) {
        super(Request.Method.POST, url, listener, listener);
        mToken = accessToken;
        mListener = listener;
        mPageIndex = pageNumber;
        mResultsToLoad = itemsPerPage;
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
