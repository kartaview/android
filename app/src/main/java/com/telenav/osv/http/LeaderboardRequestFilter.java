package com.telenav.osv.http;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

/**
 * Created by Kalman on 24/11/2016.
 */

public class LeaderboardRequestFilter implements RequestQueue.RequestFilter {
    @Override
    public boolean apply(Request<?> request) {
        return request instanceof LeaderboardRequest;
    }
}
