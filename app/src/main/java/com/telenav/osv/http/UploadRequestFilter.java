package com.telenav.osv.http;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

/**
 * Created by Kalman on 11/18/15.
 */
public class UploadRequestFilter implements RequestQueue.RequestFilter {

    @Override
    public boolean apply(Request<?> request) {
        return request instanceof VideoRequest || request instanceof PhotoRequest;
    }
}
