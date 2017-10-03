package com.telenav.osv.manager.network.parser;

import org.json.JSONObject;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.utils.Log;

/**
 * Generic api response parser that parses http response code and message
 * Created by kalmanb on 8/3/17.
 */
public abstract class ApiResponseParser<T extends ApiResponse> {

    private static final String TAG = "ApiResponseParser";

    private static final int HTTP_TIMEOUT = 409;

    private static final int HTTP_FORBIDDEN = 403;

    private static final String STATUS_KEY = "status";

    public abstract T getHolder();

    public T parse(VolleyError error) {
        T response = getHolder();
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String result = new String(error.networkResponse.data);
                JSONObject ob = new JSONObject(result);
                int httpCode = ob.getJSONObject(STATUS_KEY).getInt("httpCode");
                String httpMessage = ob.getJSONObject(STATUS_KEY).getString("httpMessage");
                int apiCode = ob.getJSONObject(STATUS_KEY).getInt("apiCode");
                String apiMessage = ob.getJSONObject(STATUS_KEY).getString("apiMessage");
                response.setApiCode(apiCode);
                response.setApiMessage(apiMessage);
                response.setHttpCode(httpCode);
                response.setHttpMessage(httpMessage);
            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }
        } else if (error instanceof TimeoutError) {
            response.setHttpCode(HTTP_TIMEOUT);
            response.setHttpMessage("Timeout error");
        } else {
            response.setHttpMessage(error.getMessage());
            response.setHttpCode(HTTP_FORBIDDEN);
        }
        return response;
    }

    public T parse(String json) {

        T response = getHolder();
        try {
            JSONObject ob = new JSONObject(json);
            int httpCode = ob.getJSONObject(STATUS_KEY).getInt("httpCode");
            String httpMessage = ob.getJSONObject(STATUS_KEY).getString("httpMessage");
            int apiCode = ob.getJSONObject(STATUS_KEY).getInt("apiCode");
            String apiMessage = ob.getJSONObject(STATUS_KEY).getString("apiMessage");
            response.setApiCode(apiCode);
            response.setApiMessage(apiMessage);
            response.setHttpCode(httpCode);
            response.setHttpMessage(httpMessage);
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
        return response;
    }
}
