package com.telenav.osv.upload.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.network.KVApi;
import com.telenav.osv.network.model.complete.ResponseModelUploadSequenceComplete;
import com.telenav.osv.network.model.generic.ResponseNetworkBase;
import com.telenav.osv.network.util.NetworkRequestConverter;
import com.telenav.osv.utils.Log;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import okhttp3.RequestBody;

/**
 * The operation which will finish a sequence upload on the network.
 * <p> This will generate a stream via {@link #getStream()} method which is the only public entry point to this operation.
 * @author horatiuf
 * @see UploadOperationBase
 * @see #getStream()
 */
public class UploadOperationSequenceComplete extends UploadOperationBase {

    /**
     * Identifier for the current class.
     */
    private static final String TAG = UploadOperationSequenceComplete.class.getSimpleName();

    /**
     * The online sequence identifier used for upload sequence complete.
     */
    private long onlineSequenceId;

    /**
     * Action which represent specific handling cases to be performed on success behaviour.
     */
    private Action actionSuccess;

    /**
     * Default constructor for the current class.
     */
    UploadOperationSequenceComplete(@NonNull String accessToken,
                                    @NonNull KVApi api,
                                    long onlineSequenceId,
                                    @NonNull SimpleEventBus eventBus,
                                    @Nullable Action actionSuccess,
                                    @Nullable Consumer<Throwable> consumerError) {
        super(accessToken, api, eventBus, consumerError);
        this.onlineSequenceId = onlineSequenceId;
        this.actionSuccess = actionSuccess;
    }

    /**
     * @return {@code Completable} composed of:
     * <ul>
     * <li> network call by using internally {@link #uploadCompleteSequenceStream()}</li>
     * <li> success handle by using internally {@link #handleUploadSequenceCompleteSuccessResponse()}</li>
     * <li> error logging and handling </li>
     * </ul>
     */
    public Completable getStream() {
        return uploadCompleteSequenceStream()
                .flatMapCompletable(response -> handleDefaultResponse(response, handleUploadSequenceCompleteSuccessResponse(), null))
                .retryWhen(this::handleDefaultRetryFlowableWithTimer)
                .doOnError(throwable -> Log.d(TAG, String.format("getStream. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    @Override
    public void dispose() {

    }

    /**
     * @return Completable which will return the network call to the {@link KVApi#sequenceComplete(RequestBody, RequestBody)}.
     */
    private Single<ResponseModelUploadSequenceComplete> uploadCompleteSequenceStream() {
        return api
                .sequenceComplete(
                        NetworkRequestConverter.generateTextRequestBody(accessToken),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(onlineSequenceId)));
    }

    /**
     * Process the response success for sequence complete.
     */
    private Consumer<ResponseNetworkBase> handleUploadSequenceCompleteSuccessResponse() {
        return (response) -> {
            if (actionSuccess != null) {
                try {
                    actionSuccess.run();
                } catch (Exception e) {
                    Log.d(TAG, String.format("handleUploadSequenceCompleteSuccessResponse. Status: error. Message: %s", e.getLocalizedMessage()));
                    return;
                }
                Log.d(TAG, "handleUploadSequenceCompleteSuccessResponse. Status: true. Message: Sequence complete successful.");
            }
        };
    }
}
