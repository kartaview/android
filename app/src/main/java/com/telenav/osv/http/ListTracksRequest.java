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
@SuppressWarnings("HardCodedStringLiteral")
public class ListTracksRequest extends StringRequest {

  private static final String PARAM_BB_TOP_LEFT = "bbTopLeft";

  private static final String PARAM_BB_BOTTOM_RIGHT = "bbBottomRight";

  private static final String PARAM_IPP = "ipp";

  private static final String PARAM_PAGE = "page";

  private static final String PARAM_ZOOM = "zoom";

  private final String mBbTopLeft;

  private final String mBbBottomRight;

  private final GenericResponseListener mListener;

  private final float mZoom;

  private final int mIPP;

  private final int mPage;

  public ListTracksRequest(String url, GenericResponseListener listener, String bbTopLeft, String bbBottomRight, int page, int ipp,
                           float zoom) {
    super(Method.POST, url, listener, listener);
    mListener = listener;
    mBbTopLeft = bbTopLeft;
    mBbBottomRight = bbBottomRight;
    mZoom = zoom;
    mIPP = ipp;
    mPage = page;
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
    params.put(PARAM_BB_TOP_LEFT, mBbTopLeft);
    params.put(PARAM_BB_BOTTOM_RIGHT, mBbBottomRight);
    params.put(PARAM_IPP, "" + mIPP);
    params.put(PARAM_PAGE, "" + mPage);
    params.put(PARAM_ZOOM, "" + mZoom);

    return params;
  }

  @Override
  protected void deliverResponse(String response) {
    mListener.onResponse(response);
  }
}