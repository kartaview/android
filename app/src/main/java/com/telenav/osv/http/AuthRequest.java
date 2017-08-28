package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.telenav.osv.listener.network.GenericResponseListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kalman on 29/08/16.
 */
public class AuthRequest extends StringRequest {

  private static final String PARAM_REQUEST_TOKEN = "request_token";

  private static final String PARAM_SECRET_TOKEN = "secret_token";

  private final String mRequestToken;

  private final String mSecretToken;

  public AuthRequest(String url, GenericResponseListener listener, String requestToken, String secretToken) {
    super(Request.Method.POST, url, listener, listener);
    mRequestToken = requestToken;
    mSecretToken = secretToken;
  }

  @Override
  protected Map<String, String> getParams() throws AuthFailureError {
    Map<String, String> params = super.getParams();
    if (params == null || params.equals(Collections.emptyMap())) {
      params = new HashMap<>();
    }
    if (mRequestToken != null && !"".equals(mRequestToken)) {
      params.put(PARAM_REQUEST_TOKEN, mRequestToken);
    }
    if (mSecretToken != null && !"".equals(mSecretToken)) {
      params.put(PARAM_SECRET_TOKEN, mSecretToken);
    }
    return params;
  }
}
