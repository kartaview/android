package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.telenav.osv.listener.network.GenericResponseListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class NearbyRequest extends KvVolleyStringRequestJarvisAuthorization {

    private static final String PARAM_LAT = "lat";

    private static final String PARAM_LON = "lng";

    private static final String PARAM_RADIUS = "distance";

    private final GenericResponseListener mListener;

    private final String mLat;

    private final String mLon;

    private final String mRadius;

    public NearbyRequest(String url, GenericResponseListener listener, String lat, String lon, int radius, boolean isJarvisAuthorization, String jarvisAccessToken) {
        super(Method.POST, url, listener, listener, isJarvisAuthorization, jarvisAccessToken);
        mListener = listener;
        mLat = lat;
        mLon = lon;
        mRadius = "" + radius;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = super.getParams();
        if (params == null || params.equals(Collections.emptyMap())) {
            params = new HashMap<>();
        }
        params.put(PARAM_LAT, mLat);
        params.put(PARAM_LON, mLon);
        params.put(PARAM_RADIUS, mRadius);
        params.put("radius", mRadius);

        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}