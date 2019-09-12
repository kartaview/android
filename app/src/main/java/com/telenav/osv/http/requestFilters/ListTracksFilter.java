package com.telenav.osv.http.requestFilters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.telenav.osv.http.ListTracksRequest;

/**
 * Created by Kalman on 12/10/15.
 */
class ListTracksFilter implements RequestQueue.RequestFilter {

    @Override
    public boolean apply(Request<?> request) {
        boolean cancel = request instanceof ListTracksRequest;
        return cancel;
    }
}
