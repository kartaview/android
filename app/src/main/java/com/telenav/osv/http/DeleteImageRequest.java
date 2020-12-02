package com.telenav.osv.http;

import com.telenav.osv.listener.network.GenericResponseListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("HardCodedStringLiteral")
class DeleteImageRequest extends KvVolleyStringRequestJarvisAuthorization {

    private static final String PARAM_IMAGE_ID = "photoId";

    private static final String PARAM_TOKEN = "access_token";

    private final GenericResponseListener mListener;

    private final String mToken;

    private int mImageId;

    public DeleteImageRequest(String url, GenericResponseListener listener, int imageId, String token, boolean isJarvisAuthorization, String jarvisAccessToken) {
        super(Method.POST, url, listener, listener, isJarvisAuthorization, jarvisAccessToken);
        mToken = token;
        mImageId = imageId;
        mListener = listener;
    }

    @Override
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_IMAGE_ID, mImageId + "");
        params.put(PARAM_TOKEN, mToken);
        return params;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }
}