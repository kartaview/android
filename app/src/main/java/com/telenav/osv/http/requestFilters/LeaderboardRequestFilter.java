package com.telenav.osv.http.requestFilters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.telenav.osv.http.LeaderboardRequest;

/**
 * Created by Kalman on 24/11/2016.
 */

public class LeaderboardRequestFilter implements RequestQueue.RequestFilter {

  @Override
  public boolean apply(Request<?> request) {
    return request instanceof LeaderboardRequest;
  }
}
