package com.telenav.osv.network.request.interceptor;

import java.io.IOException;
import android.content.Context;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.common.listener.ListenerDefault;
import com.telenav.osv.jarvis.login.utils.LoginUtils;
import com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.disposables.Disposable;
import okhttp3.Request;
import static com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers.HEADER_AUTHORIZATION;
import static com.telenav.osv.network.response.NetworkRequestHeaderIdentifiers.HEADER_AUTH_TYPE;

/**
 * Upload interceptor class which handles functionality for catching and adding request based on specific logic.
 */
public class UploadInterceptor extends NetworkConnectionInterceptor {

    /**
     * Identifier for the current class for logging purposes.
     */
    private static final String TAG = UploadInterceptor.class.getSimpleName();

    /**
     * Value for accept header which will accept application/json.
     */
    private static final String HEADER_PARAM_ACCEPT_VALUE = "application/json";

    /**
     * Flag which denotes if the internet is available or not.
     */
    private boolean isInternetAvailable;

    /**
     * Disposable used for network availability.
     */
    private Disposable networkDisposable;

    /**
     * Listener which will be signaled once internet is unavailable.
     */
    @Nullable
    private ListenerDefault internetUnavailableListener;

    /**
     * {@code ApplicationPreferences} reference to see what settings
     */
    private ApplicationPreferences applicationPreferences;

    /**
     * Reference to the context which will be used to check the availability of internet to be valid to the app settings.
     */
    private Context context;

    /**
     * Default constructor for the current class.
     */
    public UploadInterceptor(@NonNull Context context,
                             @NonNull ApplicationPreferences applicationPreferences,
                             @NonNull SimpleEventBus simpleEventBus,
                             @Nullable ListenerDefault internetUnavailableListener) {
        this(context, applicationPreferences, simpleEventBus);
        this.internetUnavailableListener = internetUnavailableListener;
    }

    /**
     * Custom constructor without any nullable fields.
     */
    public UploadInterceptor(@NonNull Context context,
                             @NonNull ApplicationPreferences applicationPreferences,
                             @NonNull SimpleEventBus simpleEventBus) {
        this.context = context;
        this.applicationPreferences = applicationPreferences;
        networkDisposable = simpleEventBus
                .filteredObservable(Boolean.class)
                .subscribe(
                        //onSuccess
                        isInternetAvailable -> this.isInternetAvailable = isInternetAvailable,
                        //error
                        throwable -> Log.d(TAG, String.format("constructor. Status: error on internet. Message: %s.", throwable.getMessage())));
    }

    @Override
    public boolean isInternetAvailable() {
        return NetworkUtils.
                isInternetConnectionAvailable(context, applicationPreferences.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED))
                && isInternetAvailable;
    }

    @Override
    public void onInternetUnavailable() {
        if (internetUnavailableListener != null) {
            internetUnavailableListener.onCallback();
        }
    }

    @Override
    public IOException getInternetException() {
        return new NoInternetException();
    }

    @Override
    public void interceptRequest(Request.Builder requestBuilder) {
        if (LoginUtils.isLoginTypePartner(applicationPreferences)) {
            requestBuilder.addHeader(HEADER_AUTH_TYPE, "token");
            requestBuilder.addHeader(HEADER_AUTHORIZATION, applicationPreferences.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
        }
        requestBuilder
                //adding header in order to accept application/json
                .addHeader(NetworkRequestHeaderIdentifiers.HEADER_ACCEPT, HEADER_PARAM_ACCEPT_VALUE);
    }

    /**
     * Clean-up method which releases specific resources.
     */
    public void dispose() {
        if (networkDisposable != null && !networkDisposable.isDisposed()) {
            networkDisposable.dispose();
        }
    }

    /**
     * Custom exception class for when the internet is not available in the case of upload.
     */
    public class NoInternetException extends IOException {

        /**
         * Message for when the internet is not available for upload.
         */
        public static final String EXCEPTION_MESSAGE_UPLOAD_NO_INTERNET_AVAILABLE = "No internet available for upload.";

        @Override
        public String getMessage() {
            return EXCEPTION_MESSAGE_UPLOAD_NO_INTERNET_AVAILABLE;
        }
    }
}
