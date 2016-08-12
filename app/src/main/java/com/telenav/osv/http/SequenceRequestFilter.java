package com.telenav.osv.http;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

/**
 * Created by Kalman on 1/13/16.
 */
public class SequenceRequestFilter implements RequestQueue.RequestFilter {
    @Override
    public boolean apply(Request<?> request) {
        return request instanceof SequenceRequest;
    }
}
