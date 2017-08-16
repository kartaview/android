package com.telenav.osv.http.requestFilters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.telenav.osv.http.SequenceRequest;

/**
 * Created by Kalman on 1/13/16.
 */
public class SequenceRequestFilter implements RequestQueue.RequestFilter {
    @Override
    public boolean apply(Request<?> request) {
        return request instanceof SequenceRequest;
    }
}
