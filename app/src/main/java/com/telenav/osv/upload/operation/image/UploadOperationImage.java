package com.telenav.osv.upload.operation.image;

import android.location.Location;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.network.OscApi;
import com.telenav.osv.network.util.NetworkRequestConverter;
import com.telenav.osv.upload.operation.UploadOperationBase;
import com.telenav.osv.upload.progress.model.UploadUpdateDisk;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.functions.Action;

/**
 * @author horatiuf
 */

public class UploadOperationImage extends UploadOperationBase {

    /**
     * Throwable message to inform that the image file was not found.
     */
    public static final String THROWABLE_MESSAGE_IMAGE_FILE_NOT_FOUND = "Image file not found.";

    /**
     * Throwable message to inform that the location was not found.
     */
    public static final String THROWABLE_MESSAGE_LOCATION_NOT_FOUND = "Location not found.";

    /**
     * Current identifier for the class.
     */
    private static final String TAG = UploadOperationImage.class.getSimpleName();

    /**
     * The name used for encoding a {@code Frame} file in a request body of type multipart body.
     */
    private static final String IMAGE_NAME_MULTI_PART_BODY = "photo";

    /**
     * The frame identifier use to get and process frame data before network request.
     */
    private String frameId;

    /**
     * Reference to the physical image file,
     */
    private OSVFile imageFile;

    /**
     * The data source for video in order to manipulate the video information.
     * @see FrameLocalDataSource
     */
    private FrameLocalDataSource frameLocalDataSource;

    /**
     * Action which represent specific handling cases to be performed on success behaviour.
     */
    private Action actionSuccess;

    /**
     * The online sequence identifier used for upload video operation.
     */
    private long onlineSequenceId;

    /**
     * Default constructor for the current class.
     */
    public UploadOperationImage(@NonNull String accessToken,
                                @NonNull OscApi api,
                                @NonNull FrameLocalDataSource frameLocalDataSource,
                                @NonNull String frameId,
                                long onlineSequenceId,
                                @NonNull SimpleEventBus eventBus,
                                @Nullable Action actionSuccess,
                                @Nullable Consumer<Throwable> consumerError) {
        super(accessToken, api, eventBus, consumerError);
        this.frameId = frameId;
        this.onlineSequenceId = onlineSequenceId;
        this.actionSuccess = actionSuccess;
        this.frameLocalDataSource = frameLocalDataSource;
    }

    @Override
    public void dispose() {

    }

    /**
     * @return {@code Completable} composed of:
     * <ul>
     * <li> frame local data source video persistence get</li>
     * <li> network call process by using internally {@link #uploadImageStream(Frame)}</li>
     * <li> error logging</li>
     * </ul>
     */
    public Completable getStream() {
        return frameLocalDataSource
                .getFrameWithLocation(frameId)
                .filter(item -> item != null)
                .flatMapCompletable(this::uploadImageStream)
                .doOnError(throwable -> Log.d(TAG, String.format("getStream. Status: error. Frame id: %s. Message: %s.", frameId, throwable.getLocalizedMessage())));
    }

    /**
     * Process the success response by:
     * <ul>
     * <li>
     * remove frame from persistence
     * </li>
     * <li>
     * updating information related to sequence, i.e. frames counts.
     * </li>
     * <li>
     * updating the disk size for the sequence
     * </li>
     * <li>
     * removal of the image file on the disk
     * </li>
     * </ul>
     */
    private void handleUploadImageSuccessResponse() {
        boolean deleteImageFromPersistence = frameLocalDataSource.deleteFrame(frameId);
        dispose();
        if (deleteImageFromPersistence) {
            if (imageFile.exists()) {
                long imageSize = Utils.fileSize(imageFile);
                boolean imageFileRemove = imageFile.delete();
                Log.d(TAG, String.format("handleUploadImageSuccessResponse. Status :%s. Message: Attempting remove image from persistence. Size: %s.", imageFileRemove, imageSize));
                if (imageFileRemove) {
                    //signal updates for disk size
                    updateEventBus.post(new UploadUpdateDisk(imageSize));
                    //archive the current child for the parent.
                    UploadUpdateProgress uploadUpdateProgress = new UploadUpdateProgress(imageSize, imageSize);
                    uploadUpdateProgress.archive();
                    updateEventBus.post(uploadUpdateProgress);
                    //call the action success to signal success response.
                    if (actionSuccess != null) {
                        try {
                            actionSuccess.run();
                        } catch (Exception e) {
                            Log.d(TAG, String.format("handleUploadImageSuccessResponse. Status: error. Message: %s", e.getLocalizedMessage()));
                        }
                    }
                }
            }
        }
        imageFile = null;
    }

    /**
     * @param frame the frame which will be persisted until this operation ends due to post-network success processing.
     * @return {@code Completable} with the API repose of the upload image network request.
     */
    private Completable uploadImageStream(Frame frame) {
        imageFile = new OSVFile(frame.getFilePath());
        if (imageFile.exists()) {
            Location location = frame.getLocation();
            if (location == null) {
                Throwable throwable = new Throwable(THROWABLE_MESSAGE_LOCATION_NOT_FOUND);
                if (consumerError != null) {
                    consumerError.accept(throwable);
                }
                return Completable.error(throwable);
            }
            return getUploadImageRequestCompletable(frame.getIndex(), location);
        } else {
            Throwable throwable = new Throwable(THROWABLE_MESSAGE_IMAGE_FILE_NOT_FOUND);
            if (consumerError != null) {
                consumerError.accept(throwable);
            }
            return Completable.error(throwable);
        }
    }

    private Completable getUploadImageRequestCompletable(int frameIndex, Location location) {
        return api
                .uploadImage(
                        NetworkRequestConverter.generateTextRequestBody(accessToken),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(onlineSequenceId)),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(frameIndex)),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(location.getAccuracy())),
                        NetworkRequestConverter.generateTextRequestBody(getLocationAsString(location)),
                        NetworkRequestConverter.generateMultipartBodyPart(NetworkRequestConverter.REQUEST_MEDIA_TYPE_IMAGE, IMAGE_NAME_MULTI_PART_BODY, imageFile, null))
                .flatMapCompletable(response -> {
                    if (handleResponse(response)) {
                        handleUploadImageSuccessResponse();
                        return Completable.complete();
                    }

                    return Completable.error(new Throwable(String.valueOf(response.getStatus().code)));
                })
                //catch any error which bypass the response check, used mostly for duplicate error check which means that the data was already upload previous, hence the
                // success signal, otherwise propagate the error forward
                .onErrorComplete(error -> {
                    if (handleResponse(error)) {
                        handleUploadImageSuccessResponse();
                        return true;
                    }
                    return false;
                });
    }
}
