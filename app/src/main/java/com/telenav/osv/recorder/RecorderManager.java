package com.telenav.osv.recorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.PhotoCommand;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.gpsTrail.GpsTrailHelper;
import com.telenav.osv.recorder.gpsTrail.ListenerRecordingGpsTrail;
import com.telenav.osv.recorder.metadata.MetadataSensorManager;
import com.telenav.osv.recorder.metadata.callback.MetadataWrittingStatusCallback;
import com.telenav.osv.recorder.persistence.RecordingFrame;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.persistence.RecordingPersistenceStatus;
import com.telenav.osv.recorder.shutter.Shutter;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.utils.ExtensionsKt;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
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
public class RecorderManager implements ObdConnectionListener, MetadataWrittingStatusCallback {

    public static final int UNKNOWN_USER_TYPE = -1;

    private static final String TAG = RecorderManager.class.getSimpleName();

    private static final int MIN_FREE_SPACE = 500;

    private final Context mContext;

    private final ApplicationPreferences appPrefs;

    private Shutter shutterManager;

    private Camera camera;

    private ObdManager mOBDManager;

    private ThreadPoolExecutor mThreadPoolExec;

    private LocalSequence sequence;

    private boolean recording;

    private UploadManager uploadManager;

    private MetadataSensorManager metadataSensorManager;

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
     * The {@code LocationService} instance for gps trail location updates.
     */
    private GpsTrailHelper gpsTrailHelper;

    public RecorderManager(@NonNull KVApplication app,
                           @NonNull UserDataSource userLocalDataSource,
                           @NonNull SequenceLocalDataSource sequenceLocalDataSource,
                           @NonNull ObdManager obdManager,
                           @NonNull Shutter shutterManager,
                           @NonNull MetadataSensorManager metadataSensorManager,
                           @NonNull GpsTrailHelper gpsTrailHelper) {
        mContext = app;
        this.userLocalDataSource = userLocalDataSource;
        this.mOBDManager = obdManager;
        this.shutterManager = shutterManager;
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        this.metadataSensorManager = metadataSensorManager;
        this.gpsTrailHelper = gpsTrailHelper;
        int coreNum = 1;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        mThreadPoolExec = new ThreadPoolExecutor(coreNum, coreNum, 7, TimeUnit.SECONDS, workQueue,
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("Recorder-pool-%d")
                        .setPriority(Thread.MAX_PRIORITY).build());
        uploadManager = Injection.provideUploadManager();
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
    public void onMetadataCreated() {
        //start recording if every check from above had passed
        Log.d(TAG, "startRecording");
        recordingEventPublishSubject.onNext(true);
        metadataSensorManager.onDeviceLog(
                appPrefs.getLongPreference(PreferenceTypes.K_RECORD_START_TIME),
                "Android",
                Utils.getOSCodeName(),
                Build.VERSION.RELEASE,
                Build.MANUFACTURER + " " + Build.MODEL,
                KVApplication.VERSION_NAME,
                Utils.getAppVersion(),
                isVideoCompression());
        gpsTrailHelper.start(null);
        //issue to log the camera sensor data
        camera.getCameraSensorDataAsync(metadataSensorManager);
        metadataSensorManager.start();
        //updates flag in crashlytics that the recording has started
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey(Log.RECORD_STATUS, true);
        startRecordingPersistence();
    }

    public void setListenerRecordingGpsTrail(ListenerRecordingGpsTrail listenerRecordingGpsTrail) {
        gpsTrailHelper.setGpsTrailListener(listenerRecordingGpsTrail);
    }

    public void removeListenerRecordingGpsTrail(ListenerRecordingGpsTrail listenerRecordingGpsTrail) {
        gpsTrailHelper.removeListenerGpsTrail(listenerRecordingGpsTrail);
    }

    @Override
    public void onMetadataLoggingError(Exception e) {
        Log.d(TAG, "onMetadataLoggingError. Status:" + e.getMessage());
        if (isRecording()) {
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(mContext);
            Bundle bundle = new Bundle();
            bundle.putInt("sequence_frames", sequence.getCompressionDetails().getLocationsCount());
            bundle.putString("device_model", Build.MODEL);
            analytics.logEvent("metadata_logging_error", bundle);
            Log.d(TAG, "onMetadataLoggingError. Status: Recording in progress, stopping the recording.");
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

    /**
     * @return a {@code Observable} in order to subscribe for receiving location accuracy updates.
     */
    public Observable<Integer> getAccuracyType() {
        return shutterManager.getAccuracyType();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoCommand(PhotoCommand command) {
        if (isRecording()) {
            Location location = shutterManager.getCurrentLocationForManualTakeImage();
            Log.d(TAG, "manual photo. Location: " + location);
            if (isLocationValid(location)) {
                try {
                    takeSnapshotIfLocationDataValid(location, 0);
                } catch (Exception e) {
                    Log.d(TAG, "takePhoto: " + Log.getStackTraceString(e));
                }
            }
        }
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
                        if (appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION)) {
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
            KVFile parentFolder = sequence.getLocalDetails().getFolder();
            if (parentFolder == null
                    || !parentFolder.exists()
                    || !persistSequence) {
                mainThreadHandler.post(() -> Toast.makeText(mContext, R.string.record_failed, Toast.LENGTH_LONG).show());
                recording = false;
                internalStopRecording();
                return;
            }
            appPrefs.saveStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID, sequence.getID());
            //set the metadata listener as this
            metadataSensorManager.setListener(this);
            //notify sensor manager that a video recording has started and should resume sensor data gathering in case was paused.
            metadataSensorManager.create(sequence.getLocalDetails().getFolder(), mContext);
            //log the device on which the data is being taken
        });
    }

    /**
     * @return {@code OSVFile} which represents the current sequence folder.
     */
    @Nullable
    public KVFile getFolder() {
        if (sequence != null) {
            return sequence.getLocalDetails().getFolder();
        }

        return null;
    }

    /**
     * @return {@code String} representing the current sequence identifier if recording is on progress.
     */
    @Nullable
    public String getCurrentSequenceId() {
        if (sequence != null) {
            return sequence.getID();
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
                        .subscribe(result -> {
                            //check if the status is recording and the location is valid, the validity of location is for a redundant fail-safe
                            if (isRecording() && isLocationValid(result.second)) {
                                takeSnapshotIfLocationDataValid(result.second, result.first);
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
                KVFile sequenceFolder = sequenceDetailsLocal.getFolder();
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

                FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
                crashlytics.setCustomKey(Log.RECORD_STATUS, false);
                userLocalDataSource
                        .getUser()
                        .subscribe(
                                user -> {
                                    Log.d(TAG, "getStopRecordingRunnable getUser. Status: success. Message: User found.");
                                    SequenceDetails sequenceDetails = sequence.getDetails();
                                    FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(mContext);
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("images", sequence.getCompressionDetails().getLocationsCount());
                                    bundle.putBoolean("obd", sequenceDetails.isObd());
                                    bundle.putInt("user_type", user.getUserType());
                                    bundle.putDouble("length", sequenceDetails.getDistance());
                                    analytics.logEvent("recorded_track", bundle);
                                },
                                throwable -> Log.d(TAG, String.format("getStopRecordingRunnable getUser. Status: error. Message: %s.", throwable.getLocalizedMessage())),
                                () -> {
                                    Log.d(TAG, "getStopRecordingRunnable getUser. Status: complete. Message: User not found.");
                                    SequenceDetails sequenceDetails = sequence.getDetails();
                                    FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(mContext);
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("images", sequence.getCompressionDetails().getLocationsCount());
                                    bundle.putBoolean("obd", sequenceDetails.isObd());
                                    bundle.putInt("user_type", UNKNOWN_USER_TYPE);
                                    bundle.putDouble("length", sequenceDetails.getDistance());
                                    analytics.logEvent("recorded_track", bundle);
                                }
                        );
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
            metadataSensorManager.stop();
            gpsTrailHelper.stop(null);
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
                KVApplication.VERSION_NAME,
                new DateTime());
        sequenceDetails.setObd(mOBDManager.getObdState() == ObdManager.ObdState.OBD_CONNECTED);

        KVFile kvFile = new KVFile(Utils.generateOSVFolder(mContext).getPath(), "/SEQ_" + sequenceId);
        try {
            kvFile.mkdir();
        } catch (Exception exception) {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.recordException(exception);
        }
        SequenceDetailsLocal sequenceDetailsLocal = new SequenceDetailsLocal(kvFile, 0, SequenceDetailsLocal.SequenceConsistencyStatus.VALID);

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
        recordingPersistence.start(sequence, ExtensionsKt.getResolution(camera), camera.getImageFormat())
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
        newLocation.setTime(location.getTime());
        return newLocation;
    }

    /**
     * Takes a snapshot if we have at least one location object in the metadata file.
     * @param location location
     * @param dist distance
     */
    private void takeSnapshotIfLocationDataValid(@NonNull Location location, double dist) {
        if (isDiskSpaceUnavailable()) {
            Toast.makeText(mContext, R.string.reached_storage_limit, Toast.LENGTH_LONG).show();
            internalStopRecording();
            return;
        }
        if (sequence != null) {
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
     * @return {@code true} if the there is no more disk size, {@code false} otherwise.
     */
    private boolean isDiskSpaceUnavailable() {
        boolean isDiskSpaceUnavailable = Utils.getAvailableSpace(mContext) <= MIN_FREE_SPACE;
        Log.d(TAG, String.format("Disk space available: %s", isDiskSpaceUnavailable));
        return isDiskSpaceUnavailable;
    }
}