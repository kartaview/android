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
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.telenav.datacollectormodule.datatype.datatypes.AccuracyObject;
import com.telenav.datacollectormodule.datatype.datatypes.GPSData;
import com.telenav.datacollectormodule.datatype.datatypes.PositionObject;
import com.telenav.datacollectormodule.datatype.datatypes.SpeedObject;
import com.telenav.datacollectormodule.datatype.util.LibraryUtil;
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
import com.telenav.osv.item.AccountData;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.item.metadata.DataCollectorItemWrapper;
import com.telenav.osv.item.metadata.VideoData;
import com.telenav.osv.item.network.PayRateData;
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
import com.telenav.osv.ui.fragment.ProfileFragment;
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

    private static final int RECORD_TIME_START_NOT_SET = -1;

    private static final String TAG = "Recorder";

    private static final int MIN_FREE_SPACE = 500;

    private static final int MAX_WAIT_TIME_FOR_LOCATION_MS = 30000;

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

    private final SharedPreferences profilePrefs;

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

    private Runnable mIdleRunnable = () -> mCameraIdle = true;

    private FFMPEG ffmpeg;

    private boolean mSafe;

    private long timeOfRecordingStart = -1;

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
        profilePrefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
        int coreNum = 1;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        mThreadPoolExec = new ThreadPoolExecutor(coreNum, coreNum, 7, TimeUnit.SECONDS, workQueue,
                new ThreadFactoryBuilder().setDaemon(false).setNameFormat("Recorder-pool-%d")
                        .setPriority(Thread.MAX_PRIORITY).build());
        mCameraManager = CameraManager.get(app);
        mUploadManager = ((OSVApplication) mContext).getUploadManager();
        mSensorManager = new SensorManager(mContext);
        appPrefs = app.getAppPrefs();
        initScoreManager(app);
        mLocationManager = LocationManager.get(app, this);
        mLocationManager.connect();
        int obdType = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE);
        Log.d(TAG, "OBD TYPE: " + obdType);
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
        notifyScoreManagerOfLocation(mActualLocation);
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

    @Override
    public void requestTakeSnapshot(float distance) {
        if (isRecording() && mActualLocation != null && mActualLocation.getLatitude() != 0 && mActualLocation.getLongitude() != 0) {
            takeSnapshotIfLocationDataValid(mActualLocation, distance);
            mPreviousLocation = mActualLocation;
        }
    }

    @Override
    public void onShutter() {
        EventBus.post(new CameraShutterEvent());
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
            notifyScoreManagerOfLocation(mActualLocation);
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
                mOBDManager = ObdManager.get(mContext, ObdManager.TYPE_BLE, this);
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
    public void onPhotoCommand(PhotoCommand command) {
        if (isRecording()) {
            if (mLocationManager != null && hasPosition()) {
                try {
                    double dist = ComputingDistance
                            .distanceBetween(mPreviousLocation.getLongitude(), mPreviousLocation.getLatitude(), mActualLocation.getLongitude(),
                                    mActualLocation.getLatitude());
                    takeSnapshotIfLocationDataValid(mActualLocation, dist);
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
                mSequence = SequenceDB.instance.createNewSequence(mContext, -1, -1, false //no 360 cam yet
                        , appPrefs.getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE), OSVApplication.VERSION_NAME, false, mSafe);
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
                    } catch (Exception ignored) {
                    }
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
                timeOfRecordingStart = System.currentTimeMillis();
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
        Runnable runnable = () -> {

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
                            Answers.getInstance().logCustom(
                                    new CustomEvent("Recorded track")
                                            .putCustomAttribute("images", finalSequence.getFrameCount())
                                            .putCustomAttribute("obd", "" + finalSequence.hasObd())
                                            .putCustomAttribute("userType", userType)
                                            .putCustomAttribute("length", finalSequence.getDistance()));

                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            mSensorManager.onPauseOrStop();
            setTimeOfRecordingStart(RECORD_TIME_START_NOT_SET);
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

    public void setByodPayRateData(PayRateData payRateData) {
        mScoreManager.setPayRateData(payRateData);
    }

    public boolean isPayRateDataAvailable() {
        return mScoreManager.getPayRateData() != null;
    }

    public void configureForUser(AccountData userAccount) {
        if (userAccount.isDriver()) {
            if (profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10).equals(ProfileFragment
                    .PAYMENT_MODEL_VERSION_20)) {
                mScoreManager.setEnabledForByodDriver(true);
            } else {
                mScoreManager.setEnabledForByodDriver(false);
            }
            mScoreManager.setEnabledForNormalUser(false);
        } else {
            mScoreManager.setEnabledForNormalUser(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION));
            mScoreManager.setEnabledForByodDriver(false);
        }
    }

    public void configureForLoggedOutUser() {
        mScoreManager.setEnabledForByodDriver(false);
        mScoreManager.setEnabledForNormalUser(false);
        mScoreManager.setPayRateData(null);
    }

    private synchronized long getTimeOfRecordingStart() {
        return this.timeOfRecordingStart;
    }

    private synchronized void setTimeOfRecordingStart(long newTimeOfRecordingStart) {
        this.timeOfRecordingStart = newTimeOfRecordingStart;
    }

    private void initScoreManager(OSVApplication app) {
        mScoreManager = new ScoreManager(app);
        mScoreManager.setEnabledForNormalUser(appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true));

        String paymentModel = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10);
        boolean isDriver = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, PreferenceTypes.USER_TYPE_UNKNOWN) == PreferenceTypes.USER_TYPE_BYOD;
        boolean isPaymentModel20 = ProfileFragment.PAYMENT_MODEL_VERSION_20.equals(paymentModel);
        boolean enablePayRateForByod = isPaymentModel20 && isDriver;

        Log.d(TAG, "paymentModel: " + paymentModel + "\n" +
                "isDriver: " + isDriver + "\n" +
                "isPaymentModel20: " + isPaymentModel20 + "\n" +
                "enablePayRateForByod: " + enablePayRateForByod + "\n" +
                "enabledForNormalUser: " + appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)
        );

        mScoreManager.setEnabledForByodDriver(enablePayRateForByod);
    }

    private void notifyScoreManagerOfLocation(Location location) {
        if (mScoreManager != null && location != null) {
            mScoreManager.onLocationChanged(location);
        }
    }

    private void logicPrioritiesChanged() {
        Log.d(TAG, "logicPrioritiesChanged: current logic = " + mCurrentLogic.getClass().getSimpleName());
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

    private void onImageSaved(boolean success, LocalSequence sequence, Location location) {
        EventBus.post(new FrameQueueEvent(mThreadPoolExec.getQueue().size()));
        if (success) {
            mScoreManager.onPictureTaken(location);
        }
        EventBus.post(new ImageSavedEvent(sequence, success));
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
        if (mCameraIdle && mSequence != null) {
            if (positionAvailableInMetadata()) {
                takeSnapshot(location, dist);
            } else {
                Log.d(TAG, "Tried to save a photo without a location being available in the metadata file.");
                long recordingStartTime = getTimeOfRecordingStart();
                if (recordingStartTime != RECORD_TIME_START_NOT_SET && System.currentTimeMillis() - recordingStartTime > MAX_WAIT_TIME_FOR_LOCATION_MS && mActualLocation != null) {
                    Log.d(TAG, "20 seconds elapsed, creating a location entry in metadata using the actual location.");
                    SensorManager.logSensorData(createDataCollectorGPSWrapperForLocation(mActualLocation));
                    takeSnapshot(location, dist);
                } else {
                    Log.d(TAG, "Waiting some more for a location.");
                }
            }
        }
    }

    private void takeSnapshot(@NonNull Location location, double dist) {
        mCameraIdle = false;
        mHandler.removeCallbacks(mIdleRunnable);
        mHandler.postDelayed(mIdleRunnable, 2000);
        mSequence.setTotalLength(mSequence.getTotalLength() + dist);
        if (mIndex == 0) {
            mSequence.setLocation(new SKCoordinate(location.getLatitude(), location.getLongitude()));
            SequenceDB.instance.updateSequenceLocation(mSequence.getId(), location.getLatitude(), location.getLongitude());
        }
        mOrientation = (int) Math.toDegrees(mSensorManager.getHeadingValues()[0]);
        long timestamp = System.currentTimeMillis();
        mCameraManager.takeSnapshot(this, mJpegPictureCallback, timestamp, mSequence.getId(), mSequence.getFolder().getPath(), location);
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
            gpsData.setSpeedObject(new SpeedObject(mActualLocation.getSpeed(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS));
        }
        gpsData.setSensorType(LibraryUtil.GPS_DATA);
        DataCollectorItemWrapper dataCollectorItemWrapper = new DataCollectorItemWrapper(gpsData);
        return dataCollectorItemWrapper;
    }

    /**
     * Checks whether at least one location entry exists in the metadata file.
     * @return {@code true} if a location entry exists in the metadata file, for the current track,
     * {@code false} otherwise.
     */
    private boolean positionAvailableInMetadata() {
        return SensorManager.isGpsDataAvailableInMetadata();
    }

    private void saveFrame(final boolean safe, final byte[] jpegData, final int sequenceId, final String folderPath, final Location location,
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
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE,
                                            !appPrefs.getBooleanPreference(PreferenceTypes.K_EXTERNAL_STORAGE));
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
                            SequenceDB.instance.insertPhoto(sequenceId, -1, mIndex, jpg.getPath(), location.getLatitude(), location.getLongitude(),
                                    location.getAccuracy(), orientation);

                            SensorManager.logVideoData(new VideoData(mIndex, 0, timestamp));
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
                    Log.d(TAG, "saveFrame: encoding done in " + (System.currentTimeMillis() - time) + " ms ,  video file " + ret[0] + " and frame " +
                            ret[1]);
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
                    SensorManager.logVideoData(new VideoData(mIndex, mVideoIndexF, timestamp));
                    try {
                        SequenceDB.instance.insertVideoIfNotAdded(sequenceId, mVideoIndexF, folderPath + "/" + mVideoIndexF + ".mp4");
                    } catch (Exception ignored) {
                    }

                    try {
                        SequenceDB.instance
                                .insertPhoto(sequenceId, mVideoIndexF, mIndex, folderPath + "/" + mVideoIndexF + ".mp4", location.getLatitude(),
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
}
