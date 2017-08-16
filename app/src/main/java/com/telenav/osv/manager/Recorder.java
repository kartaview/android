package com.telenav.osv.manager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.greenrobot.eventbus.NoSubscriberEvent;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.telenav.ffmpeg.FFMPEG;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.command.PhotoCommand;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraShutterEvent;
import com.telenav.osv.event.hardware.camera.FrameQueueEvent;
import com.telenav.osv.event.hardware.camera.ImageSavedEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.LocationEvent;
import com.telenav.osv.event.hardware.gps.TrackChangedEvent;
import com.telenav.osv.event.hardware.obd.ObdSpeedEvent;
import com.telenav.osv.event.hardware.obd.ObdStatusEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.SensorData;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.listener.ImageReadyCallback;
import com.telenav.osv.listener.ShutterCallback;
import com.telenav.osv.listener.ShutterListener;
import com.telenav.osv.manager.capture.CameraManager;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.location.ScoreManager;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.manager.shutterlogic.AutoShutterLogic;
import com.telenav.osv.manager.shutterlogic.GpsShutterLogic;
import com.telenav.osv.manager.shutterlogic.IdleShutterLogic;
import com.telenav.osv.manager.shutterlogic.ObdShutterLogic;
import com.telenav.osv.manager.shutterlogic.ShutterLogic;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * Component which handles recording related functionalities
 * Created by Kalman on 04/07/16.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Recorder implements ObdManager.ConnectionListener, LocationManager.LocationEventListener, ShutterListener, ShutterCallback {

    private static final String TAG = "Recorder";

    private static final int MIN_FREE_SPACE = 500;

    private final Context mContext;

    private final LocationManager mLocationManager;

    private final ArrayList<SKCoordinate> mCurrentTrack = new ArrayList<>();

    private final ShutterLogic mObdLogic = new ObdShutterLogic();

    private final ShutterLogic mGpsLogic = new GpsShutterLogic();

    private final ShutterLogic mAutoLogic = new AutoShutterLogic();

    private final ShutterLogic mIdleLogic = new IdleShutterLogic();

    private final ApplicationPreferences appPrefs;

    private final CameraManager mCameraManager;

    private final Object shutterSynObject = new Object();

    private ShutterLogic mCurrentLogic = mIdleLogic;

    private ObdManager mOBDManager;

    private Location mPreviousLocation;

    private Location mActualLocation;

    private ScoreManager mScoreManager;

    private boolean mMapEnabled;

    private boolean mReceivedQualityPosition = false;

    private ThreadPoolExecutor mThreadPoolExec;

    private LocalSequence mSequence;

    private String mSequencePath;

    private int mIndex = 0;

    private boolean recording;

    private UploadManager mUploadManager;

    private SensorManager mSensorManager;

    private int mOrientation = 0;

    private boolean mCameraIdle = true;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {
            mCameraIdle = true;
        }
    };

    private FFMPEG ffmpeg;

    private boolean mSafe;

    private ImageReadyCallback mJpegPictureCallback = new ImageReadyCallback() {
        @Override
        public void onPictureTaken(final byte[] jpegData, long timestamp, int sequenceId, String folderPath, Location location) {
            mHandler.removeCallbacks(mIdleRunnable);
            if (!recording) {
                mCameraIdle = true;
                return;
            }
            final boolean mSafeF = mSafe;
            final int mOrientationF = mOrientation;
            mCameraIdle = true;

            saveFrame(mSafeF, jpegData, sequenceId, folderPath, location, mOrientationF, timestamp);
        }
    };

    public Recorder(OSVApplication app) {
        mContext = app;
        int coreNum = 1;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        mThreadPoolExec = new ThreadPoolExecutor(
                coreNum,
                coreNum,
                7,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadFactoryBuilder().setDaemon(false).setNameFormat("Recorder-pool-%d").setPriority(Thread.MAX_PRIORITY).build());
        mCameraManager = CameraManager.get(app);
        mUploadManager = ((OSVApplication) mContext).getUploadManager();
        mSensorManager = new SensorManager(mContext);
        appPrefs = app.getAppPrefs();
        mScoreManager = new ScoreManager(app, appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));
        mLocationManager = LocationManager.get(app, this);
        mLocationManager.connect();
        int obdType = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE);
        createObdManager(obdType);
        EventBus.register(this);
        mMapEnabled = !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED);
        logicPrioritiesChanged();
    }

    @Override
    public void onLocationChanged(Location location, boolean shouldCenter) {
        if (location == null) {
            return;
        }
        Log.d(TAG, "onLocationChanged: ");
        mActualLocation = location;
        if (mPreviousLocation == null) {
            mPreviousLocation = mActualLocation;
        }
        EventBus.postSticky(new LocationEvent(mActualLocation, shouldCenter));
        if (mMapEnabled) {
            offerNewTrackPosition(mActualLocation);
        }
        if (mScoreManager != null && appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
            mScoreManager.onLocationChanged(mActualLocation);
        }
        mCurrentLogic.onLocationChanged(mPreviousLocation, mActualLocation);
    }

    @Override
    public void onLocationTimedOut() { // this is redundant as
        Log.d(TAG, "onLocationTimedOut: recording active = " + isRecording());
    }

    @Override
    public void onGpsAccuracyChanged(int type) {
        mReceivedQualityPosition = isRecording() && (mReceivedQualityPosition || type != LocationManager.ACCURACY_BAD);
        if (mReceivedQualityPosition) {
            mAutoLogic.setFunctional(true);
        }
        if (type == LocationManager.ACCURACY_BAD) {
            mGpsLogic.setFunctional(false);
        } else {
            mGpsLogic.setFunctional(true);
        }
        logicPrioritiesChanged();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStateChanged(RecordingEvent event) {
        mMapEnabled = !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED);
        if (event.started) {
            mReceivedQualityPosition = false;
            mAutoLogic.stop();
            mAutoLogic.setFunctional(false);
            logicPrioritiesChanged();
            if (mMapEnabled) {
                synchronized (mCurrentTrack) {
                    mCurrentTrack.clear();
                    SKPosition position = SKPositionerManager.getInstance().getCurrentGPSPosition(true);
                    mCurrentTrack.add(position.getCoordinate());
                }
            }
            if (mActualLocation != null && appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
                mScoreManager.onLocationChanged(mActualLocation);
            }
        } else {
            mAutoLogic.stop();
            mAutoLogic.setFunctional(false);
            logicPrioritiesChanged();
            EventBus.clear(TrackChangedEvent.class);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onGpsCommand(GpsCommand event) {
        if (!event.start) {
            mAutoLogic.stop();
        }
    }

    private void logicPrioritiesChanged() {
        ShutterLogic appropriate = null;
        if (mObdLogic.betterThan(mCurrentLogic)) {
            appropriate = mObdLogic;
        } else if (mGpsLogic.betterThan(mCurrentLogic)) {
            appropriate = mGpsLogic;
        } else if (mAutoLogic.betterThan(mCurrentLogic)) {
            appropriate = mAutoLogic;
        } else if (mIdleLogic.betterThan(mCurrentLogic)) {
            appropriate = mIdleLogic;
        }
        if (appropriate != null) {
            mCurrentLogic.stop();
            mCurrentLogic.setShutterListener(null);
            mCurrentLogic = appropriate;
            mCurrentLogic.setShutterListener(this);
            Log.d(TAG, "logicPrioritiesChanged: new logic = " + mCurrentLogic.getClass().getSimpleName());
            mCurrentLogic.start();
        }
    }

    @Override
    public void onSpeedObtained(SpeedData speedData) {
        try {
            if (speedData.getSpeed() != -1) {
                if (!mObdLogic.isFunctional()) {
                    mObdLogic.setFunctional(true);
                    logicPrioritiesChanged();
                }
            }
            mCurrentLogic.onSpeedChanged(speedData);
            EventBus.post(new ObdSpeedEvent(speedData));

            if (mSequence != null && !mSequence.hasObd()) {
                mSequence.setHasObd(true);
                SequenceDB.instance.setOBDForSequence(mSequence.getId(), true);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "onSpeedObtained: " + Log.getStackTraceString(e));
        }
    }

    @Override
    public void onObdConnected() {
        mObdLogic.setFunctional(true);
        logicPrioritiesChanged();
        mOBDManager.startRunnable();
        EventBus.postSticky(new ObdStatusEvent(ObdStatusEvent.TYPE_CONNECTED));
    }

    @Override
    public void onObdDisconnected() {
        mObdLogic.setFunctional(false);
        logicPrioritiesChanged();
        mOBDManager.stopRunnable();
        EventBus.postSticky(new ObdStatusEvent(ObdStatusEvent.TYPE_DISCONNECTED));
    }

    @Override
    public void onObdConnecting() {
        EventBus.postSticky(new ObdStatusEvent(ObdStatusEvent.TYPE_CONNECTING));
    }

    @Override
    public void onObdDataTimedOut() {
        Log.d(TAG, "onObdDataTimedOut: ");
        mObdLogic.setFunctional(false);
        EventBus.post(new ObdSpeedEvent(new SpeedData("TIMEOUT")));
    }

    public void setCameraPreviewSurface(SurfaceTexture surface) {
        mCameraManager.setPreviewSurface(surface);
    }

    public void openCamera() {
        mCameraManager.open();
    }

    public void closeCamera() {
        mCameraManager.release();
    }

    public ObdManager getOBDManager() {
        return mOBDManager;
    }

    public void createObdManager(int type) {
        if (mOBDManager != null) {
            mOBDManager.unregisterReceiver();
            EventBus.unregister(mOBDManager);
            mOBDManager.removeConnectionListener();
            mOBDManager.destroy();
        }
        switch (type) {
            case PreferenceTypes.V_OBD_BLE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BLE, this);
                } else {
                    appPrefs.saveIntPreference(PreferenceTypes.K_OBD_TYPE, PreferenceTypes.V_OBD_BT);
                    mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BT, this);
                }
                break;
            case PreferenceTypes.V_OBD_BT:
                mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BT, this);
                break;
            case PreferenceTypes.V_OBD_WIFI:
            default:
                mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_WIFI, this);
                break;
        }
        EventBus.register(mOBDManager);
        mOBDManager.registerReceiver();
        mOBDManager.setConnectionListener(this);
    }

    public void connectLocation() {
        mLocationManager.connect();
    }

    public void disconnectLocation() {
        mLocationManager.disconnect();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPhotoCommand(PhotoCommand command){
        if (isRecording()) {
            if (mLocationManager != null && hasPosition()) {
                try {
                    double dist = ComputingDistance.distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(), mActualLocation.getLongitude(),
                            mActualLocation.getLatitude());
                    takeSnapshot(mActualLocation, dist);
                } catch (Exception e) {
                    Log.d(TAG, "takePhoto: " + Log.getStackTraceString(e));
                }
            }
        }
    }

    public boolean hasPosition() {
        return mPreviousLocation != null && mActualLocation != null;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void noSubscriber(NoSubscriberEvent event) {

    }

    private void onImageSaved(boolean success, LocalSequence sequence, Location location) {
        EventBus.post(new FrameQueueEvent(mThreadPoolExec.getQueue().size()));
        if (success) {
            mScoreManager.onPictureTaken(location);
        }
        EventBus.post(new ImageSavedEvent(sequence, success));
    }

    @Override
    public void takeSnapshot(float distance) {
        if (isRecording() && mActualLocation != null && mActualLocation.getLatitude() != 0 && mActualLocation.getLongitude() != 0) {
            takeSnapshot(mActualLocation, distance);
            mPreviousLocation = mActualLocation;
        }
    }

    public void startRecording() {
        if (mUploadManager != null && mUploadManager.isUploading()) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mUploadManager.cancelUploadTasks();
                }
            });
        }
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {

                mCameraIdle = true;
                recording = true;
                mSafe = appPrefs.getBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false);
                mSequence = SequenceDB.instance.createNewSequence(mContext
                        , -1
                        , -1
                        , false //no 360 cam yet
                        , appPrefs.getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE)
                        , OSVApplication.VERSION_NAME
                        , false, mSafe);
                if (mSequence == null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, R.string.filesystem_error_message, Toast.LENGTH_LONG).show();
                        }
                    });
                    recording = false;
                    return;
                }
                mSequencePath = mSequence.getFolder().getPath();
                if (mSequence == null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, R.string.error_creating_folder_message, Toast.LENGTH_SHORT).show();
                        }
                    });
                    recording = false;
                    return;
                }
                if (!mSafe) {
                    try {
                        ffmpeg = new FFMPEG(new FFMPEG.ErrorListener() {
                            @Override
                            public void onError() {
                                int value = appPrefs.getIntPreference(PreferenceTypes.K_FFMPEG_CRASH_COUNTER);
                                appPrefs.saveIntPreference(PreferenceTypes.K_FFMPEG_CRASH_COUNTER, value + 1);
                                Log.d(TAG, "onError: FFMPEG crashed, counter is at " + (value + 1));
                            }
                        });
                    } catch (ExceptionInInitializerError e) {
                        Log.w(TAG, "startRecording: " + Log.getStackTraceString(e));
                        if (Fabric.isInitialized()) {
                            Answers.getInstance().logCustom(new CustomEvent("FFMPEG object creation failed."));
                        }
                        Log.e(TAG, "startRecording: could not init ffmpeg");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, R.string.ffmpeg_init_error_message, Toast.LENGTH_SHORT).show();
                            }
                        });
                        stopRecording();
                        return;
                    } catch (NoClassDefFoundError | UnsatisfiedLinkError e) {
                        Log.e(TAG, "startRecording: could not init ffmpeg");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, R.string.ffmpeg_init_error_message, Toast.LENGTH_SHORT).show();
                            }
                        });
                        stopRecording();
                        return;
                    }
                    int ret = -1;
                    try {
                        ret = ffmpeg.initial(mSequence.getFolder().getPath() + "/");
                    } catch (Exception ignored) {}
                    if (ret != 0) {
                        Log.e(TAG, "startRecording: could not create video file");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, R.string.error_creating_video_file_message, Toast.LENGTH_SHORT).show();
                            }
                        });
                        stopRecording();
                        return;
                    }
                }
                if (mSequence == null || mSequence.getFolder() == null) {
                    Log.e(TAG, "startRecording: could not create video file");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, R.string.error_creating_video_file_message, Toast.LENGTH_SHORT).show();
                        }
                    });
                    stopRecording();
                    return;
                }
                EventBus.postSticky(new RecordingEvent(mSequence, true));
                synchronized (shutterSynObject) {
                    mIndex = 0;
                }

                OSVFile folder = null;
                if (mSequence == null || mSequence.getFolder() == null) {
                    if (mSequencePath != null) {
                        folder = new OSVFile(mSequencePath);
                    }
                } else {
                    folder = mSequence.getFolder();
                }

                mSensorManager.onResume(folder, mSafe);
                if (Fabric.isInitialized()) {
                    Crashlytics.setBool(Log.RECORD_STATUS, true);
                }
            }
        });
    }

    public void stopRecording() {
        stopRecording(false);
    }

    public void stopRecording(boolean synchronously) {
        if (!recording) {
            return;
        }
        recording = false;
        final LocalSequence finalSequence = mSequence;
        EventBus.clear(RecordingEvent.class);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mSequence != null && SequenceDB.instance.getNumberOfFrames(mSequence.getId()) <= 0) {
                    SequenceDB.instance.deleteRecords(mSequence.getId());
                    if (mSequence.getFolder() != null) {
                        mSequence.getFolder().delete();
                    }
                }
                synchronized (shutterSynObject) {
                    mIndex = 0;
                }
                if (finalSequence != null) {
                    SequenceDB.instance.updateSequenceFrameCount(finalSequence.getId());
                    finalSequence.refreshStats();
                    if (Fabric.isInitialized()) {
                        int userType = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
                        if (Fabric.isInitialized()) {
                            try {
                                Answers.getInstance().logCustom(new CustomEvent("Recorded track")
                                        .putCustomAttribute("images", finalSequence.getFrameCount())
                                        .putCustomAttribute("obd", "" + finalSequence.hasObd())
                                        .putCustomAttribute("userType", userType)
                                        .putCustomAttribute("length", finalSequence.getDistance())
                                );
                            } catch (Exception ignored) {}
                        }
                    }
                }
                mSensorManager.onPauseOrStop();
                if (!mSafe) {
                    if (ffmpeg != null) {
                        int ret = ffmpeg.close();
                        Log.d(TAG, "run: ffmpeg close: " + ret);
                    }
                }

                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC)) {
                    mCameraManager.unlockFocus();
                }
                if (finalSequence == null) {
                    EventBus.post(new RecordingEvent(null, false));
                    return;
                }
                if (SequenceDB.instance.getNumberOfFrames(finalSequence.getId()) <= 0) {
                    LocalSequence.deleteSequence(finalSequence.getId());
                    if (finalSequence.getFolder() != null) {
                        finalSequence.getFolder().delete();
                    }
                    Log.d(TAG, "stopRecording: deleted sequence");
                }
                mSequence = null;
                mSequencePath = null;
                Log.d(TAG, "stopRecording: Stopped recording");
                if (Fabric.isInitialized()) {
                    Crashlytics.setBool(Log.RECORD_STATUS, false);
                }
                EventBus.post(new RecordingEvent(finalSequence, false));
                UploadManager.scheduleAutoUpload(mContext);
            }
        };
        if (synchronously) {
            runnable.run();
        } else {
            mThreadPoolExec.execute(runnable);
        }
    }

    public boolean isRecording() {
        return recording;
    }

    private void offerNewTrackPosition(Location location) {
        SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(location));
        if (isRecording()) {
            SKPosition position = SKPositionerManager.getInstance().getCurrentGPSPosition(true);
            synchronized (mCurrentTrack) {
                mCurrentTrack.add(position.getCoordinate());
                Log.d(TAG, "onLocationChanged: track size is " + mCurrentTrack.size());
            }
            EventBus.postSticky(new TrackChangedEvent(mCurrentTrack));
        }
    }

    /**
     * takes a snapshot
     * exposure value, otherwise set it to 0
     * @param location location
     * @param dist distance
     */
    private void takeSnapshot(Location location, double dist) {
        if (mCameraIdle && mSequence != null) {
            mCameraIdle = false;
            mHandler.removeCallbacks(mIdleRunnable);
            mHandler.postDelayed(mIdleRunnable, 2000);
            mSequence.setTotalLength(mSequence.getTotalLength() + dist);
            if (mIndex == 0) {
                mSequence.setLocation(new SKCoordinate(location.getLatitude(), location.getLongitude()));
                SequenceDB.instance.updateSequenceLocation(mSequence.getId(), location.getLatitude(), location.getLongitude());
            }
            mOrientation = (int) Math.toDegrees(SensorManager.mHeadingValues[0]);
            long timestamp = System.currentTimeMillis();
            mCameraManager.takeSnapshot(this, mJpegPictureCallback, timestamp, mSequence.getId(), mSequence.getFolder().getPath(), location);

        }
    }


    private void saveFrame(final boolean safe, final byte[] jpegData, final int sequenceId,
                           final String folderPath, final Location location,
                           final int orientation, final long timestamp) {
        Log.d(TAG, "saveFrame: posting frame data to handler");

        if (mUploadManager != null && mUploadManager.isUploading()) {
            mThreadPoolExec.execute(new Runnable() {
                @Override
                public void run() {
                    mUploadManager.cancelUploadTasks();
                }
            });
        }
        mThreadPoolExec.execute(new Runnable() {
            @Override
            public void run() {
                int available = (int) Utils.getAvailableSpace(mContext);
                Log.d(TAG, "saveFrame: entered data handler");
                if (available <= MIN_FREE_SPACE) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean needToRestart = false;
                            try {
                                if (Utils.checkSDCard(mContext)) {
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE
                                            , !appPrefs.getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE));
                                    needToRestart = true;
                                    Toast.makeText(mContext, R.string.reached_current_storage_limit_message, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(mContext, R.string.reached_storage_limit, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "saveFrame: minimum space reached" + Log.getStackTraceString(e));
                                Toast.makeText(mContext, R.string.reached_storage_limit, Toast.LENGTH_LONG).show();
                            }
                            stopRecording();
                            if (needToRestart) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startRecording();
                                    }
                                }, 1500);
                            }
                        }
                    });
                    return;
                }
                if (jpegData == null) {
                    Log.w(TAG, "saveFrame: jpegData is null");
                    return;
                }
                final long time = System.currentTimeMillis();
                if (safe) {
                    String path;
                    if (folderPath != null) {
                        path = folderPath + "/" + mIndex + ".jpg";
                    } else {
                        String folder = Utils.generateOSVFolder(mContext).getPath() + "/SEQ_" + sequenceId;
                        path = folder + "/" + mIndex + ".jpg";
                        if (mSequence == null) {
                            mSequence = new LocalSequence(new OSVFile(folder));
                        }
                    }
                    String tmpPath = path + ".tmp";
                    FileOutputStream out;
                    try {
                        out = new FileOutputStream(tmpPath);
                        out.write(jpegData);
                        out.close();
                        OSVFile jpg = new OSVFile(path);
                        OSVFile tmpFile = new OSVFile(tmpPath);
                        tmpFile.renameTo(jpg);
                        Log.v(TAG, "Saved JPEG data : " + jpg.getName() + ", size: " + ((float) jpg.length()) / 1024f / 1024f + " mb");

                        try {
                            SequenceDB.instance.insertPhoto(sequenceId, -1, mIndex, jpg.getPath(), location.getLatitude(),
                                    location.getLongitude(), location.getAccuracy(), orientation);

                            SensorManager.logSensorData(new SensorData(mIndex, 0, timestamp));
                            SensorManager.flushToDisk();
                            synchronized (shutterSynObject) {
                                mIndex++;
                            }
                        } catch (SQLiteConstraintException e) {
                            jpg.delete();
                            Log.w(TAG, "saveFrame: " + Log.getStackTraceString(e));
                            stopRecording();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startRecording();
                                }
                            }, 1500);

                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write image", e);
                        onImageSaved(false, mSequence, location);
                        return;
                    }
                } else {
                    int[] ret = ffmpeg.encode(jpegData);
                    Log.d(TAG, "saveFrame: encoding done in " + (System.currentTimeMillis() - time)
                            + " ms ,  video file " + ret[0] + " and frame " + ret[1]);
                    if (ret[0] < 0 || ret[1] < 0) {
//                    synchronized (shutterSynObject) {
//                        mIndex--;
//                    }
                        onImageSaved(false, mSequence, location);
                        if (ret[0] < 0) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, R.string.encoding_error_message, Toast.LENGTH_SHORT).show();
                                }
                            });
                            stopRecording();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startRecording();
                                }
                            }, 1500);
                        }
                        return;
                    }

                    int mVideoIndexF = ret[0];
                    SensorManager.logSensorData(new SensorData(mIndex, mVideoIndexF, timestamp));
                    SensorManager.flushToDisk();
                    try {
                        SequenceDB.instance.insertVideoIfNotAdded(sequenceId, mVideoIndexF, folderPath + "/" + mVideoIndexF + ".mp4");
                    } catch (Exception ignored) {}

                    try {
                        SequenceDB.instance.insertPhoto(sequenceId, mVideoIndexF, mIndex, folderPath + "/" + mVideoIndexF + ".mp4", location.getLatitude(),
                                location.getLongitude(), location.getAccuracy(), orientation);
                        synchronized (shutterSynObject) {
                            mIndex++;
                        }
                    } catch (final SQLiteConstraintException e) {
                        Log.w(TAG, "saveFrame: " + Log.getStackTraceString(e));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                if (Fabric.isInitialized()) {
                                    Answers.getInstance().logCustom(new CustomEvent("SQLiteConstraintException at insert photo"));
                                }
                                stopRecording();
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startRecording();
                                    }
                                }, 1500);
                            }
                        });

                    }
                }
                mSequence.setFrameCount(mSequence.getFrameCount() + 1);
                onImageSaved(true, mSequence, location);
            }
        });
    }

    public List<Size> getSupportedPicturesSizes() {
        return mCameraManager.getSupportedPictureSizes();
    }

    public void forceCloseCamera() {
        mCameraManager.forceCloseCamera();
    }

    /**
     * focus on the coordinates
     * @param x x coord on a 1000x1000 grid
     * @param y y coord on a 1000x1000 grid
     */
    public void focusOnArea(int x, int y) {
        mCameraManager.focusOnTouch(x, y);
    }

    @Override
    public void onShutter() {
        EventBus.post(new CameraShutterEvent());
    }
}
