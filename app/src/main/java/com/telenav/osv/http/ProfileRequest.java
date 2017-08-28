package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by adrianbostan on 22/07/16.
 */

public class ProfileRequest extends StringRequest {

  private static final String PARAM_USER_NAME = "username";

  private final GenericResponseListener mListener;

  private final String mName;

  public ProfileRequest(String url, GenericResponseListener listener, String name) {
    super(Request.Method.POST, url, listener, listener);
    mName = name;
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
    params.put(PARAM_USER_NAME, mName);
    return params;
  }

  @Override
  protected void deliverResponse(String response) {
    mListener.onResponse(response);
  }
}
