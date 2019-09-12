package com.telenav.osv.http.requestFilters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.telenav.osv.http.ListSequencesRequest;

/**
 * Created by Kalman on 12/10/15.
 */
public class ListRequestFilter implements RequestQueue.RequestFilter {

    @Override
    public boolean apply(Request<?> request) {
        return request instanceof ListSequencesRequest;
    }
}
