package com.telenav.osv.network.response;

import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.utils.Log;
import androidx.core.util.Pair;
import okhttp3.Interceptor;
import okhttp3.Response;

public class ProgressResponseInterceptor implements Interceptor {

    private final String TAG = ProgressResponseInterceptor.class.getSimpleName();

    private final SimpleEventBus eventBus;

    private Pair<String, String>[] headers;

    public ProgressResponseInterceptor(SimpleEventBus eventBus, Pair<String, String>... headers) {
        this.eventBus = eventBus;
        this.headers = headers;
    }

    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        Response.Builder builder = originalResponse.newBuilder();

        String progressResponseIdentifier = originalResponse.request().header(NetworkRequestHeaderIdentifiers.HEADER_REQUEST_IDENTIFIER);
        boolean fileIdentifierIsSet = progressResponseIdentifier != null && !progressResponseIdentifier.isEmpty();
        Log.d(TAG, String.format("intercept. Status: %s. Message: Progress response identifier set status.", fileIdentifierIsSet));

        if (fileIdentifierIsSet) {
            builder.body(
                    new ProgressResponseBody(
                            progressResponseIdentifier,
                            originalResponse.body(),
                            (identifier, bytesRead, done) -> {
                                // we post an event into the Bus !
                                Log.d(TAG, "progress response body. Status: posting update.");
                                eventBus.post(new ProgressEvent(identifier, bytesRead, done));
                            }));
        } else { // do nothing if it's not a file with an identifier :)
            builder.body(originalResponse.body());
        }

        return builder.build();
    }
}
