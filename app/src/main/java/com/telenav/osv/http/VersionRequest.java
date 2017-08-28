package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kalman on 10/6/2015.
 */
public class VersionRequest extends StringRequest {

  private final GenericResponseListener mListener;

  private String mLat;

  private String mLon;

  private String mRadius;

  public VersionRequest(String url, GenericResponseListener listener) {
    super(Method.POST, url, listener, listener);
    mListener = listener;
  }

  @Override
  public Map<String, String> getHeaders() throws AuthFailureError {
    Map<String, String> headers = super.getHeaders();

    if (headers == null || headers.equals(Collections.emptyMap())) {
      headers = new HashMap<>();
    }

    headers.put("Accept", "application/json");

    return headers;
  }

  @Override
  protected Map<String, String> getParams() throws AuthFailureError {
    Map<String, String> params = super.getParams();
    if (params == null || params.equals(Collections.emptyMap())) {
      params = new HashMap<>();
    }
    params.put("platform", "android");

    return params;
  }

  @Override
  protected void deliverResponse(String response) {
    mListener.onResponse(response);
  }
}