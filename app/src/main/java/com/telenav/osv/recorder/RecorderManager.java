package com.telenav.osv.recorder;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.telenav.datacollectormodule.datatype.datatypes.AccuracyObject;
import com.telenav.datacollectormodule.datatype.datatypes.AltitudeObject;
import com.telenav.datacollectormodule.datatype.datatypes.GPSData;
import com.telenav.datacollectormodule.datatype.datatypes.OpenXcSpeedObject;
import com.telenav.datacollectormodule.datatype.datatypes.PositionObject;
import com.telenav.datacollectormodule.datatype.util.LibraryUtil;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.PhotoCommand;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPaid;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.data.user.model.details.driver.DriverDetails;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.item.metadata.DataCollectorItemWrapper;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.filter.LocationFilterType;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.persistence.RecordingFrame;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.persistence.RecordingPersistenceStatus;
import com.telenav.osv.recorder.sensor.SensorManager;
import com.telenav.osv.recorder.shutter.Shutter;
import com.telenav.osv.recorder.tagging.logger.RecordingTaggingLogger;
import com.telenav.osv.recorder.tagging.logger.RecordingTaggingLoggerImpl;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.fabric.sdk.android.Fabric;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import static com.telenav.osv.obd.manager.ObdManager.ObdConnectionListener;

/**
 * The class is the middle man between the following components: location, photos, camera, recording, obd, score and sensors.
 * Also, the class is responsible for recording and for saving a track.
 * Each track has a sequence. The sequence contains all the photos made during that track.
 * <p>
 * There are two recording modes:
 * <ul>
 * <li>
 * JPEG recording mode: the application will take photos and stored them into the sequence folder as JPEG files.
 * </li>
 * <li>
 * MP4 recording mode: the application will take photos and convert them to an mp4 which will be stored into the sequence folder.
 * The maximum number of photos for an mp4 file is 64. When the maximum number of taken photos was succeed a new mp4 file will be created.
 * </li>
 * </ul>
 * </p>
 * <p>
 * Created by Kalman on 04/07/16.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class RecorderManager implements ObdConnectionListener, SensorManager.MetadataLoggerListener {

    public static final int UNKNOWN_USER_TYPE = -1;

    private static final String TAG = RecorderManager.class.getSimpleName();

    private static final int MIN_FREE_SPACE = 500;

    private final Context mContext;

    private final LocationService locationService;

    private final ApplicationPreferences appPrefs;

    private Shutter shutterManager;

    private Camera camera;

    private ObdManager mOBDManager;

    private Location mPreviousLocation;

    private Location mActualLocation;

    private ThreadPoolExecutor mThreadPoolExec;

    private LocalSequence sequence;

    private boolean recording;

    private UploadManager uploadManager;

    private SensorManager mSensorManager;

    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * @see com.telenav.osv.data.user.datasource.local.UserLocalDataSource
     */
    private UserDataSource userLocalDataSource;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Disposable takePictureDisposable;

    /**
     * The recording {@code Subject} which emits a {@code Boolean} to the currently subscribed observers.
     * The value emitted will be {@code true} if the recording started,{@code false } otherwise.
     */
    private PublishSubject<Boolean> recordingEventPublishSubject = PublishSubject.create();

    /**
     * The recording {@code Subject} which emits an {@code Exception} to the currently subscribed observers.
     */
    private PublishSubject<Exception> recordingErrorPublishSubject = PublishSubject.create();

    /**
     * The image capture {@code Subject} which emits {@code ImageSavedEvent} to the currently subscribed observers.
     */
    private PublishSubject<ImageSavedEvent> imageCapturePublishSubject = PublishSubject.create();

    /**
     * Persistence manager which is responsible to encode and store a JPEG or a video file.
     */
    private RecordingPersistence recordingPersistence;

    /**
     * Logger for recording tagging.
     * This is available only during a recording session and should be release when recording stops.
     */
    private RecordingTaggingLogger recordingTaggingLogger;

    public RecorderManager(@NonNull OSVApplication app,
                           @NonNull UserDataSource userLocalDataSource,
                           @NonNull SequenceLocalDataSource sequenceLocalDataSource,
                           @NonNull ObdManager obdManager,
                           @NonNull LocationService locationService,
                           @NonNull Shutter shutterManager) {
        mContext = app;
        this.locationService = locationService;
        this.userLocalDataSource = userLocalDataSource;
        this.mOBDManager = obdManager;
        this.shutterManager = shutterManager;
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        int coreNum = 1;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        mThreadPoolExec = new ThreadPoolExecutor(coreNum, coreNum, 7, TimeUnit.SECONDS, workQueue,
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("Recorder-pool-%d")
                        .setPriority(Thread.MAX_PRIORITY).build());
        uploadManager = Injection.provideUploadManager();
        mSensorManager = new SensorManager(mContext);
        appPrefs = app.getAppPrefs();
        EventBus.register(this);
    }

    @Override
    public void onSpeedObtained(SpeedData speedData) {
        try {
            if (sequence == null) {
                return;
            }
            SequenceDetails sequenceDetails = sequence.getDetails();
            if (!sequence.getDetails().isObd()) {
                sequenceDetails.setObd(true);
                sequenceLocalDataSource.updateObd(sequence.getID(), true);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "onSpeedObtained: " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void onObdConnected() {
    }

    @Override
    public void onObdDisconnected() {
    }

    @Override
    public void onObdConnecting() {
    }

    @Override
    public void onObdInitialised() {
    }

    @Override
    public void onMetadataLoggingError(Exception e) {
        if (isRecording()) {
            if (Fabric.isInitialized()) {
                Answers.getInstance().logCustom(new CustomEvent("Metadata Logging Error")
                        .putCustomAttribute("Sequence frames ", sequence.getCompressionDetails().getLocationsCount())
                        .putCustomAttribute("Device model", Build.MODEL));
            }
            Log.d(TAG, "onMetadataLoggingError. Status: error. Message: Error while logging in metadata => stop recording.");
            internalStopRecording();
            recordingErrorPublishSubject.onNext(e);
        }
    }

    @Override
    public void onMetadataLoggingFinished(long metadataSize) {
        String sequenceId = appPrefs.getStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID);
        if (!StringUtils.isEmpty(sequenceId)) {
            long sequenceSize = sequence.getLocalDetails().getDiskSize();
            long newSequenceSize = sequenceSize + metadataSize;
            Log.d(TAG,
                    String.format("onMetadataLoggingFinished. Status: update disk size. Sequence size: %s. Metadata size: %s. New size: %s.",
                            sequenceSize,
                            metadataSize,
                            newSequenceSize));
            sequenceLocalDataSource.updateDiskSize(sequenceId, newSequenceSize);
            sequence.getLocalDetails().setDiskSize(newSequenceSize);
            appPrefs.removePreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID);
        }
        recordingEventPublishSubject.onNext(false);
        sequence = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoCommand(PhotoCommand command) {
        if (isRecording()) {
            if (locationService != null && hasPosition() && isLocationValid(mActualLocation)) {
                try {
                    takeSnapshotIfLocationDataValid(copyLocation(mActualLocation), 0);
                } catch (Exception e) {
                    Log.d(TAG, "takePhoto: " + Log.getStackTraceString(e));
                }
            }
        }
    }

    public boolean hasPosition() {
        return mPreviousLocation != null && mActualLocation != null;
    }

    /**
     * Starts the recording. Before recording, the upload tasks are canceled.
     * The method creates a new sequence and the recording will take place if the sequence and the sequence path are not null.
     */
    @SuppressLint("CheckResult")
    public void startRecording() {
        cancelCurrentUpload();
        mOBDManager.addObdConnectionListener(this);
        shutterManager.onRecordingStateChanged(true);
        observeOnLocationUpdates();
        observeOnTakeImageRequests();
        mThreadPoolExec.execute(() -> {
            recording = true;
            //save the start record time
            long currentTimeMillis = LocalDateTime.now().toDateTime().getMillis();
            appPrefs.saveLongPreference(PreferenceTypes.K_RECORD_START_TIME, currentTimeMillis);

            // Specific ignored subscribeOn in order for this code to be executed on the current thread synchronously
            sequence = generateSequence();
            userLocalDataSource.getUser().subscribe(
                    //onSuccess
                    user -> {
                        SequenceDetailsRewardBase rewardBase = null;
                        BaseUserDetails userDetails = user.getDetails();
                        if (userDetails instanceof DriverDetails) {
                            DriverDetails driverDetails = (DriverDetails) userDetails;
                            //ToDo: addChild pay rate if required
                            rewardBase = new SequenceDetailsRewardPaid(0, driverDetails.getDriverPayment().getCurrencySymbol(), null);
                        } else if (appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION)) {
                            rewardBase = new SequenceDetailsRewardPoints(0, "points", null);
                        }
                        sequence.setRewardDetails(rewardBase);
                    },
                    //onError
                    throwable -> Log.d(TAG, String.format("startRecording. Status: error. Message: %s.", throwable.getLocalizedMessage())),
                    //onComplete
                    () -> Log.d(TAG, "startRecording. Status: complete. Message: User not found.")
            );
            boolean persistSequence = sequenceLocalDataSource.persistSequence(sequence);
            // Check if the sequence and the sequence physical folder has been created
            if (sequence.getLocalDetails().getFolder() == null
                    || !persistSequence) {
                mainThreadHandler.post(() -> Toast.makeText(mContext, R.string.record_failed, Toast.LENGTH_LONG).show());
                recording = false;
                internalStopRecording();
                return;
            }
            appPrefs.saveStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID, sequence.getID());
            //start recording if every check from above had passed
            Log.d(TAG, "startRecording");
            recordingEventPublishSubject.onNext(true);
            //notify sensor manager that a video recording has started and should resume sensor data gathering in case was paused.
            mSensorManager.onResume(sequence.getLocalDetails().getFolder(), !isVideoCompression(), RecorderManager.this);
            //updates flag in crashlytics that the recording has started
            if (Fabric.isInitialized()) {
                Crashlytics.setBool(Log.RECORD_STATUS, true);
            }
            startRecordingPersistence();
        });
    }

    /**
     * @return {@code OSVFile} which represents the current sequence folder.
     */
    @Nullable
    public OSVFile getFolder() {
        if (sequence != null) {
            return sequence.getLocalDetails().getFolder();
        }

        return null;
    }

    private void internalStopRecording() {
        Log.d(TAG, "internalStopRecording");
        mThreadPoolExec.execute(getStopRecordingRunnable());
        recordingErrorPublishSubject.onNext(new Exception("The recording encountered an internal error."));
    }

    public void stopRecording() {
        mThreadPoolExec.execute(getStopRecordingRunnable());
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * The method is used to stop the recording. When a recording is stopped the number of frames for the current sequence is updated.
     * <p>If the number of frames for the current sequence is 0 then the device folder and its data from the persistence will be removed.
     */
    public void forceCloseCamera() {
        if (camera != null) {
            camera.closeCamera();
        }
    }

    /**
     * Method used to subscribe for recording state changes.
     * @return the observable for recording state changes.
     */
    public Subject<Boolean> getRecordingStateObservable() {
        return recordingEventPublishSubject;
    }

    /**
     * Method used to subscribe for recording errors.
     * @return the observable for recording errors.
     */
    public Subject<Exception> getRecordingErrorObservable() {
        return recordingErrorPublishSubject;
    }

    /**
     * Method used to subscribe for image capture events.
     * @return the observable for image capture events.
     */
    public Subject<ImageSavedEvent> getImageCaptureObservable() {
        return imageCapturePublishSubject;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void setRecordingPersistence(RecordingPersistence recordingPersistence) {
        this.recordingPersistence = recordingPersistence;
    }

    /**
     * @return concrete implementation of {@link RecordingTaggingLogger}.
     */
    public RecordingTaggingLogger getRecordingTaggingLogger() {
        if (recordingTaggingLogger == null) {
            recordingTaggingLogger = new RecordingTaggingLoggerImpl(getImageCaptureObservable(), sequenceLocalDataSource);
        }
        return recordingTaggingLogger;
    }

    public void release() {
        Log.d(TAG, "release recorder component");
        EventBus.unregister(this);
        shutterManager.destroy();
        recordingPersistence = null;
    }

    private void observeOnTakeImageRequests() {
        compositeDisposable.add(
                shutterManager.getTakeImageObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(distance -> {
                            if (isRecording() && mActualLocation != null && mActualLocation.getLatitude() != 0 && mActualLocation.getLongitude() != 0) {
                                takeSnapshotIfLocationDataValid(mActualLocation, distance);
                                mPreviousLocation = mActualLocation;
                            }
                        }));
    }

    /**
     * @return {@code Runnable} representing the record stop event in order to update data.
     */
    @SuppressLint("CheckResult")
    @NonNull
    private Runnable getStopRecordingRunnable() {
        return () -> {
            Log.d(TAG, "getStopRecordingRunnable. Status: stopping. Message: Initialising recording stop.");
            if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
                compositeDisposable.clear();
            }
            if (takePictureDisposable != null && !takePictureDisposable.isDisposed()) {
                takePictureDisposable.dispose();
            }
            mOBDManager.removeObdConnectionListener(this);
            shutterManager.onRecordingStateChanged(false);
            //resets the recording
            appPrefs.saveLongPreference(PreferenceTypes.K_RECORD_START_TIME, 0);
            recording = false;
            if (!appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC) && camera != null) {
                camera.unlockFocus();
            }
            //if the last sequence was created but no frames was stored for it, then the sequence will be removed
            if (sequence != null) {
                SequenceDetailsLocal sequenceDetailsLocal = sequence.getLocalDetails();
                String sequenceID = sequence.getID();
                OSVFile sequenceFolder = sequenceDetailsLocal.getFolder();
                // if the sequence metadata is missing or the sequence does not have any locations persisted any data related to it both on the persistence and on db.
                if (!Utils.doesMetadataExist(sequenceFolder) || sequence.getCompressionDetails().getLocationsCount() == 0) {
                    boolean sequenceRemove = sequenceLocalDataSource.deleteSequence(sequenceID);
                    Log.d(TAG,
                            String.format(
                                    "getStopRecordingRunnable removeSequence. Status: %s. Id: %s. Message: Removing sequence due issues with either location or metadata.",
                                    sequenceRemove,
                                    sequenceID));
                    if (sequenceFolder != null) {
                        boolean folderRemoved = sequenceFolder.delete();
                        Log.d(TAG,
                                String.format(
                                        "getStopRecordingRunnable removeSequence. Status: %s. Id: %s. Message: Deleting folder for the sequence.",
                                        folderRemoved,
                                        sequenceID));
                    }
                } else {
                    Log.d(TAG, "getStopRecordingRunnable. Status: schedule auto-upload. Message: Scheduling auto-upload for the new recorder sequence.");
                    boolean updateMetadataDiskSize = sequenceLocalDataSource.updateDiskSize(sequenceID,
                            sequence.getLocalDetails().getDiskSize() + Utils.getMetadataSize(sequenceFolder));
                    Log.d(TAG, String.
                            format("getStopRecordingRunnable add metadata to disk size. Status: %s. Message: Attempting to update disk size of the sequence.",
                                    updateMetadataDiskSize));
                    //ToDo: schedule auto-upload
                }
                if (Fabric.isInitialized()) {
                    Crashlytics.setBool(Log.RECORD_STATUS, false);
                    userLocalDataSource
                            .getUser()
                            .subscribe(
                                    user -> {
                                        Log.d(TAG, "getStopRecordingRunnable getUser. Status: success. Message: User found.");
                                        SequenceDetails sequenceDetails = sequence.getDetails();
                                        Answers.getInstance().logCustom(
                                                new CustomEvent("Recorded track")
                                                        .putCustomAttribute("images", sequence.getCompressionDetails().getLocationsCount())
                                                        .putCustomAttribute("obd", "" + sequenceDetails.isObd())
                                                        .putCustomAttribute("userType", user.getUserType())
                                                        .putCustomAttribute("length", sequenceDetails.getDistance()));
                                    },
                                    throwable -> Log.d(TAG, String.format("getStopRecordingRunnable getUser. Status: error. Message: %s.", throwable.getLocalizedMessage())),
                                    () -> {
                                        Log.d(TAG, "getStopRecordingRunnable getUser. Status: complete. Message: User not found.");
                                        SequenceDetails sequenceDetails = sequence.getDetails();
                                        Answers.getInstance().logCustom(
                                                new CustomEvent("Recorded track")
                                                        .putCustomAttribute("images", sequence.getCompressionDetails().getLocationsCount())
                                                        .putCustomAttribute("obd", "" + sequenceDetails.isObd())
                                                        .putCustomAttribute("userType", UNKNOWN_USER_TYPE)
                                                        .putCustomAttribute("length", sequenceDetails.getDistance()));
                                    }
                            );
                }
            }
            if (recordingTaggingLogger != null && sequence != null) {
                recordingTaggingLogger.finish(sequence.getID(), sequence.getLocalDetails());
                recordingTaggingLogger = null;
            }
            Log.d(TAG, "stopRecording");
            if (recordingPersistence != null) {
                recordingPersistence.stop()
                        .subscribe(() -> {
                            Log.d(TAG, "stop RecordingPersistence. Status: success");
                        }, throwable -> {
                            Log.d(TAG, String.format("stop RecordingPersistence. Status: error. Message: %s", throwable.getMessage()));
                        });
            }
            mSensorManager.onPauseOrStop();
        };
    }

    /**
     * @return {@code LocalSequence} representing the new recorded sequence with default values.
     */
    private LocalSequence generateSequence() {
        Location defaultLocation = new Location(StringUtils.EMPTY_STRING);
        defaultLocation.setLatitude(0);
        defaultLocation.setLongitude(0);
        String sequenceId = UUID.randomUUID().toString();
        SequenceDetails sequenceDetails = new SequenceDetails(defaultLocation,
                0,
                OSVApplication.VERSION_NAME,
                new DateTime());
        sequenceDetails.setObd(mOBDManager.getObdState() == ObdManager.ObdState.OBD_CONNECTED);

        OSVFile oscFile = new OSVFile(Utils.generateOSVFolder(mContext).getPath(), "/SEQ_" + sequenceId);
        SequenceDetailsLocal sequenceDetailsLocal = new SequenceDetailsLocal(oscFile, 0, SequenceDetailsLocal.SequenceConsistencyStatus.VALID);

        SequenceDetailsCompressionBase sequenceDetailsCompressionBase;
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED)) {
            SequenceDetailsCompressionVideo compressionVideo = new SequenceDetailsCompressionVideo(0, null, 0);
            compressionVideo.setVideos(new ArrayList<>());
            sequenceDetailsCompressionBase = compressionVideo;
        } else {
            sequenceDetailsCompressionBase = new SequenceDetailsCompressionJpeg(0, null, 0);
        }

        return new LocalSequence(sequenceId,
                sequenceDetails,
                sequenceDetailsLocal,
                sequenceDetailsCompressionBase);
    }

    /**
     * Starts the recording persistence and registers an observer for the persistence status.
     */
    private void startRecordingPersistence() {
        // If the camera API used is Camera1 and the recording mode is video the images for encoder have the same size as the preview.
        // The camera1 doesn't support to set a custom size for frames which is not depending on the preview available sizes,
        // therefore the maximum size for a frame will be of 1920x1080.
        Size videoSize;
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED) && !camera.isCamera2Api()) {
            videoSize = camera.getPreviewSize();
        } else {
            videoSize = camera.getPictureSize();
        }
        recordingPersistence.start(sequence, videoSize, camera.getImageFormat())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "startRecordingPersistence. Status: success. Message: Recording persistence started.");
                    }

                    @Override
                    public void onError(Throwable e) {
                        switch (e.getMessage()) {
                            case RecordingPersistenceStatus.STATUS_ERROR_CREATE_VIDEO_FILE:
                                mainThreadHandler.post(() -> Toast.makeText(mContext, R.string.error_creating_video_file_message, Toast.LENGTH_SHORT).show());
                                break;
                        }
                        internalStopRecording();
                    }
                });
    }

    /**
     * @return {@code true} if the recording mode is set to video compression, {@code false} otherwise.
     */
    private boolean isVideoCompression() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED);
    }

    /**
     * Stops the current upload if its uploading any frames in order to start a new recording.
     * ToDo: remove this limitation when the APP is decent. (in 2 years, lol)
     */
    private void cancelCurrentUpload() {
        uploadManager.stop();
    }

    /**
     * @param location the {@code Location} to be validated.
     * @return {@code true} if the coordinates are valid, {@code false} otherwise.
     */
    private boolean isLocationValid(Location location) {
        return location != null && location.getLatitude() != 0 && location.getLongitude() != 0;
    }

    /**
     * Copies a location to a new location object.
     * @param location the location object which will be copied.
     * @return {@code Location} representing either the copied location, {@code null} otherwise if the coordinates are both invalid or the object is null. In order to validate
     * the location the {@link #isLocationValid(Location)} is used.
     */
    @Nullable
    private Location copyLocation(@NonNull Location location) {
        if (!isLocationValid(location)) {
            return null;
        }
        Location newLocation = new Location(location.getProvider());
        newLocation.setLatitude(location.getLatitude());
        newLocation.setLongitude(location.getLongitude());
        return newLocation;
    }

    /**
     * Observe on location updates in order to update the track position.
     */
    private void observeOnLocationUpdates() {
        Log.d(TAG, "observeOnLocationUpdates");
        compositeDisposable.add(locationService.getLocationUpdates()
                .filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES))
                .subscribe(location -> {
                    Log.d(TAG, "onLocationChanged: ");
                    mActualLocation = location;
                    SensorManager.logSensorData(createDataCollectorGPSWrapperForLocation(mActualLocation));
                    if (mPreviousLocation == null) {
                        mPreviousLocation = mActualLocation;
                    }
                }));
    }

    /**
     * Takes a snapshot if we have at least one location object in the metadata file.
     * <p>
     * If there is no location object in the metadata file 30 seconds after the recording has started,
     * a location is created from {@link #mActualLocation}, and inserted in the metadata file.
     * <p>
     * This happens because there needs to be at least 1 location entry in the metadata file before the first frame
     * is saved, otherwise the server will consider the frame will have its coordinates to 0,0...
     * @param location location
     * @param dist distance
     */
    private void takeSnapshotIfLocationDataValid(@NonNull Location location, double dist) {
        if (isDiskSpaceUnavailable()) {
            Toast.makeText(mContext, R.string.reached_storage_limit, Toast.LENGTH_LONG).show();
            internalStopRecording();
            return;
        }
        if (sequence != null && positionAvailableInMetadata()) {
            takeFrame(location, dist);
        }
    }

    /**
     * Issues a frame create from {@code CameraManager}. This will reroute to the set callback on the camera manager.
     */
    private void takeFrame(@NonNull Location location, double distance) {
        if (camera == null || !recording) {
            return;
        }
        //check if a take frame operation is already started, then wait for that operation until is finished
        //before starting a new one.
        if (takePictureDisposable != null && !takePictureDisposable.isDisposed()) {
            return;
        }
        takePictureDisposable = camera.takePicture()
                .flatMapCompletable(frameData ->
                        recordingPersistence.save(new RecordingFrame(frameData,
                                location,
                                new DateTime().getMillis(),
                                distance)))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onSuccess
                        () -> {
                            //update UI with EventBuss (of course) to show data on recorder screen
                            ImageSavedEvent imageSavedEvent = new ImageSavedEvent(sequence.getDetails().getDistance(),
                                    sequence.getLocalDetails().getDiskSize(),
                                    sequence.getCompressionDetails().getLocationsCount(), location);
                            EventBus.post(imageSavedEvent);
                            imageCapturePublishSubject.onNext(imageSavedEvent);
                        },
                        //onError
                        throwable -> {
                            if (throwable instanceof TimeoutException) {
                                Log.d(TAG, "takeFrame. Status: timeout.");
                                return;
                            }
                            Log.d(TAG, String.format("takeFrame. Status: error. Error message: %s. Sequence id: %s. Video compression: %s. Recording started: %s",
                                    throwable.getMessage(), sequence.getID(), isVideoCompression(), recording));
                            if (appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED)) {
                                Toast.makeText(mContext, R.string.encoding_error_message, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, R.string.take_picture_error_message, Toast.LENGTH_SHORT).show();
                            }
                            if (throwable instanceof MediaCodec.CodecException) {
                                MediaCodec.CodecException codecException = (MediaCodec.CodecException) throwable;
                                Log.d(TAG, String.format("takeFrame. diagnostic %s ", codecException.getDiagnosticInfo()));
                                Log.d(TAG, String.format("takeFrame. isRecoverable %s ", codecException.isRecoverable()));
                                Log.d(TAG, String.format("takeFrame. isTransient %s", codecException.isTransient()));
                            } else {
                                Log.d(TAG, String.format("takeFrame. Status: error. Exception: %s %s", throwable.getMessage(), throwable.getClass().getSimpleName()));
                            }
                            internalStopRecording();
                        });
    }

    /**
     * Creates a {@link DataCollectorItemWrapper}, for a {@link GPSData}, having as the location source the location
     * received as a parameter.
     * @param mActualLocation
     * @return
     */
    private DataCollectorItemWrapper createDataCollectorGPSWrapperForLocation(@NonNull Location mActualLocation) {
        GPSData gpsData = new GPSData(LibraryUtil.PHONE_SENSOR_READ_SUCCESS);
        gpsData.setPositionObject(new PositionObject(mActualLocation.getLatitude(), mActualLocation.getLongitude()));
        if (mActualLocation.hasAccuracy()) {
            gpsData.setAccuracyObject(new AccuracyObject(mActualLocation.getAccuracy(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS));
        }
        if (mActualLocation.hasSpeed()) {
            gpsData.setSpeedObject(new OpenXcSpeedObject(mActualLocation.getSpeed(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS));
        }
        if (mActualLocation.hasAltitude()) {
            gpsData.setAltitudeObject(new AltitudeObject(mActualLocation.getAltitude(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS));
        }
        return new DataCollectorItemWrapper(gpsData);
    }

    /**
     * Checks whether at least one location entry exists in the metadata file.
     * @return {@code true} if a location entry exists in the metadata file, for the current track,
     * {@code false} otherwise.
     */
    private boolean positionAvailableInMetadata() {
        return SensorManager.isGpsDataAvailableInMetadata();
    }

    /**
     * @return {@code true} if the there is no more disk size, {@code false} otherwise.
     */
    private boolean isDiskSpaceUnavailable() {
        boolean isDiskSpaceUnavailable = Utils.getAvailableSpace(mContext) <= MIN_FREE_SPACE;
        Log.d(TAG, String.format("Disk space available: %s", isDiskSpaceUnavailable));
        return isDiskSpaceUnavailable;
    }
}