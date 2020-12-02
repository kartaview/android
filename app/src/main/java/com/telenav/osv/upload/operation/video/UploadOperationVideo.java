package com.telenav.osv.upload.operation.video;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.data.video.model.Video;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.network.KVApi;
import com.telenav.osv.network.request.ProgressRequestListener;
import com.telenav.osv.network.util.NetworkRequestConverter;
import com.telenav.osv.upload.operation.UploadOperationBase;
import com.telenav.osv.upload.progress.model.UploadUpdateDisk;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import io.reactivex.Completable;

/**
 * The operation which will upload a video file to the network.
 * <p> This will generate a stream via {@link #getStream()} method which is the only public entry point to this operation.
 * @author horatiuf
 * @see UploadOperationBase
 * @see #getStream()
 */
public class UploadOperationVideo extends UploadOperationBase implements ProgressRequestListener {

    /**
     * Throwable message to inform that the video file was not found.
     */
    public static final String THROWABLE_MESSAGE_VIDEO_FILE_NOT_FOUND = "Video file not found.";

    /**
     * Current identifier for the class.
     */
    private static final String TAG = UploadOperationVideo.class.getSimpleName();

    /**
     * The name used for encoding a {@code Video} file in a request body of type multipart body.
     */
    private static final String VIDEO_NAME_MULTI_PART_BODY = "video";

    /**
     * The video identifier use to get and process video data before network request.
     */
    private String videoId;

    /**
     * The data source for video in order to manipulate the video information.
     * @see SequenceLocalDataSource
     */
    private VideoLocalDataSource videoLocalDataSource;

    /**
     * Reference to the video model use in post-process success handle and updates of persistence.
     */
    private Video video;

    /**
     * Reference to the physical video file
     */
    private KVFile videoFile;

    /**
     * Action which represent specific handling cases to be performed on success behaviour.
     */
    private Consumer<Integer> consumerSuccess;

    /**
     * The online sequence identifier used for upload video operation.
     */
    private long onlineSequenceId;

    /**
     * The upload update progress for the video.
     */
    private UploadUpdateProgress uploadUpdateProgressVideo;

    /**
     * Default constructor for the current class.
     */
    public UploadOperationVideo(@NonNull String accessToken,
                                @NonNull KVApi api,
                                @NonNull VideoLocalDataSource videoLocalDataSource,
                                @NonNull String videoId,
                                long onlineSequenceId,
                                @NonNull SimpleEventBus eventBus,
                                @Nullable Consumer<Integer> consumerSuccess,
                                @Nullable Consumer<Throwable> consumerError) {
        super(accessToken, api, eventBus, consumerError);
        this.videoLocalDataSource = videoLocalDataSource;
        this.videoId = videoId;
        this.onlineSequenceId = onlineSequenceId;
        this.consumerSuccess = consumerSuccess;
    }

    /**
     * @return {@code Completable} composed of:
     * <ul>
     * <li> video local data source video persistence get</li>
     * <li> network call by using internally {@link #processUploadVideoCompletable(Video)}</li>
     * <li> error logging</li>
     * </ul>
     */
    public Completable getStream() {
        return videoLocalDataSource
                .getVideo(videoId)
                .filter(item -> item != null)
                .flatMapCompletable(this::processUploadVideoCompletable)
                .doOnError(throwable -> Log.d(TAG, String.format("getStream. Status: error. Video id: %s. Message: %s.", videoId, throwable.getLocalizedMessage())));
    }

    @Override
    public void dispose() {

    }

    @Override
    public void update(long bytesWritten, long contentLength) {
        long currentUnit = (long) (((double) bytesWritten / contentLength) * uploadUpdateProgressVideo.getTotalUnit());
        uploadUpdateProgressVideo.setCurrentUnit(currentUnit);
    }

    private void handleUploadVideoErrorResponse(Throwable throwable) {
        Log.d(TAG, String.format("handleUploadVideoErrorResponse. Status: error. Video id: %s. Message: %s.", videoId, throwable.getLocalizedMessage()));
        uploadUpdateProgressVideo.cancel();
        updateEventBus.post(uploadUpdateProgressVideo);
        videoFile = null;
    }

    /**
     * Process the success response by:
     * <ul>
     * <li>
     * remove video from persistence
     * </li>
     * <li>
     * updating information related to sequence, i.e. frames/videos counts.
     * </li>
     * <li>
     * updating the disk size for the sequence
     * </li>
     * <li>
     * removal of the video file on the disk
     * </li>
     * </ul>
     */
    private void handleUploadVideoSuccessResponse() {
        boolean deleteVideoFromPersistence = videoLocalDataSource.deleteVideo(videoId);
        dispose();
        long videoSize = Utils.fileSize(videoFile);
        Log.d(TAG,
                String.format("handleUploadVideoSuccessResponse. Status: %s. Message: Attempting remove video from persistence. Size: %s.", deleteVideoFromPersistence, videoSize));
        if (deleteVideoFromPersistence) {
            if (videoFile.exists()) {
                boolean videoFileRemove = videoFile.delete();
                if (videoFileRemove) {
                    //signal updates for disk size
                    updateEventBus.post(new UploadUpdateDisk(videoSize));
                    //archive the current child for the parent.
                    uploadUpdateProgressVideo.setCurrentUnit(videoSize);
                    uploadUpdateProgressVideo.archive();
                    updateEventBus.post(uploadUpdateProgressVideo);
                    //call the success consumer to signal success response.
                    if (consumerSuccess != null) {
                        consumerSuccess.accept(video.getLocationsCount());
                    }
                }
            }
        }
        videoFile = null;
    }

    /**
     * @param video the video which will be persisted until this operation ends due to post-network success processing.
     * @return {@code Completable} with the API repose of the upload video network request.
     * <p> The </p>
     */
    private Completable processUploadVideoCompletable(Video video) {
        this.video = video;
        videoFile = new KVFile(video.getPath());
        if (videoFile.exists()) {
            return getUploadVideoRequestCompletable();
        } else {
            Throwable throwable = new Throwable(THROWABLE_MESSAGE_VIDEO_FILE_NOT_FOUND);
            if (consumerError != null) {
                consumerError.accept(throwable);
            }
            return Completable.error(throwable);
        }
    }

    /**
     * @return {@code Completable} which will make the upload video request and handle error/retry mechanism for the network.
     */
    private Completable getUploadVideoRequestCompletable() {
        return api
                .uploadVideo(
                        NetworkRequestConverter.generateTextRequestBody(accessToken),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(onlineSequenceId)),
                        NetworkRequestConverter.generateTextRequestBody(String.valueOf(video.getIndex())),
                        NetworkRequestConverter.generateMultipartBodyPart(NetworkRequestConverter.REQUEST_MEDIA_TYPE_VIDEO, VIDEO_NAME_MULTI_PART_BODY, videoFile, this))
                .doOnSubscribe(consumer -> {
                    uploadUpdateProgressVideo = new UploadUpdateProgress(0, Utils.fileSize(videoFile));
                    updateEventBus.post(uploadUpdateProgressVideo);
                })
                .flatMapCompletable(response -> {
                    if (handleResponse(response)) {
                        handleUploadVideoSuccessResponse();
                        return Completable.complete();
                    }

                    return Completable.error(new Throwable(String.valueOf(response.getStatus().code)));
                })
                //catch any error which bypass the response check, used mostly for duplicate error check which means that the data was already upload previous, hence the
                // success signal, otherwise propagate the error forward
                .onErrorComplete(error -> {
                    if (handleResponse(error)) {
                        handleUploadVideoSuccessResponse();
                        return true;
                    }
                    handleUploadVideoErrorResponse(error);
                    return false;
                });
    }
}
