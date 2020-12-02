package com.telenav.osv.application;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mapbox.mapboxsdk.Mapbox;
import com.telenav.osv.R;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.event.AppReadyEvent;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.location.AccuracyQualityChecker;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.recorder.encoder.VideoEncoder;
import com.telenav.osv.recorder.metadata.MetadataSensorManager;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.persistence.frame.FramePersistenceManager;
import com.telenav.osv.recorder.persistence.video.VideoPersistenceManager;
import com.telenav.osv.recorder.score.PositionMatcher;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.shutter.Shutter;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.ExtensionsKt;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Date;

/**
 * Manages the application itself (on top of the activity), mainly to force Camera getting
 * closed in case of crash.
 */
public class KVApplication extends Application {

    public static final int START_RECORDING_PERMISSION = 111;

    public static final int CAMERA_PERMISSION = 112;

    public static final int LOCATION_PERMISSION = 113;

    public static final int LOCATION_PERMISSION_BT = 114;

    public static final int APP_VERSION_CODE_2_7_4 = 100;

    public static final int APP_VERSION_CODE_3_2_0 = 114;

    private final static String TAG = KVApplication.class.getSimpleName();

    public static long sUiThreadId;

    public static Date runTime = new Date(System.currentTimeMillis());

    public static String VERSION_NAME = "";

    private boolean isDebug;

    private Thread.UncaughtExceptionHandler mDefaultExHandler;

    /**
     * Object for accessing application preferences
     */
    private ApplicationPreferences appPrefs;

    private RecorderManager mRecorder;

    private RecordingPersistence recordingPersistence;

    private Score score;

    private Camera camera;

    private boolean mIsMainProcess;

    private Thread.UncaughtExceptionHandler mExHandler = new Thread.UncaughtExceptionHandler() {

        public void uncaughtException(Thread thread, Throwable ex) {
            Log.e(TAG, "uncaughtException: " + Log.getStackTraceString(ex));

            isDebug = Utils.isDebugBuild(KVApplication.this);
            if (!isDebug) {
                try {
                    FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
                    crashlytics.recordException(ex);
                } catch (Exception e) {
                    Log.d(TAG, "uncaughtException: Crashlitics not initialized, cannot send logs.");
                }
            }
            if (!mIsMainProcess) {
                Log.d(TAG, "uncaughtException: ");
                System.exit(1);
                return;
            }
            appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, true);
            if (thread.getId() == sUiThreadId) {
                if (mRecorder != null) {
                    Log.e(TAG, "Uncaught exception! Closing down camera safely firsthand");
                    mRecorder.forceCloseCamera();
                    stopService(new Intent(KVApplication.this, CameraHandlerService.class));
                }
                Log.w(TAG, "uncaughtException: on ui thread");
                if (isDebug) {
                    mDefaultExHandler.uncaughtException(thread, ex);
                } else {
                    int restartedUntilNow = appPrefs.getIntPreference(PreferenceTypes.K_RESTART_COUNTER);
                    if (restartedUntilNow <= 2) {
                        appPrefs.saveIntPreference(PreferenceTypes.K_RESTART_COUNTER, restartedUntilNow + 1);
                        Intent mStartActivity = new Intent(KVApplication.this, SplashActivity.class);
                        mStartActivity.putExtra(SplashActivity.RESTART_FLAG, true);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent =
                                PendingIntent.getActivity(KVApplication.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) KVApplication.this.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                    }
                    System.exit(1);
                }
            } else {
                if (isDebug) {
                    mDefaultExHandler.uncaughtException(thread, ex);
                }
            }
        }
    };

    private boolean mReady;

    private LoginManager mLoginManager;

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        //init MapBoxSdk
        Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
        FirebaseApp.initializeApp(getApplicationContext());
        runTime = new Date(System.currentTimeMillis());
        Log.d(TAG, "onCreate: time " + System.currentTimeMillis());

        Log.d(TAG, "onCreate: " + Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" +
                KVApplication.VERSION_NAME);

        appPrefs = new ApplicationPreferences(this);
        Log.d(TAG, String.format("onCreate: app code version - %s", appPrefs.getFloatPreference(PreferenceTypes.K_VERSION_CODE)));
        migratePreferences();
        initPrefsFtue();
        appPrefs.saveLongPreference(PreferenceTypes.K_RECORD_START_TIME, 0);
        appPrefs.saveIntPreference(PreferenceTypes.K_APP_RUN_TIME_COUNTER, appPrefs.getIntPreference(PreferenceTypes.K_APP_RUN_TIME_COUNTER) + 1);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_MANUAL_STOPPED, false);
        isDebug = Utils.isDebugBuild(this);
        BackgroundThreadPool.post(() -> {
            String currentProcName = "";
            int pid = android.os.Process.myPid();
            ActivityManager manager = (ActivityManager) KVApplication.this.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
                if (processInfo.pid == pid) {
                    currentProcName = processInfo.processName;
                    break;
                }
            }
            mIsMainProcess = !currentProcName.contains(getString(R.string.playback_process_name));
            if (mIsMainProcess) {
                Log.d(TAG, "onCreate: --------------------------------------------------------------------------------------\n" +
                        "------------------------------------------------------------------------------------------------" +
                        "------------------------------------------------------------------------------------------------" +
                        "------------------------------------------------------------------------------------------------" +
                        "------------------------------------------------------------------------------------------------" +
                        "------------------------------------------------------------------------------------------------" +
                        "------------------------------------------------------------------------------------------------");
                boolean crashed = appPrefs.getBooleanPreference(PreferenceTypes.K_CRASHED, false);
                boolean isVideoMode = appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, false);
                int counter = appPrefs.getIntPreference(PreferenceTypes.K_NEW_ENCODER_CRASH_COUNTER);
                if (crashed && isVideoMode && counter >= 2) {
                    Log.d(TAG, "onCreate: K_CRASHED is true, showing message and setting safe mode");
                    appPrefs.saveIntPreference(PreferenceTypes.K_NEW_ENCODER_CRASH_COUNTER, 0);
                    appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, true);
                    appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, false);
                }
                //init room database to perform a migration
                Injection.provideKVDatabase(getApplicationContext());
                appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, true);
                Log.d(TAG, "onCreate: K_CRASHED is set to true");
            }
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            Log.d(TAG, "onCreate: start " + currentProcName);
            try {
                if (getExternalFilesDir(null) != null) {
                    Log.externalFilesDir = getExternalFilesDir(null).getPath();
                    Log.deleteOldLogs(KVApplication.this);
                }
            } catch (Exception e) {
                Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
            }
            Utils.isDebugEnabled(KVApplication.this);
            try {
                FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
                crashlytics.setCustomKey(Log.RECORD_STATUS, false);
                crashlytics.setCustomKey(Log.LOG_FILE, Log.getLogFile().getAbsolutePath());
                crashlytics.setCustomKey(Log.SDK_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED));
                FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getApplicationContext());
                Bundle bundle = new Bundle();
                bundle.putBoolean(Log.SDK_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED));
                analytics.logEvent("new_app_session", bundle);
                crashlytics.setCustomKey(Log.POINTS_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION));
                crashlytics.setCustomKey(Log.UPLOAD_STATUS, false);
                crashlytics.setCustomKey(Log.PLAYBACK, "none");
                int type = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
                crashlytics.setCustomKey(Log.USER_TYPE, type);
                crashlytics.setCustomKey(Log.SAFE_RECORDING, !appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED));
                crashlytics.setCustomKey(Log.STATIC_FOCUS, appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC));
                crashlytics.setCustomKey(Log.CAMERA_API_NEW, appPrefs.getBooleanPreference(PreferenceTypes.K_USE_CAMERA_API_NEW));
                crashlytics.setUserId(appPrefs.getStringPreference(PreferenceTypes.K_USER_ID));
                Log.d(TAG, "Crashlytics: initialized");
                if (!isDebug && mIsMainProcess) {
                    try {
                        PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                        float version = pInfo.versionCode;
                        float savedVersion = appPrefs.getFloatPreference(PreferenceTypes.K_VERSION_CODE);
                        if (savedVersion != version) {
                            Bundle bundleUpdateEvent = new Bundle();
                            bundleUpdateEvent.putFloat(Log.OLD_VERSION, savedVersion);
                            bundleUpdateEvent.putFloat(Log.NEW_VERSION, version);
                            analytics.logEvent("update_event", bundle);
                            if (savedVersion < APP_VERSION_CODE_2_7_4 && version > APP_VERSION_CODE_2_7_4) {
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
                                appPrefs.removePreference(PreferenceTypes.K_SAFE_MODE_ENABLED);
                                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
                                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
                            } else if (version > APP_VERSION_CODE_2_7_4) {
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
                                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
                                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
                            }
                            appPrefs.saveFloatPreference(PreferenceTypes.K_VERSION_CODE, version);
                            Log.d(TAG, "onCreate: new versionCode! " + version);
                        }
                        getCamera();
                    } catch (Exception e) {
                        Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "onCreate: " + Log.getStackTraceString(e));
            }
            PackageInfo pInfo;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                VERSION_NAME = pInfo.versionName;
            } catch (Exception e) {
                Log.w(TAG, "onCreate: " + Log.getStackTraceString(e));
            }
            String arch = System.getProperty("os.arch");
            Log.d(TAG, "onCreate: architecture is " + arch);
            if (mIsMainProcess) {
                mDefaultExHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(mExHandler);
            }
            if (mIsMainProcess) {
                getLoginManager();
            }
            mReady = true;
            EventBus.postSticky(new AppReadyEvent());
        });
    }

    public boolean isMainProcess() {
        return mIsMainProcess;
    }

    public ApplicationPreferences getAppPrefs() {
        if (appPrefs == null) {
            appPrefs = new ApplicationPreferences(this);
        }
        return appPrefs;
    }

    public RecorderManager getRecorder() {
        if (mRecorder == null) {
            mRecorder = new RecorderManager(KVApplication.this,
                    getUserDataSource(),
                    getSequenceLocalDataSource(),
                    getObdManager(),
                    getShutterManager(),
                    getMetadataSensorManager(),
                    Injection.provideGpsTrailHelper(getLocationService()));
        }
        return mRecorder;
    }

    public boolean isReady() {
        return mReady;
    }

    public LoginManager getLoginManager() {
        if (mLoginManager == null) {
            mLoginManager = new LoginManager(this, getUserDataSource(),
                    Injection.provideJarvisLoginUseCase(Injection.provideJarvisLoginApi()));
        }
        return mLoginManager;
    }

    /**
     * Init the camera and return the camera based on {@link CameraHelper#initCamera(ApplicationPreferences, Context)} logic.
     * @return an instance to the {@link Camera} implementation.
     */
    public Camera getCamera() {
        if (camera == null) {
            camera = CameraHelper.initCamera(appPrefs, getApplicationContext());
        }
        return camera;
    }

    /**
     * Reload the camera based on the saved preferences.
     * @return an instance to the {@link Camera} implementation.
     */
    public Camera reloadCameraBasedOnSettings() {
        if (camera == null) {
            int pictureWidth = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
            int pictureHeight = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
            boolean videoMode = appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED);
            Context context = getApplicationContext();
            Log.d(TAG,
                    "reloadCameraBasedOnSettings. Status: Initialising camera. Width: " + pictureWidth + ". Height: " + pictureHeight + ". Video mode: " + videoMode);
            camera = Injection.provideCamera(context,
                    new Size(pictureWidth, pictureHeight),
                    Utils.getLandscapeScreenSize(context),
                    !videoMode);
            Size newResolution = ExtensionsKt.getResolution(camera);
            CameraHelper.saveResolutionInPrefs(newResolution, camera, appPrefs);
        }
        return camera;
    }

    /**
     * @return an instance to the {@link Score} implementation.
     */
    public Score getScore() {
        if (score == null) {
            score = Injection.provideScoreManager(getScoreDataSource(), getPositionMatcher(), getLocationService(), getObdManager());
        }
        return score;
    }

    public void releaseRecording() {
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
            Log.d(TAG, "getRecordingPersistence : release persistence");
            recordingPersistence = null;
            releaseCamera();
        }
    }

    public void releaseCamera() {
        if (camera != null && camera.isCameraOpen()) {
            camera.closeCamera();
        }
        camera = null;
    }

    public void releaseScore() {
        if (score != null) {
            score.release();
            score = null;
        }
    }

    /**
     * @return {@link RecordingPersistence}
     */
    public RecordingPersistence getRecordingPersistence() {
        Log.d(TAG, "getRecordingPersistence");
        if (!appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED)) {
            if (recordingPersistence == null) {
                Log.d(TAG, "getRecordingPersistence: new frame persistence");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getFrameLocalDataSource(),
                        getMetadataSensorManager());
            } else if (!(recordingPersistence instanceof FramePersistenceManager)) {
                Log.d(TAG, "getRecordingPersistence: switching from video to frame");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getFrameLocalDataSource(),
                        getMetadataSensorManager());
            }
        } else {
            if (recordingPersistence == null) {
                Log.d(TAG, "getRecordingPersistence: new video persistence");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getVideoDataSource(),
                        getVideoEncoder(),
                        getMetadataSensorManager());
            } else if (!(recordingPersistence instanceof VideoPersistenceManager)) {
                Log.d(TAG, "getRecordingPersistence: switching from frame to video");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getVideoDataSource(),
                        getVideoEncoder(),
                        getMetadataSensorManager());
            }
        }
        return recordingPersistence;
    }

    /**
     * Migrate the preferences for Settings 2.1
     */
    private void migratePreferences() {
        if (!appPrefs.contains(PreferenceTypes.K_MAP_ENABLED) && appPrefs.contains(PreferenceTypes.K_MAP_DISABLED)) {
            boolean previousMapEnabled = !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED);
            appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_ENABLED, previousMapEnabled);
        } else if (!appPrefs.contains(PreferenceTypes.K_MAP_ENABLED)) {
            //enable map as default option
            appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_ENABLED, true);
        }

        if ((!appPrefs.contains(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED) && appPrefs.contains(PreferenceTypes.K_RECORDING_MAP_ENABLED)) ||
                !appPrefs.contains(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED)) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED, true);
        }
    }

    private SequenceLocalDataSource getSequenceLocalDataSource() {
        return Injection.provideSequenceLocalDataSource(getApplicationContext(),
                getFrameLocalDataSource(),
                getScoreDataSource(),
                getLocationLocalDataSource(),
                getVideoDataSource());
    }

    /**
     * @return {@code LocationService}
     */
    private LocationService getLocationService() {
        return Injection.provideLocationService(getApplicationContext());
    }

    /**
     * @return {@code Shutter}'s concrete implementation.
     */
    private Shutter getShutterManager() {
        return Injection.provideShutterManager(
                getLocationService(),
                getObdManager(),
                appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_BENCHMARK_SHUTTER_LOGIC),
                getMetadataSensorManager(),
                getMetadataSensorManager(),
                new AccuracyQualityChecker());
    }

    /**
     * @return {@code VideoLocalDataSource}.
     */
    private VideoLocalDataSource getVideoDataSource() {
        return Injection.provideVideoDataSource(this.getApplicationContext());
    }

    /**
     * @return {@code FrameLocalDataSource}.
     */
    private FrameLocalDataSource getFrameLocalDataSource() {
        return Injection.provideFrameLocalDataSource(this.getApplicationContext());
    }

    /**
     * @return {@code UserDataSource}.
     */
    private UserDataSource getUserDataSource() {
        return Injection.provideUserRepository(this.getApplicationContext());
    }

    /**
     * @return {@code LocationLocalDataSource}.
     */
    private LocationLocalDataSource getLocationLocalDataSource() { return Injection.provideLocationLocalDataSource(this.getApplicationContext());}

    /**
     * @return {@code ScoreDataSource}.
     */
    private ScoreDataSource getScoreDataSource() {
        return Injection.provideScoreLocalDataSource(this.getApplicationContext());
    }

    /**
     * @return {@code ObdManager}.
     */
    private ObdManager getObdManager() {
        return Injection.provideObdManager(getApplicationContext(), getAppPrefs());
    }

    /**
     * @return {@link VideoEncoder}
     */
    private VideoEncoder getVideoEncoder() {
        return Injection.provideVideoEncoder();
    }

    /**
     * @return {@link PositionMatcher}
     */
    private PositionMatcher getPositionMatcher() {
        return Injection.providePositionMatcher(getApplicationContext());
    }

    private MetadataSensorManager getMetadataSensorManager() { return Injection.provideMetadataSensorManager();}

    /**
     * Initialises the preferences for the FTUE use case.
     */
    private void initPrefsFtue() {
        if (!appPrefs.getBooleanPreference(PreferenceTypes.K_FTUE)) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_FTUE, true);
            appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
            //TODO: Add the preferences for private build. For the app store build this should be removed from FTUE and the gamification ON.
            appPrefs.saveBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
        }
    }
}
