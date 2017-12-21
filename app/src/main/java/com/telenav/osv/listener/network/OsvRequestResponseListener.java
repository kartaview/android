package com.telenav.osv.listener.network;


import com.android.volley.VolleyError;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.manager.network.parser.ApiResponseParser;
import com.telenav.osv.utils.Log;

/**
 * Combined response listener which auto parser status messages
 * Created by kalmanb on 8/3/17.
 */
public abstract class OsvRequestResponseListener<T extends ApiResponseParser<G>, G extends ApiResponse> extends GenericResponseListener {

    private static final String TAG = "OsvRequestResponseListener";

    private final T parser;

    public OsvRequestResponseListener(T parser) {
        this.parser = parser;
    }

    @Override
    public void onErrorResponse(final VolleyError error) {
        Log.d(TAG, "Volley error on request. Error: " + Log.getStackTraceString(error));
        G g = parser.parse(error);
        onFailure(g.getHttpCode(), g);
    }

    @Override
    public void onResponse(String s) {
        G g = parser.parse(s);
        onSuccess(g.getHttpCode(), g);
    }

    public abstract void onSuccess(final int status, final G g);

    public abstract void onFailure(final int status, final G g);
}
