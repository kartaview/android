package com.telenav.osv.common.model.base;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Base class for all the interactors which provides the default settings for each server request.
 */
public abstract class BaseInteractorImpl {

    /**
     * The server options used for performing the request.
     */
    private static final String[] URL_ENV =
            {"api.openstreetcam.org/", "staging-api.openstreetcam.org/", "testing-api.openstreetcam.org/", "beta-api.openstreetcam.org/", "api.private.openstreetcam.org/"};

    private static final String BODY_PARAM_ACCESS_TOKEN = "access_token";

    private static final String HEADER_PARAM_ACCEPT = "Accept";

    private static final String HEADER_PARAM_ACCEPT_VALUE = "application/json";

    /**
     * Server version number, which should be updated constantly when the backend changes.
     */
    private static final String URL_VER = "1.0/";

    /**
     * The request protocol.
     */
    private static final String PROTOCOL = "https://";

    /**
     * Instance of the {@code Retrofit} which adapts a provided interface to a HTTP call.
     */
    protected Retrofit retrofit;

    /**
     * Constructor of the current class
     * @param currentServer the position of the server in the {@link #URL_ENV}
     * @param token the user token used to perform a request
     */
    public BaseInteractorImpl(int currentServer, String token) {
        retrofit = new Retrofit.Builder()
                .baseUrl(PROTOCOL + URL_ENV[currentServer] + URL_VER)
                .client(createOkHttpClient(token))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    /**
     * Creates an HTTP client which holds all the common request settings.
     * @return the instance of the {@code OkHttpClient}.
     */
    private OkHttpClient createOkHttpClient(String token) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.addInterceptor(chain -> {
            RequestBody requestBody = new FormBody.Builder()
                    .add(BODY_PARAM_ACCESS_TOKEN, token)
                    .build();
            Request request = chain.request();
            request = request.newBuilder().build();
            request = request.newBuilder().addHeader(HEADER_PARAM_ACCEPT, HEADER_PARAM_ACCEPT_VALUE)
                    .post(requestBody)
                    .build();
            return chain.proceed(request);
        });
        return client.build();
    }
}