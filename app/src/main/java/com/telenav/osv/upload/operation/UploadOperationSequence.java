package com.telenav.osv.upload.operation;

import java.util.ArrayList;
import java.util.List;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.network.OscApi;
import com.telenav.osv.upload.operation.image.UploadOperationImage;
import com.telenav.osv.upload.operation.video.UploadOperationVideo;
import com.telenav.osv.upload.progress.model.UploadUpdateDisk;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

/**
 * The operation which will upload a metadata file to the network.
 * <p> This will generate a stream via {@link #getStream()} method which is the only public entry point to this operation.
 * @author horatiuf
 * @see UploadOperationBase
 * @see #getStream()
 */
public class UploadOperationSequence extends UploadOperationBase {

    /**
     * The identifier for the current class.
     */
    private static final String TAG = UploadOperationSequence.class.getSimpleName();

    /**
     * The synchronization objects used in updates to provide thread safe operations.
     */
    private static final Object synchObject = new Object();

    /**
     * The data source for sequence in order to manipulate the sequence information.
     * @see SequenceLocalDataSource
     */
    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * The data source for video in order to manipulate the video information.
     * @see VideoLocalDataSource
     */
    private VideoLocalDataSource videoLocalDataSource;

    /**
     * The data source for the frame in order to manipulate the frame information.
     * @see FrameLocalDataSource
     */
    private FrameLocalDataSource frameLocalDataSource;

    /**
     * The sequence identifier which will be used in order to initiate the process to upload the sequence identified by the id.
     */
    private String sequenceId;

    /**
     * The {@code disposable} representing updates regarding progress of the upload operation.
     */
    private CompositeDisposable compositeDisposableUpdate;

    /**
     * Reference to the current sequence loaded from the memory. This will be used in order to give any information required by any operation.
     */
    private LocalSequence localSequence;

    /**
     * Progress update related to sequence.
     */
    private UploadUpdateProgress uploadUpdateProgressSequence;

    /**
     * The update progress consumer to be called in order to inform for progress updates.
     */
    private Consumer<UploadUpdateProgress> updateConsumer;

    /**
     * Default constructor for the current class.
     */
    public UploadOperationSequence(@NonNull String sequenceId,
                                   @NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                   @NonNull String accessToken,
                                   @NonNull OscApi api,
                                   @NonNull FrameLocalDataSource frameLocalDataSource,
                                   @NonNull VideoLocalDataSource videoLocalDataSource,
                                   @NonNull Consumer<UploadUpdateProgress> updateConsumer) {
        super(accessToken, api, new SimpleEventBus());
        this.sequenceId = sequenceId;
        this.frameLocalDataSource = frameLocalDataSource;
        this.videoLocalDataSource = videoLocalDataSource;
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        this.updateConsumer = updateConsumer;
        compositeDisposableUpdate = new CompositeDisposable();

        compositeDisposableUpdate.add(updateEventBus
                .filteredObservable(UploadUpdateDisk.class)
                .observeOn(Schedulers.io())
                .subscribe(this::processUploadUpdateDisk,
                        throwable -> Log.d(TAG, String.format("constructor updates disk. Error: %s", throwable.getLocalizedMessage()))));

        compositeDisposableUpdate.add(updateEventBus
                .filteredObservable(UploadUpdateProgress.class)
                .observeOn(Schedulers.io())
                .subscribe(this::processUploadUpdateProgress,
                        throwable -> Log.d(TAG, String.format("constructor updates progress. Error: %s", throwable.getLocalizedMessage()))));
    }

    /**
     * @return {@code Completable} composed of:
     * <ul>
     * <li> persistence get of the sequence with reward</li>
     * <li> metadata process - interally via {@link #processSequenceUploadMetadata(LocalSequence)}</li>
     * <li> compression process - interally via {@link #processSequenceUploadMetadata(LocalSequence)}</li>
     * <li> complete process - interally via {@link #processSequenceUploadMetadata(LocalSequence)}</li>
     * <li> error logging and handling </li>
     * </ul>
     */
    public Completable getStream() {
        return sequenceLocalDataSource
                .getSequenceWithReward(sequenceId)
                .doOnSuccess(item -> {
                    // persist fields which are required by different operations
                    this.localSequence = item;
                    //setup the current progress update
                    long sequenceSize = localSequence.getLocalDetails().getDiskSize();
                    this.uploadUpdateProgressSequence = new UploadUpdateProgress(0, sequenceSize);
                    if (updateConsumer != null) {
                        updateConsumer.accept(uploadUpdateProgressSequence);
                    }
                    //return upload metadata stream
                })
                .flatMapCompletable(sequence ->
                        processSequenceUploadMetadata(sequence)
                                .andThen(Completable.defer(() -> processSequenceUploadTagging(sequence.getLocalDetails().getFolder(), sequence.getDetails().getOnlineId())))
                                .andThen(Completable.defer(() -> processSequenceUploadCompression(sequence.getCompressionDetails() instanceof SequenceDetailsCompressionVideo)))
                                .andThen(Completable.defer(this::processSequenceUploadComplete)))
                .retryWhen(this::handleDefaultRetryFlowableWithTimer)
                .doOnError(throwable -> Log.d(TAG, String.format("getStream. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    @Override
    public void dispose() {
        if (compositeDisposableUpdate != null && !compositeDisposableUpdate.isDisposed()) {
            compositeDisposableUpdate.dispose();
        }
    }

    /**
     * Process any computation required to update the disk size for the sequence.
     * @param uploadUpdateDisk the new {@code UploadUpdateDisk} received from any operation.
     */
    private void processUploadUpdateDisk(UploadUpdateDisk uploadUpdateDisk) {
        synchronized (synchObject) {
            long oldSize = localSequence.getLocalDetails().getDiskSize();
            long newCurrentDiskSize = oldSize - uploadUpdateDisk.getTotalUnit();
            boolean updateDiskSize = sequenceLocalDataSource.updateDiskSize(sequenceId, newCurrentDiskSize);
            Log.d(TAG,
                    String.format("updateDiskSize. Status: %s. Sequence id: %s. Old disk size: %s. New disk Size: %s.",
                            updateDiskSize,
                            sequenceId,
                            oldSize,
                            newCurrentDiskSize));
            if (updateDiskSize) {
                localSequence.getLocalDetails().setDiskSize(newCurrentDiskSize);
            }
        }
    }

    /**
     * Process any computation required to update the progress update for the sequence children.
     * @param uploadUpdateProgress the new {@code UploadUpdateProgress} representing a child.
     */
    private void processUploadUpdateProgress(UploadUpdateProgress uploadUpdateProgress) {
        synchronized (synchObject) {
            if (uploadUpdateProgressSequence != null) {
                if (uploadUpdateProgress.isCancel()) {
                    uploadUpdateProgressSequence.removeChild(uploadUpdateProgress);
                } else if (uploadUpdateProgress.isArchive()) {
                    uploadUpdateProgressSequence.archive(uploadUpdateProgress);
                } else {
                    uploadUpdateProgressSequence.addChild(uploadUpdateProgress);
                }
            }
        }
    }

    /**
     * @param videosIds the identifiers for all videos which will be uploaded.
     * @return {@code collection} with all the completable returned by creation and call of {@link UploadOperationVideo#getStream()} correlated to each video identifier given.
     */
    private Flowable<Completable> setUploadVideoStreamCollection(List<String> videosIds) {
        List<Completable> uploadCompressionCompletables = new ArrayList<>();
        for (String videoId : videosIds) {
            uploadCompressionCompletables.add(
                    new UploadOperationVideo(
                            accessToken,
                            api,
                            videoLocalDataSource,
                            videoId,
                            localSequence.getDetails().getOnlineId(),
                            updateEventBus,
                            videoSuccessResponseConsumer(),
                            null)
                            .getStream());
        }
        return Flowable.fromIterable(uploadCompressionCompletables);
    }

    /**
     * @param frameIds the identifiers for all frames which will be uploaded.
     * @return {@code collection} with all the completable returned by creation and call of {@link UploadOperationImage#getStream()} correlated to each frame identifier given.
     */
    private Flowable<Completable> setUploadFrameStreamCollection(List<String> frameIds) {
        List<Completable> uploadCompressionCompletables = new ArrayList<>();
        for (String frameId : frameIds) {
            uploadCompressionCompletables.add(
                    new UploadOperationImage(
                            accessToken,
                            api,
                            frameLocalDataSource,
                            frameId,
                            localSequence.getDetails().getOnlineId(),
                            updateEventBus,
                            frameSuccessResponseAction(),
                            null)
                            .getStream());
        }
        return Flowable.fromIterable(uploadCompressionCompletables);
    }

    /**
     * The action success consumer which will update the frame count for the current sequence in both persistence and cache.
     */
    private Action frameSuccessResponseAction() {
        return () -> {
            synchronized (synchObject) {
                SequenceDetailsCompressionBase compressionBase = localSequence.getCompressionDetails();
                int newLocationsCount = compressionBase.getLocationsCount() - 1;
                boolean updateSizeCount = sequenceLocalDataSource.updateCompressionSizeInfo(localSequence.getID(), newLocationsCount, 0);
                Log.d(TAG,
                        String.format("frameSuccessResponseAction. Status: %s. Message: Updating compression size info. Frame count: %s",
                                updateSizeCount,
                                newLocationsCount));
                compressionBase.setLocationsCount(newLocationsCount);
            }
        };
    }

    /**
     * The video success consumer which will update the frame count and video count for the current sequence in both persistence and cache.
     */
    private Consumer<Integer> videoSuccessResponseConsumer() {
        return (locationsCount) -> {
            SequenceDetailsCompressionBase compressionBase = localSequence.getCompressionDetails();
            int newLocationsCount = compressionBase.getLocationsCount() - locationsCount;
            int newVideoCount = compressionBase.getLength() - 1;
            boolean updateSizeCount = sequenceLocalDataSource.updateCompressionSizeInfo(localSequence.getID(), newLocationsCount, newVideoCount);
            Log.d(TAG,
                    String.format("handleVideoSuccessSequenceUpdate. Status: %s. Message: Updating compression size info. Loc count: %s. Video count: %s",
                            updateSizeCount,
                            newLocationsCount,
                            newVideoCount));
            compressionBase.setLength(newVideoCount);
            compressionBase.setLocationsCount(newLocationsCount);
        };
    }

    /**
     * @param localSequence the sequence given from local persistence which will be used to create the upload metadata operation.
     * @return Completable with the logic of {@link UploadOperationMetadata}. Also it will persist before fields which are required by different operations.
     */
    private Completable processSequenceUploadMetadata(LocalSequence localSequence) {
        return new UploadOperationMetadata(
                accessToken,
                api,
                this.localSequence.getLocalDetails().getFolder(),
                this.localSequence.getDetails(),
                updateEventBus,
                this.localSequence.getRewardDetails(),
                (onlineId) -> {
                    boolean updateOnlineId = sequenceLocalDataSource.updateOnlineId(sequenceId, onlineId);
                    Log.d(TAG, String.format("processSequenceUploadMetadata. Status: %s. Message: Persisting the sequence online id in the persistence.", updateOnlineId));
                    localSequence.getDetails().setOnlineId(onlineId);
                },
                null)
                .getStream();
    }

    /**
     * @param sequenceFolder the sequence given from local persistence which will be used to create the upload tagging operation.
     * @return Completable with the logic of {@link UploadOperationTagging}.
     */
    private Completable processSequenceUploadTagging(OSVFile sequenceFolder, long onlineId) {
        return new UploadOperationTagging(
                sequenceFolder,
                onlineId,
                accessToken,
                api,
                updateEventBus,
                null)
                .getStream();
    }

    /**
     * @return {@code Completable} composed by:
     * <ul>
     * <li>
     * video/frame local data source search by ids based on the given param.
     * </li>
     * <li>
     * process above ids into a stream by using {@link Completable#mergeDelayError(Iterable)} with the iterable returned by {@link #setUploadVideoStreamCollection(List)} or
     * {@link #setUploadFrameStreamCollection(List)}.
     * </li>
     * </ul>
     */
    private Completable processSequenceUploadCompression(boolean isVideoCompression) {
        if (isVideoCompression) {
            return videoLocalDataSource
                    .getVideoIdsBySequenceId(sequenceId)
                    .doOnSuccess(frameIds -> Log.d(TAG,
                            String.format("processSequenceUploadCompression. Status: success. Count: %s. Message: Videos found for sequence.", frameIds.size())))
                    .flatMapCompletable(videoIds -> Completable
                            .mergeDelayError(setUploadVideoStreamCollection(videoIds), MERGE_DELAY_ERROR_NO_SERIAL)
                            .retryWhen(this::handleDefaultRetryFlowableWithTimer));
        }

        return frameLocalDataSource
                .getFrameIdsBySequenceId(sequenceId)
                .doOnSuccess(frameIds -> Log.d(TAG,
                        String.format("processSequenceUploadCompression. Status: success. Count: %s. Message: Frames found for sequence.", frameIds.size())))
                .flatMapCompletable(framesIds ->
                        Completable
                                .mergeDelayError(setUploadFrameStreamCollection(framesIds), MERGE_DELAY_ERROR_CONCURRENT_NO)
                                .retryWhen(this::handleDefaultRetryFlowableWithTimer));
    }

    /**
     * @return {@code Completable} representing {@link UploadOperationSequenceComplete#getStream()} method for the current sequence.
     */
    private Completable processSequenceUploadComplete() {
        return new UploadOperationSequenceComplete(
                accessToken,
                api,
                localSequence.getDetails().getOnlineId(),
                updateEventBus,
                sequenceCompleteSuccessAction(),
                null)
                .getStream();
    }

    /**
     * The video success action which will remove the sequence from persistence and device.
     * @return action which handles sequence complete operation success behaviour related to sequence.
     */
    private Action sequenceCompleteSuccessAction() {
        return () -> {
            if (localSequence.getLocalDetails().getDiskSize() != 0) {
                boolean updateDiskSize = sequenceLocalDataSource.updateDiskSize(sequenceId, 0);
                Log.d(TAG, String.format("sequenceCompleteSuccessAction. Status: %s. Sequence id: %s. New size: 0", updateDiskSize, localSequence.getID()));
                uploadUpdateProgressSequence.setCurrentUnit(uploadUpdateProgressSequence.getTotalUnit());
            }
            boolean deleteSequenceFromPersistence = sequenceLocalDataSource.deleteSequence(sequenceId);
            Log.d(TAG, String.format(
                    "sequenceCompleteSuccessAction. Status: %s. Sequence id: %s. Message: Attempting to remove sequence from persistence.",
                    deleteSequenceFromPersistence,
                    sequenceId));
            OSVFile sequenceFolder = localSequence.getLocalDetails().getFolder();
            if (deleteSequenceFromPersistence && sequenceFolder.exists()) {
                boolean folderDelete = sequenceFolder.delete();
                Log.w(TAG, String.format("sequenceCompleteSuccessAction. Status: %s. Attempting to remove the folder for the sequence local id: %s.",
                        folderDelete,
                        sequenceId));
            }
            uploadUpdateProgressSequence.complete();
        };
    }
}
