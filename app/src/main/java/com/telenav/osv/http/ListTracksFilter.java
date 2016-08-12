package com.telenav.osv.http;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

/**
 * Created by Kalman on 12/10/15.
 */
public class ListTracksFilter implements RequestQueue.RequestFilter {
    @Override
    public boolean apply(Request<?> request) {
        boolean cancel = request instanceof ListTracksRequest;
        return cancel;
    }
}
