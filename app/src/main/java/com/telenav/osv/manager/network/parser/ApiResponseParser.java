package com.telenav.osv.manager.network.parser;


import org.json.JSONObject;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.telenav.osv.item.network.ApiResponse;

/**
 * Created by kalmanb on 8/3/17.
 */
public abstract class ApiResponseParser<T extends ApiResponse> {
    private static final String TAG = "ApiResponseParser";

    public abstract T getHolder();

    public T parse(VolleyError error) {
        T response = getHolder();
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
//                Log.w(TAG, "parse: " + new String(error.networkResponse.data));
                String result = new String(error.networkResponse.data);
                JSONObject ob = new JSONObject(result);
                int httpCode = ob.getJSONObject("status").getInt("httpCode");
                String httpMessage = ob.getJSONObject("status").getString("httpMessage");
                int apiCode = ob.getJSONObject("status").getInt("apiCode");
                String apiMessage = ob.getJSONObject("status").getString("apiMessage");
                response.setApiCode(apiCode);
                response.setApiMessage(apiMessage);
                response.setHttpCode(httpCode);
                response.setHttpMessage(httpMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (error instanceof TimeoutError) {
            response.setHttpCode(408);
            response.setHttpMessage("Timeout error");
        } else {
            response.setHttpMessage(error.getMessage());
            response.setHttpCode(403);
        }
        return response;
    }

    public T parse(String json) {

        T response = getHolder();
        try {
//                Log.w(TAG, "parse: " + json);
            JSONObject ob = new JSONObject(json);
            int httpCode = ob.getJSONObject("status").getInt("httpCode");
            String httpMessage = ob.getJSONObject("status").getString("httpMessage");
            int apiCode = ob.getJSONObject("status").getInt("apiCode");
            String apiMessage = ob.getJSONObject("status").getString("apiMessage");
            response.setApiCode(apiCode);
            response.setApiMessage(apiMessage);
            response.setHttpCode(httpCode);
            response.setHttpMessage(httpMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
