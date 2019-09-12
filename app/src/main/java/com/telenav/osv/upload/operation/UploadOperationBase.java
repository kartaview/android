package com.telenav.osv.upload.operation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Publisher;
import android.location.Location;
import com.google.gson.Gson;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.common.network.NetworkConstants;
import com.telenav.osv.network.OscApi;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;
import com.telenav.osv.network.request.interceptor.UploadInterceptor;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;

/**
 * Base class for the upload operation. This will contain only logic/fields common for <b>all</b> operations.
 * <p>Common fields:
 * <ul>
 * <li>{@link #accessToken}</li>
 * <li>{@link #api}</li>
 * </ul>
 * <p>Common methods:
 * <ul>
 * <li>{@link #dispose()}</li>
 * </ul>
 * @author horatiuf
 */

public abstract class UploadOperationBase {

    /**
     * The pattern for location.
     */
    public static final String PATTERN_LOCATION = "%s,%s";

    public static final int DEFAULT_NO_INTERNET_DELAY_IN_SECONDS = 3;

    /**
     * Specify for the delay merge operations number in order for the operation to be serial, one processed at a time.
     * <p> Note that this refers to processing and not networking.
     */
    protected static final int MERGE_DELAY_ERROR_NO_SERIAL = 1;

    /**
     * Specify for the delay merge operations number in order for the operation to be concurrent, multiple processed at a time.
     * <p> Note that this refers to processing and not networking.
     */
    protected static final int MERGE_DELAY_ERROR_CONCURRENT_NO = 5;

    /**
     * The identifier for the current class in logs.
     */
    private static final String TAG = UploadOperationBase.class.getSimpleName();

    /**
     * The limit of retries for network.
     */
    protected int RETRIES_LIMIT = 3;

    /**
     * The access token used in order to identify the network request
     */
    protected String accessToken;

    /**
     * Reference to the api which will be used for all operations in order to do network operations.
     */
    protected OscApi api;

    /**
     * Action which represent specific handling cases to be performed on error behaviour.
     */
    protected Consumer<Throwable> consumerError;

    /**
     * The event buss which will signal response updates from the operation.
     * @see SimpleEventBus
     */
    protected SimpleEventBus updateEventBus;

    /**
     * Default constructor for the current class.
     */
    public UploadOperationBase(@NonNull String accessToken,
                               @NonNull OscApi api,
                               @NonNull SimpleEventBus updateEventBus,
                               @Nullable Consumer<Throwable> consumerError) {
        this.accessToken = accessToken;
        this.api = api;
        this.updateEventBus = updateEventBus;
        this.consumerError = consumerError;
    }

    /**
     * Constructor for the current class without any nullable fields.
     */
    UploadOperationBase(@NonNull String accessToken,
                        @NonNull OscApi api,
                        @NonNull SimpleEventBus updateEventBus) {
        this(accessToken, api, updateEventBus, null);
    }

    /**
     * @param errors the errors received by the network request.
     * @return {@code Flowable} which will retry all logic for {@link #RETRIES_LIMIT} after will signal the error.
     */
    public Flowable handleDefaultRetryFlowableWithTimer(Flowable<Throwable> errors) {
        AtomicInteger atomicInteger = new AtomicInteger();
        return errors.flatMap((Function<Throwable, Publisher<?>>) throwable -> {
            if (throwable instanceof UploadInterceptor.NoInternetException) {
                Log.d(TAG, "handleDefaultRetryFlowableWithTimer. Status: error no internet. Message: Infinite retry engaged.");
                return Flowable.timer(DEFAULT_NO_INTERNET_DELAY_IN_SECONDS, TimeUnit.SECONDS);
            }
            if (atomicInteger.getAndIncrement() != RETRIES_LIMIT) {
                Log.d(TAG, String.format("handleDefaultRetryFlowableWithTimer. Status: error retry. Counter: %s.", atomicInteger.get()));
                return Flowable.timer(atomicInteger.get(), TimeUnit.SECONDS);
            }

            return Flowable.error(throwable);
        });
    }

    /**
     * @param throwable the successful response to be processed.
     * @return {@code true} if the response is valid, {@code false} otherwise.
     */
    public boolean handleResponse(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            try {
                //this is done directly due to error body cannot be read twice since after the first time the stream of charsets will get closed
                //see https://github.com/square/retrofit/issues/1321#issuecomment-251160231
                String errorBodyJson = httpException.response().errorBody().string();
                ResponseNetworkBase status = new Gson().fromJson(errorBodyJson, ResponseNetworkBase.class);
                return handleResponse(status);
            } catch (IOException e) {
                Log.d(TAG, String.format("error exception. Status: error. Message: %s.", e.getLocalizedMessage()));
                return false;
            }
        }
        return false;
    }

    /**
     * Cleanup method will be called at the end of the operation.
     */
    public abstract void dispose();

    /**
     * @return the Completable stream which will contain the functionality for the concrete implementation operation logic.
     */
    public abstract Completable getStream();

    /**
     * @param response the response from the network request.
     * @param successConsumer the success consumer to be called on success.
     * @param handleError the error consumer to be called on error handle.
     * @return Completable which are either the success or error. The success will be validated internally by {@link #handleResponse(ResponseNetworkBase)}.
     * <p> The error handle is recommended for operations which do not require specific handle since the throwable will contain the message and not the api code.
     */
    Completable handleDefaultResponse(ResponseNetworkBase response, @NonNull Consumer<ResponseNetworkBase> successConsumer, @Nullable Consumer<Throwable> handleError) {
        if (handleResponse(response)) {
            successConsumer.accept(response);
            return Completable.complete();
        }
        Throwable throwable = new Throwable(response.getStatus().message);
        if (consumerError != null) {
            consumerError.accept(throwable);
        }
        return Completable.error(throwable);
    }

    /**
     * @param responseNetworkBase the successful response to be processed.
     * @return {@code true} if the response is valid, {@code false} otherwise.
     */
    protected boolean handleResponse(ResponseNetworkBase responseNetworkBase) {
        if (responseNetworkBase == null) {
            return false;
        }
        return isResponseValid(responseNetworkBase.getStatus().code);
    }

    /**
     * @param location the initial location of the sequence.
     * @return the initial location of the sequence in the network request format.
     */
    protected String getLocationAsString(Location location) {
        return String.format(PATTERN_LOCATION, location.getLatitude(), location.getLongitude());
    }

    /**
     * @param apiCode the api code which will be evaluated for validity of response.
     * @return {@code true} when the given parameter is either signaling success or duplicate key, {@code false} otherwise.
     * <p> The duplicate key api code is seen as 'success' since the data is already uploaded on the server with success response therefore the response was not previously
     * processed correctly, either due a network crash or a manual/automatic stop of the upload feature.
     */
    private boolean isResponseValid(@NetworkConstants.NetworkErrorApiCode int apiCode) {
        return NetworkConstants.NETWORK_ERROR_API_V1_CODE_DUPLICATE_KEY == apiCode
                || NetworkConstants.NETWORK_SUCCESS_API_V1_CODE_SUCCESS == apiCode
                || NetworkConstants.NETWORK_ERROR_API_V2_CODE_DUPLICATE_KEY == apiCode;
    }
}
