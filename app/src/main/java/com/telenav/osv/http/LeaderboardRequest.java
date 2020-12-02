package com.telenav.osv.http;

import com.android.volley.AuthFailureError;
import com.telenav.osv.listener.network.GenericResponseListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kalman on 22/11/2016.
 */
public class LeaderboardRequest extends KvVolleyStringRequestJarvisAuthorization {

    private static final String PARAM_DATE = "fromDate";

    private static final String PARAM_COUNTRY_CODE = "countryCode";

    private static final String PARAM_STATECODE = "stateCode";

    private final String date;

    private final String countryCode;

    private final String stateCode;

    public LeaderboardRequest(String url, GenericResponseListener listener, String date, String countryCode, String stateCode, boolean isJarvisAuthorization, String jarvisAccessToken) {
        super(Method.POST, url, listener, listener, isJarvisAuthorization, jarvisAccessToken);
        this.date = date;
        this.countryCode = countryCode;
        this.stateCode = stateCode;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = super.getParams();
        if (params == null || params.equals(Collections.emptyMap())) {
            params = new HashMap<>();
        }
        if (date != null) {
            params.put(PARAM_DATE, date);
        }
        if (countryCode != null) {
            params.put(PARAM_COUNTRY_CODE, countryCode);
        }
        if (stateCode != null) {
            params.put(PARAM_STATECODE, stateCode);
        }
        return params;
    }
}
