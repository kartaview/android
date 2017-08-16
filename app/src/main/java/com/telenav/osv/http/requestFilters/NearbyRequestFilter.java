package com.telenav.osv.http.requestFilters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.telenav.osv.http.NearbyRequest;

/**
 * Created by Kalman on 12/10/15.
 */
public class NearbyRequestFilter implements RequestQueue.RequestFilter {
    @Override
    public boolean apply(Request<?> request) {
        return request instanceof NearbyRequest;
    }
}
