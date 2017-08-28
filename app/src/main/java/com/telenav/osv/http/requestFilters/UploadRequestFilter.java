package com.telenav.osv.http.requestFilters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.telenav.osv.http.PhotoRequest;
import com.telenav.osv.http.VideoRequest;

/**
 * Created by Kalman on 11/18/15.
 */
public class UploadRequestFilter implements RequestQueue.RequestFilter {

  @Override
  public boolean apply(Request<?> request) {
    return request instanceof VideoRequest || request instanceof PhotoRequest;
  }
}
