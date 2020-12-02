package com.telenav.osv.network.request.interceptor;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Base interceptor class for network which will require a concrete implementation for internet availability via {@link #isInternetAvailable()} and a handle callback concrete
 * implementation requirement via {@link #onInternetUnavailable()}.
 * <p> This will override {@link #intercept(Chain)} in order to put the logic for internet check here based on concrete interceptor logic.
 */
public abstract class NetworkConnectionInterceptor implements Interceptor {

    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        //checks for internet availability
        if (!isInternetAvailable()) {
            //call the callback for unavailability handle
            onInternetUnavailable();
            throw getInternetException();
        }
        Request.Builder requestBuilder = chain.request().newBuilder();
        interceptRequest(requestBuilder);
        return chain.proceed(requestBuilder.build());
    }

    /**
     * This will be called for intercepting a request with the builder as a parameter if any changes needs to be done to the request, e.g. adding a header.
     * @param requestBuilder the request builder which will be passed as a param for changes.
     */
    public abstract void interceptRequest(Request.Builder requestBuilder);

    /**
     * @return {@code true} if the internet is available, otherwise {@code false}.
     * <p> The implementation should provide logic in order to match with the return description.
     */
    public abstract boolean isInternetAvailable();

    /**
     * Internet unavailable callback.
     * <p> The implementation should provide logic which handles internet unavailability.
     */
    public abstract void onInternetUnavailable();

    /**
     * Internet unavailable exception.
     * <p> This will be thrown without any requirement to call the network request.
     */
    public abstract IOException getInternetException();
}
