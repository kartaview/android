package com.telenav.osv.application;

import java.util.Date;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
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
import com.telenav.osv.location.LocationService;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.encoder.VideoEncoder;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.persistence.frame.FramePersistenceManager;
import com.telenav.osv.recorder.persistence.video.VideoPersistenceManager;
import com.telenav.osv.recorder.score.PositionMatcher;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.shutter.Shutter;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;
import net.danlew.android.joda.JodaTimeAndroid;
import androidx.appcompat.app.AppCompatDelegate;
import io.fabric.sdk.android.Fabric;

/**
 * Manages the application itself (on top of the activity), mainly to force Camera getting
 * closed in case of crash.
 */
public class OSVApplication extends Application {

    public static final int START_RECORDING_PERMISSION = 111;

    public static final int CAMERA_PERMISSION = 112;

    public static final int LOCATION_PERMISSION = 113;

    public static final int LOCATION_PERMISSION_BT = 114;

    public static final int APP_VERSION_CODE_2_7_4 = 100;

    private final static String TAG = "OSVApplication";

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

            isDebug = Utils.isDebugBuild(OSVApplication.this);
            if (!isDebug) {
                try {
                    Crashlytics.logException(ex);
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
                    stopService(new Intent(OSVApplication.this, CameraHandlerService.class));
                }
                Log.w(TAG, "uncaughtException: on ui thread");
                if (isDebug) {
                    mDefaultExHandler.uncaughtException(thread, ex);
                } else {
                    int restartedUntilNow = appPrefs.getIntPreference(PreferenceTypes.K_RESTART_COUNTER);
                    if (restartedUntilNow <= 2) {
                        appPrefs.saveIntPreference(PreferenceTypes.K_RESTART_COUNTER, restartedUntilNow + 1);
                        Intent mStartActivity = new Intent(OSVApplication.this, SplashActivity.class);
                        mStartActivity.putExtra(SplashActivity.RESTART_FLAG, true);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent =
                                PendingIntent.getActivity(OSVApplication.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) OSVApplication.this.getSystemService(Context.ALARM_SERVICE);
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
        runTime = new Date(System.currentTimeMillis());
        Log.d(TAG, "onCreate: time " + System.currentTimeMillis());

        Log.d(TAG, "onCreate: " + Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" +
                OSVApplication.VERSION_NAME);

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
            ActivityManager manager = (ActivityManager) OSVApplication.this.getSystemService(Context.ACTIVITY_SERVICE);
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
                Injection.provideOSCDatabase(getApplicationContext());
                appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, true);
                Log.d(TAG, "onCreate: K_CRASHED is set to true");
            }
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
            Log.d(TAG, "onCreate: start " + currentProcName);
            try {
                if (getExternalFilesDir(null) != null) {
                    Log.externalFilesDir = getExternalFilesDir(null).getPath();
                    Log.deleteOldLogs(OSVApplication.this);
                }
            } catch (Exception e) {
                Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
            }
            Utils.isDebugEnabled(OSVApplication.this);
            try {
                Fabric.with(new Fabric.Builder(OSVApplication.this).kits(new Crashlytics(), new CrashlyticsNdk(), new Answers()).build());
                Crashlytics.setBool(Log.RECORD_STATUS, false);
                Crashlytics.setString(Log.LOG_FILE, Log.getLogFile().getAbsolutePath());
                Crashlytics.setBool(Log.SDK_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED));
                Answers.getInstance().logCustom(new CustomEvent("New app session").putCustomAttribute(Log.SDK_ENABLED, "" +
                        appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED)));
                Crashlytics.setBool(Log.POINTS_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION));
                Crashlytics.setBool(Log.UPLOAD_STATUS, false);
                Crashlytics.setString(Log.PLAYBACK, "none");
                int type = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
                Crashlytics.setInt(Log.USER_TYPE, type);
                Crashlytics.setBool(Log.SAFE_RECORDING, !appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED));
                Crashlytics.setBool(Log.STATIC_FOCUS, appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC));
                Crashlytics.setBool(Log.CAMERA_API_NEW, appPrefs.getBooleanPreference(PreferenceTypes.K_USE_CAMERA_API_NEW));
                Crashlytics.setUserIdentifier(appPrefs.getStringPreference(PreferenceTypes.K_USER_ID));
                Crashlytics.setUserName(appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME));
                Log.d(TAG, "Crashlytics: initialized");
                if (!isDebug && mIsMainProcess) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            float version = pInfo.versionCode;
                            float savedVersion = appPrefs.getFloatPreference(PreferenceTypes.K_VERSION_CODE);
                            if (savedVersion != version) {
                                if (Fabric.isInitialized()) {
                                    Answers.getInstance().logCustom(new CustomEvent("UpdateEvent").putCustomAttribute(Log.OLD_VERSION, savedVersion)
                                            .putCustomAttribute(Log.NEW_VERSION, version));
                                }
                                if (savedVersion < APP_VERSION_CODE_2_7_4 && version > APP_VERSION_CODE_2_7_4) {
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
                                    appPrefs.removePreference(PreferenceTypes.K_SAFE_MODE_ENABLED);
                                    appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
                                    appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
                                } else if (version > APP_VERSION_CODE_2_7_4) {
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
                                }
                                appPrefs.saveFloatPreference(PreferenceTypes.K_VERSION_CODE, version);
                                Log.d(TAG, "onCreate: new versionCode! " + version);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
                        }
                    }, 10000);
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
            SKReverseGeocoderManager.getInstance();
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
            mRecorder = new RecorderManager(OSVApplication.this,
                    getUserDataSource(),
                    getSequenceLocalDataSource(),
                    getObdManager(),
                    getLocationService(),
                    getShutterManager());
        }
        return mRecorder;
    }

    public boolean isReady() {
        return mReady;
    }

    public LoginManager getLoginManager() {
        if (mLoginManager == null) {
            mLoginManager = new LoginManager(this, getUserDataSource());
        }
        return mLoginManager;
    }

    /**
     * @return an instance to the {@link Camera} implementation.
     */
    public Camera getCamera() {
        if (camera == null) {
            int pictureWidth = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, 0);
            int pictureHeight = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, 0);
            boolean isJpegMode = !appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED);
            camera = Injection.provideCamera(getApplicationContext(),
                    new Size(pictureWidth, pictureHeight),
                    Utils.getLandscapeScreenSize(getApplicationContext()),
                    isJpegMode);
            if (pictureWidth == 0 || pictureHeight == 0) {
                if (!camera.isCamera2Api() && !isJpegMode) {
                    savePictureResolution(camera.getPreviewSize());
                } else {
                    savePictureResolution(camera.getPictureSize());
                }
            }
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
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getFrameLocalDataSource());
            } else if (!(recordingPersistence instanceof FramePersistenceManager)) {
                Log.d(TAG, "getRecordingPersistence: switching from video to frame");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getFrameLocalDataSource());
            }
        } else {
            if (recordingPersistence == null) {
                Log.d(TAG, "getRecordingPersistence: new video persistence");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getVideoDataSource(),
                        getVideoEncoder());
            } else if (!(recordingPersistence instanceof VideoPersistenceManager)) {
                Log.d(TAG, "getRecordingPersistence: switching from frame to video");
                recordingPersistence = Injection.provideRecordingPersistence(getSequenceLocalDataSource(), getLocationLocalDataSource(), getVideoDataSource(),
                        getVideoEncoder());
            }
        }
        return recordingPersistence;
    }

    /**
     * Migrate the preferences for Settings 2.1
     */
    private void migratePreferences() {
        if (!appPrefs.contains(PreferenceTypes.K_MAP_ENABLED) && appPrefs.contains(PreferenceTypes.K_MAP_DISABLED)) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_ENABLED, !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED));
        } else if (!appPrefs.contains(PreferenceTypes.K_MAP_ENABLED)) {
            //enable map as default option
            appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_ENABLED, true);
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
        return Injection.provideShutterManager(getLocationService(), getObdManager(), appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_BENCHMARK_SHUTTER_LOGIC));
    }

    /**
     * Stores the picture resolution to the application preferences.
     * @param pictureSize
     */
    private void savePictureResolution(Size pictureSize) {
        appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, pictureSize.getWidth());
        appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, pictureSize.getHeight());
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

    /**
     * Initialises the preferences for the FTUE use case.
     */
    private void initPrefsFtue() {
        if (!appPrefs.getBooleanPreference(PreferenceTypes.K_FTUE)) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_FTUE, true);
            appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
            //TODO: Add the preferences for private build. For the app store build this should be removed from FTUE and the gamification ON.
            appPrefs.saveBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
//            appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_RECORDING_TAGGING, true);
//            appPrefs.saveIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE, URL_ENV_PRIVATE_POSITION);
        }
    }
}
