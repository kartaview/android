package com.telenav.osv.recorder.persistence.video;

import android.graphics.ImageFormat;
import android.media.MediaCodecInfo;
import android.os.HandlerThread;

import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.data.video.model.Video;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.recorder.encoder.VideoEncoder;
import com.telenav.osv.recorder.metadata.callback.MetadataPhotoVideoCallback;
import com.telenav.osv.recorder.persistence.RecordingFrame;
import com.telenav.osv.recorder.persistence.RecordingPersistenceManager;
import com.telenav.osv.recorder.persistence.RecordingPersistenceStatus;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;

import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Manager which is responsible to persist each frame in database and on local storage as being part from a video file.
 * The manager receives an array of byte representing the frame information which are encoded into a video file.
 */
public class VideoPersistenceManager extends RecordingPersistenceManager {

    private static final String TAG = VideoPersistenceManager.class.getSimpleName();

    /**
     * The name for the handler thread used for encoding operation.
     */
    private static final String HANDLER_NAME = "VideoEncoderHandler";

    /**
     * The maximum number of frames/video.
     * When the maximum number of frames is reached a new video should be created.
     */
    private static final int MAX_FRAME = 50;

    /**
     * The current video index which is increased when a video reaches the maximum number of frames.
     */
    private int currentVideoIndex = -1;

    /**
     * Instance of {@code encoder} component which is used to convert camera frames in a video file.
     */
    private VideoEncoder videoEncoder;

    /**
     * Local data source for video database persistence.
     */
    private VideoLocalDataSource videoLocalDataSource;

    /**
     * The disk size for the current sequence.
     */
    private long diskSize = 0;

    /**
     * A flag to determine if a new video should be created.
     * The flag is {@code true} when the current video has the number of {@link #MAX_FRAME} encoded frames,
     * {@code false} otherwise.
     */
    private boolean isNewVideo;

    /**
     * The color format for the video.
     * The frames should be encoded in this format before sending them further to the encoder.
     */
    private int videoColorFormat;

    /**
     * The thread for the encoding operation.
     */
    private HandlerThread backgroundThread;

    /**
     * Constructor for video persistence manager.
     * @param sequenceLocalDataSource local data source for sequence.
     * @param locationLocalDataSource local data source for location.
     * @param videoLocalDataSource local data source for frame.
     */
    public VideoPersistenceManager(SequenceLocalDataSource sequenceLocalDataSource, LocationLocalDataSource locationLocalDataSource,
                                   VideoLocalDataSource videoLocalDataSource, VideoEncoder videoEncoder, MetadataPhotoVideoCallback metadataPhotoVideoCallback) {
        super(sequenceLocalDataSource, locationLocalDataSource, metadataPhotoVideoCallback);
        this.videoLocalDataSource = videoLocalDataSource;
        this.videoEncoder = videoEncoder;
        setEncodingFormat();
    }

    @Override
    public Completable start(LocalSequence sequence, Size formatSize, int imageFormat) {
        startHandler();
        return super.start(sequence, formatSize, imageFormat);
    }

    @Override
    public Completable save(RecordingFrame frame) {
        Log.d(TAG, "save()");
        return prepareVideoFileForEncoder()
                .andThen(videoEncoder.encode(getConvertedFrameData(frame.getFrameData()))
                        .subscribeOn(Schedulers.computation()))
                .observeOn(recordingPersistenceScheduler)
                .andThen(persistVideoToLocalDataSource()
                        .flatMapCompletable(video -> persistLocationToLocalDataSource(video, frame)));
    }

    @Override
    protected void remove(String id) {
        boolean removeVideo = videoLocalDataSource.deleteVideo(id);
        Log.d(TAG, String.format("remove. Status: %s. Video id: %s", removeVideo, id));
    }

    @Override
    public Completable stop() {
        return super.stop().doOnComplete(() -> {
            videoEncoder.stopEncoder();
            stopHandler();
            long newSize = Utils.fileSize(new KVFile(getCurrentVideoPath())) - diskSize;
            updateSequenceDiskSize(newSize);
            currentVideoIndex = -1;
            sequence = null;
            folderPath = null;
        });
    }

    /**
     * Starts the handler in order to perform the handler operation on a background thread.
     */
    private void startHandler() {
        backgroundThread = new HandlerThread(HANDLER_NAME);
        backgroundThread.start();
    }

    /**
     * Stops the video encoding handler.
     */
    private void stopHandler() {
        if (backgroundThread == null) {
            return;
        }
        try {
            backgroundThread.quitSafely();
            backgroundThread.join();
            backgroundThread = null;
        } catch (InterruptedException e) {
            Log.e(TAG, String.format("stopBackground: %s", e.getMessage()));
        }
    }

    /**
     * Persist frame location to local storage.
     * @param video the video data.
     * @param frame teh frame data.
     * @return {@code Completable} which notifies the observer when the operation was finished.
     */
    private Completable persistLocationToLocalDataSource(Video video, RecordingFrame frame) {
        return Completable.create(emitter -> {
            //persist the location of the frame
            long fileSize = Utils.fileSize(new KVFile(video.getPath()));
            boolean persistLocation = persistLocation(frame.getLocation(),
                    null,
                    video.getID(),
                    fileSize - diskSize,
                    currentVideoIndex,
                    frame.getTimestamp());
            //if the location could not be persisted removes the video
            Log.d(TAG, String.format("persistLocationToLocalDataSource. Status: %s. Sequence id: %s. Video compression: %s.", persistLocation, sequence.getID(), true));
            if (!persistLocation) {
                remove(video.getID());
                emitter.onError(new Throwable(RecordingPersistenceStatus.STATUS_ERROR_LOCATION_PERSISTENCE));
                return;
            } else {
                diskSize = fileSize;
                if (isNewVideo) {
                    SequenceDetailsCompressionVideo compressionVideo = ((SequenceDetailsCompressionVideo) sequence.getCompressionDetails());
                    if (compressionVideo.getVideos() != null) {
                        compressionVideo.getVideos().add(video);
                        Log.d(TAG, "persistLocationToLocalDataSource. Status: success. Message: New video added.");
                    } else {
                        Log.d(TAG, "persistLocationToLocalDataSource. Status: error. Message: Failed to add the new video.");
                        emitter.onError(new Throwable());
                        return;
                    }
                    isNewVideo = false;
                }
                updateSequenceCache(sequence.getDetails(), frame.getLocation(), frame.getDistance(), true);
            }
            emitter.onComplete();
        });
    }

    /**
     * Persists the current video to local storage and if is necessary creates a new video.
     * @return {@code Single} which emits the current video.
     */
    private Single<Video> persistVideoToLocalDataSource() {
        return Single.create(emitter -> {
            SequenceDetailsCompressionVideo compressionVideo = ((SequenceDetailsCompressionVideo) sequence.getCompressionDetails());
            String sequenceID = sequence.getID();
            Video video = null;
            // save the video in the persistence with a new video index
            if (isNewVideo) {
                video = new Video(UUID.randomUUID().toString(), 0, currentVideoIndex, getCurrentVideoPath());
                boolean saveVideo = videoLocalDataSource.saveVideo(video, sequenceID);
                if (!saveVideo) {
                    boolean deleteVideoStatus = new KVFile(video.getPath()).delete();
                    Log.d(TAG, String.format("persistVideoToLocalDataSource. Status: %s. Message: The video could not be persisted. Removing video physical device.",
                            deleteVideoStatus));
                    emitter.onError(new Throwable());
                    return;
                } else {
                    Log.d(TAG, String.format("persistVideoToLocalDataSource. Status: success. Message: Persist video. Sequence id: %s. ", sequenceID));
                }
            } else if (compressionVideo.getVideos() != null) {
                video = compressionVideo.getVideos().get(currentVideoIndex);
            }
            if (video == null) {
                Log.d(TAG, String.format("persistVideoToLocalDataSource. Status: error. Message: Video not found for index - %s", currentVideoIndex));
                emitter.onError(new Throwable());
                return;
            }
            video.setLocationsCount(video.getLocationsCount() + 1);
            boolean updateVideo = videoLocalDataSource.updateFrameCount(video.getID(), video.getLocationsCount());
            Log.d(TAG, String.format("persistVideoToLocalDataSource. Status: %s. Message: Update video frame count. Sequence id: %s", updateVideo, sequenceID));
            if (!updateVideo) {
                emitter.onError(new Throwable());
                return;
            }
            emitter.onSuccess(video);
        });
    }

    /**
     * Initialises and starts the encoder for a new video encoding.
     * @return {@code Completable} which notifies the observer when the initialization was finished.
     */
    private Completable prepareVideoFileForEncoder() {
        return Single
                .create(emitter -> {
                    Log.d(TAG, "prepareVideoFileForEncoder. frameIndex: " + frameIndex);
                    if (frameIndex % MAX_FRAME == 0) {
                        isNewVideo = true;
                        Log.d(TAG, "prepareVideoFileForEncoder. currentVideoIndex: " + currentVideoIndex);
                        //for first frame the encoder is not started yet
                        if (frameIndex != 0) {
                            Log.d(TAG, String.format("prepareVideoFileForEncoder. Status: pre stop encoder. Disk size: %s.", diskSize));
                            videoEncoder.stopEncoder();
                            long newSize = Utils.fileSize(new KVFile(getCurrentVideoPath())) - diskSize;
                            updateSequenceDiskSize(newSize);
                            Log.d(TAG, String.format("prepareVideoFileForEncoder. Status: post stop encoder. Disk size: %s.", newSize));
                        }
                        currentVideoIndex++;
                        diskSize = 0;
                        new KVFile(getCurrentVideoPath());
                        Log.d(TAG, String.format("prepareVideoFileForEncoder. Status: success. Message: New video file with index %s", currentVideoIndex));
                        emitter.onSuccess(true);
                    } else {
                        Log.d(TAG, String.format("prepareVideoFileForEncoder. Status: success. Message: Same video file, disk size - %s", diskSize));
                        emitter.onSuccess(false);
                    }
                })
                .subscribeOn(recordingPersistenceScheduler)
                .flatMapCompletable(isNewVideo -> startVideoEncoder((Boolean) isNewVideo)
                        .subscribeOn(AndroidSchedulers.from(backgroundThread.getLooper())));
    }

    /**
     * Starts the encoder for a new video.
     * @param isNewVideo {@code true} if a new video was created, {@code false} otherwise.
     * @return {@code Completable} which notifies the observer when the encoder was started.
     */
    private Completable startVideoEncoder(boolean isNewVideo) {
        if (isNewVideo) {
            Log.d(TAG, "startVideoEncoder. Message: New video file. Start video encoder.");
            return videoEncoder.startEncoder(getCurrentVideoPath(), formatSize, videoColorFormat);
        } else {
            Log.d(TAG, "startVideoEncoder. Message: Same video file, use the same video encoder.");
            return Completable.complete();
        }
    }

    /**
     * @return the path for the current video file.
     */
    private String getCurrentVideoPath() {
        return folderPath + "/" + currentVideoIndex + ".mp4";
    }

    /**
     * Handles the conversion from camera output format to encoding input format.
     * @param cameraFrame the frame data in camera output format.
     * @return the frame data having the encoding format YUV420 Planar/Semi-Planar or
     * {@code null} when none of the YUV format is supported by encoder.
     */
    private byte[] getConvertedFrameData(CameraFrame cameraFrame) {
        Log.d(TAG, "getConvertedFrameData. Image format " + cameraFrame.getImageFormat());
        if (videoColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
            if (imageFormat == ImageFormat.NV21) {
                Log.d(TAG, "getConvertedFrameData. Status: converting. Message: Convert image to YUV420SemiPlanar from NV21.");
                return CameraHelper.convertFromNV21ToYUV420SemiPlanar(cameraFrame.getFrameData(), formatSize.getWidth(), formatSize.getHeight());
            } else {
                Log.d(TAG, "getConvertedFrameData. Status: converting. Message: Convert image to YUV420SemiPlanar from YUV.");
                return CameraHelper.convertCameraYUVtoYUV420SemiPlanar(cameraFrame.getFrameData(), formatSize.getWidth(), formatSize.getHeight());
            }
        } else {
            if (imageFormat == ImageFormat.NV21) {
                Log.d(TAG, "getConvertedFrameData. Status: converting. Message: Convert image to YUV420Planar from NV21.");
                return CameraHelper.convertFromNV21ToYUV420Planar(cameraFrame.getFrameData(), formatSize.getWidth(), formatSize.getHeight());
            } else {
                Log.d(TAG, "getConvertedFrameData. Status: converting. Message: Convert image to YUV420Planar from YUV.");
                return CameraHelper.convertCameraYUVtoYUV420Planar(cameraFrame.getFrameData(), formatSize.getWidth(), formatSize.getHeight());
            }
        }
    }

    /**
     * Sets the format for encoder considering also the frame format.
     */
    private void setEncodingFormat() {
        //Check which formats are supported by the encoder
        boolean isFormatYUV420SemiPlanar = Utils.isYUV420SemiPlanarSupportedByEncoder();
        //Set the video color format to YUV420 semi-planar as first option if is available,
        // because the conversion from NV21 is more efficient, swapping only the V data with U data.
        // For the frame format YUV420 there is no difference between the formats.
        if (isFormatYUV420SemiPlanar) {
            videoColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        } else {
            videoColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        }
        Log.d(TAG, "setEncodingFormat. Codec Format: " + videoColorFormat);
    }
}