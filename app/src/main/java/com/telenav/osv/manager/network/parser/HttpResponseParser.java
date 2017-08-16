package com.telenav.osv.manager.network.parser;


import com.telenav.osv.item.network.ApiResponse;

/**
 * Created by kalmanb on 8/3/17.
 */
public class HttpResponseParser extends ApiResponseParser<ApiResponse> {
    private static final String TAG = "HttpResponseParser";

    @Override
    public ApiResponse getHolder() {
        return new ApiResponse();
    }
}
