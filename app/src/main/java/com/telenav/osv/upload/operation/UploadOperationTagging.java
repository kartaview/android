package com.telenav.osv.upload.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.network.KVApi;
import com.telenav.osv.network.util.NetworkRequestConverter;
import com.telenav.osv.recorder.tagging.converter.GeoJsonConverter;
import com.telenav.osv.upload.progress.model.UploadUpdateDisk;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import io.reactivex.Completable;
import okhttp3.MultipartBody;

public class UploadOperationTagging extends UploadOperationBase {

    private static final String TAG = UploadOperationTagging.class.getSimpleName();

    /**
     * The name used for encoding a {@code Sequence} tagging file in a request body of type multipart body.
     */
    private static final String TAGGING_NAME_MULTI_PART_BODY = "file";

    /**
     * The request parameter which represents the type of the API v2 for uploading file.
     */
    private static final String REQUEST_PARAM_DATA_TYPE = "TAGGED_ROADS";

    /**
     * The request parameter which represents the type of the API v2 for uploading file.
     */
    private static final String REQUEST_PARAM_SEQUENCE_INDEX = "0";

    /**
     * The sequence folder which will be used for metadata file search.
     */
    private KVFile sequenceFolder;

    /**
     * Reference to the tagging file used in both pre and post request processing.
     */
    private KVFile taggingFile;

    /**
     * The online sequence identifier used for upload video operation.
     */
    private long onlineSequenceId;

    /**
     * Default constructor for the current class.
     */
    public UploadOperationTagging(
            @NonNull KVFile sequenceFolder,
            long onlineSequenceId,
            @NonNull String accessToken,
            @NonNull KVApi api,
            @NonNull SimpleEventBus updateEventBus,
            @Nullable Consumer<Throwable> consumerError) {
        super(accessToken, api, updateEventBus, consumerError);
        this.sequenceFolder = sequenceFolder;
        this.onlineSequenceId = onlineSequenceId;
    }

    @Override
    public void dispose() {

    }

    @Override
    public Completable getStream() {
        return Completable.defer(this::uploadTaggingStream)
                .doOnError(throwable -> Log.d(TAG, String.format("getStream. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    private Completable uploadTaggingStream() {
        //loads the file for tagging which is in geoJson format
        loadTaggingFileIntoMemory();
        if (taggingFile == null || !taggingFile.exists()) {
            taggingFile = null;
            return Completable.complete();
        }
        MultipartBody.Part taggingMultiPartBody =
                NetworkRequestConverter.generateMultipartBodyPart(
                        NetworkRequestConverter.REQUEST_MEDIA_TYPE_JSON,
                        TAGGING_NAME_MULTI_PART_BODY,
                        taggingFile,
                        null);
        return requestTaggingFileUploadNetworkCompletable(taggingMultiPartBody);
    }

    private Completable requestTaggingFileUploadNetworkCompletable(MultipartBody.Part taggingPart) {
        return api
                .uploadTagging(
                        NetworkRequestConverter.generateTextRequestBody(accessToken),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(onlineSequenceId)),
                        NetworkRequestConverter.generateTextRequestBody(REQUEST_PARAM_DATA_TYPE),
                        NetworkRequestConverter.generateTextRequestBody(REQUEST_PARAM_SEQUENCE_INDEX),
                        taggingPart)
                .flatMapCompletable(response -> handleDefaultResponse(response, (networkResponse) -> removeTaggingFileIfExists(), null))
                //catch any error which bypass the response check, used mostly for duplicate error check which means that the data was already upload previous, hence the
                // success signal, otherwise propagate the error forward
                .onErrorComplete(error -> {
                    if (handleResponse(error)) {
                        removeTaggingFileIfExists();
                        return true;
                    }
                    return false;
                })
                .retryWhen(this::handleDefaultRetryFlowableWithTimer);
    }

    /**
     * Loads the zip or the txt file of the metadata into memory in case it is not already
     */
    private void loadTaggingFileIntoMemory() {
        if (taggingFile != null && taggingFile.exists()) {
            return;
        }
        taggingFile = new KVFile(sequenceFolder, GeoJsonConverter.RECORDING_TAGGING_FILE_NAME);
    }

    /**
     * Remove the tagging file in exists. This will be performed before a {@link Completable#complete()} manual callback.
     * <p> The method will internally call progress updates with the size removed from the disk in case the remove of the file is successful.</p>
     */
    private void removeTaggingFileIfExists() {
        //loads the tagging file into the memory
        loadTaggingFileIntoMemory();
        //if tagging file exists remove and send a progress and disk update
        if (taggingFile.exists()) {
            long taggingSize = Utils.fileSize(taggingFile);
            boolean taggingFileRemove = taggingFile.delete();
            Log.d(TAG, String.format("removeTaggingFileIfExists. Status : %s. Message: Remove physical tagging file from the device. Size: %s.", taggingFileRemove, taggingSize));
            updateEventBus.post(new UploadUpdateDisk(taggingSize));
            updateEventBus.post(new UploadUpdateProgress(taggingSize, taggingSize));
        }
        taggingFile = null;
    }
}
