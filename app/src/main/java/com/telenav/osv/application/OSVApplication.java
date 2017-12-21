package com.telenav.osv.application;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatDelegate;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.telenav.osv.R;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.command.LogoutCommand;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.AppReadyEvent;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
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

    public static final int ACCOUNTS_PERMISSION_FACEBOOK = 115;

    public static final int ACCOUNTS_PERMISSION_GOOGLE = 116;

    private final static String TAG = "OSVApplication";

    public static long sUiThreadId;

    public static Date runTime = new Date(System.currentTimeMillis());

    public static String VERSION_NAME = "";

    private static String PACKAGE_NAME;

    private boolean isDebug;

    private Thread.UncaughtExceptionHandler mDefaultExHandler;

    /**
     * Object for accessing application preferences
     */
    private ApplicationPreferences appPrefs;

    private UploadManager mUploadManager;

    private Recorder mRecorder;

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
            appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
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
        runTime = new Date(System.currentTimeMillis());
        Log.d(TAG, "onCreate: time " + System.currentTimeMillis());

        Log.d(TAG, "onCreate: " + Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" +
                OSVApplication.VERSION_NAME);

        appPrefs = new ApplicationPreferences(this);
        isDebug = Utils.isDebugBuild(this);
        BackgroundThreadPool.post(new Runnable() {

            @Override
            public void run() {
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
                    boolean safe = appPrefs.getBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false);
                    int counter = appPrefs.getIntPreference(PreferenceTypes.K_FFMPEG_CRASH_COUNTER);
                    if (crashed && !safe && counter >= 2) {
                        Log.d(TAG, "onCreate: K_CRASHED is true, showing message and setting safe mode");
                        appPrefs.saveIntPreference(PreferenceTypes.K_FFMPEG_CRASH_COUNTER, 0);
                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, true);
                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, true);
                    }

                    appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, true);
                    Log.d(TAG, "onCreate: K_CRASHED is set to true");
                }
                AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
                Log.d(TAG, "onCreate: process " + currentProcName);
                try {
                    if (getExternalFilesDir(null) != null) {
                        Log.externalFilesDir = getExternalFilesDir(null).getPath();
                        Log.deleteOldLogs(OSVApplication.this);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
                }
                Utils.getSelectedStorage(OSVApplication.this);
                Utils.isDebugEnabled(OSVApplication.this);
                try {
                    Fabric.with(new Fabric.Builder(OSVApplication.this).kits(new Crashlytics(), new CrashlyticsNdk(), new Answers()).build());
                    Crashlytics.setBool(Log.RECORD_STATUS, false);
                    Crashlytics.setString(Log.LOG_FILE, Log.getLogFile().getAbsolutePath());
                    Crashlytics.setBool(Log.SDK_ENABLED, !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED));
                    Answers.getInstance().logCustom(new CustomEvent("New app session").putCustomAttribute(Log.SDK_ENABLED, "" +
                            !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED)));
                    Crashlytics.setBool(Log.POINTS_ENABLED, appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION));
                    Crashlytics.setBool(Log.UPLOAD_STATUS, false);
                    Crashlytics.setString(Log.PLAYBACK, "none");
                    int type = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
                    Crashlytics.setInt(Log.USER_TYPE, type);
                    Crashlytics.setBool(Log.SAFE_RECORDING, appPrefs.getBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED));
                    Crashlytics.setBool(Log.STATIC_FOCUS, appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC));
                    Crashlytics.setBool(Log.CAMERA_API_NEW, appPrefs.getBooleanPreference(PreferenceTypes.K_USE_CAMERA_API_NEW));
                    Crashlytics.setUserIdentifier(appPrefs.getStringPreference(PreferenceTypes.K_USER_ID));
                    Crashlytics.setUserName(appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME));
                    Log.d(TAG, "Crashlytics: initialized");
                    if (!isDebug && mIsMainProcess) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                    float version = pInfo.versionCode;
                                    float savedVersion = appPrefs.getFloatPreference(PreferenceTypes.K_VERSION_CODE);
                                    if (savedVersion != version) {
                                        if (Fabric.isInitialized()) {
                                            Answers.getInstance().logCustom(new CustomEvent("UpdateEvent").putCustomAttribute(Log.OLD_VERSION, savedVersion)
                                                    .putCustomAttribute(Log.NEW_VERSION, version));
                                        }
                                        if (version == 42) {
                                            EventBus.post(new LogoutCommand());
                                            SharedPreferences prefs = getSharedPreferences(ProfileFragment.PREFS_NAME, MODE_PRIVATE);
                                            prefs.edit().clear().apply();
                                        }
                                        if (version == 68 || (savedVersion < 68 && version >= 69)) {
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false);
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false);
                                        }
                                        appPrefs.saveFloatPreference(PreferenceTypes.K_VERSION_CODE, version);
                                        Log.d(TAG, "onCreate: new versionCode! " + version);
                                    }
                                } catch (Exception e) {
                                    Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
                                }
                            }
                        }, 10000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onCreate: " + Log.getStackTraceString(e));
                }
                PACKAGE_NAME = getApplicationContext().getPackageName();

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
                    mUploadManager = new UploadManager(OSVApplication.this);
                    mLoginManager = new LoginManager(OSVApplication.this);
                    SequenceDB.instantiate(OSVApplication.this);
                    if (mRecorder == null) {
                        mRecorder = new Recorder(OSVApplication.this);
                    }
                    consistencyCheck();
                } else {
                    SequenceDB.instantiate(OSVApplication.this);
                }
                mReady = true;
                EventBus.postSticky(new AppReadyEvent());
                //                if (Utils.isDebugEnabled(OSVApplication.this) && Utils.isDebuggableFlag(OSVApplication.this)) {
                //                    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                //                            .detectActivityLeaks()
                //                            .detectLeakedSqlLiteObjects()
                //                            .penaltyLog()
                //                            .penaltyDeath()
                //                            .build());
                //                }
            }
        });
    }

    public boolean isMainProcess() {
        return mIsMainProcess;
    }

    public void consistencyCheck() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SequenceDB.instance.consistencyCheck(OSVApplication.this);
                LocalSequence.forceRefreshLocalSequences();

                Log.d(TAG, "consistencyCheck: starting");
                try {
                    OSVFile osv = Utils.generateOSVFolder(OSVApplication.this);
                    for (OSVFile folder : osv.listFiles()) {
                        if (folder.getName().contains("&")) {
                            Log.d(TAG, "consistencyCheck: renaming " + folder.getName());
                            OSVFile file = new OSVFile(folder.getParentFile(), folder.getName().replace("&", ""));
                            folder.renameTo(file);
                            folder = file;
                            Log.d(TAG, "consistencyCheck: renamed to " + folder.getName());
                        }
                        OSVFile[] imgs = folder.listFiles(new FilenameFilter() {

                            @Override
                            public boolean accept(File dir, String filename) {
                                return filename.contains(".jpg") || filename.contains(".mp4");
                            }
                        });
                        if (imgs.length == 0) {
                            folder.delete();
                        } else {
                            for (OSVFile img : imgs) {
                                try {
                                    if (img.getName().endsWith("tmp")) {
                                        String seqId = folder.getName().split("_")[1];
                                        String index = img.getName().split("\\.")[0];
                                        SequenceDB.instance.deleteVideo(Integer.valueOf(seqId), Integer.valueOf(index));
                                        img.delete();
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
                                }
                            }
                            if (folder.listFiles(new FilenameFilter() {

                                @Override
                                public boolean accept(File dir, String filename) {
                                    return filename.contains(".jpg") || filename.contains(".mp4");
                                }
                            }).length == 0) {
                                folder.delete();
                            } else if (Utils.isInternetAvailable(OSVApplication.this)) {
                                //we check if the onlineSequenceId, stored on the device, exists on the server also
                                final int onlineSequenceId = SequenceDB.instance.getOnlineId(Integer.valueOf(folder.getName().split("_")[1]));
                                if (onlineSequenceId != -1) {

                                    final int sequenceId = Integer.valueOf(folder.getName().split("_")[1]);
                                    Cursor cursor = SequenceDB.instance.getFrames(Integer.valueOf(folder.getName().split("_")[1]));
                                    if (cursor != null && cursor.getCount() > 0) {
                                        new UserDataManager(OSVApplication.this)
                                                .listImages(onlineSequenceId, new NetworkResponseDataListener<PhotoCollection>() {

                                                    @Override
                                                    public void requestFailed(int status, PhotoCollection details) {
                                                        if (details.getApiCode() == API_ARGUMENT_OUT_OF_RANGE) {
                                                            int nrRowsAffected = SequenceDB.instance.resetOnlineSequenceId(onlineSequenceId);
                                                            Log.d(TAG, "consistencyCheck: rollback on sequence " + sequenceId + ", nr of rows " + "affected: " +
                                                                    nrRowsAffected);
                                                        }
                                                    }

                                                    @Override
                                                    public void requestFinished(int status, PhotoCollection collectionData) {

                                                    }
                                                });
                                    }
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
                }
                Log.d(TAG, "consistencyCheck: done.");
            }
        }).start();
    }

    public ApplicationPreferences getAppPrefs() {
        if (appPrefs == null) {
            appPrefs = new ApplicationPreferences(this);
        }
        return appPrefs;
    }

    public UploadManager getUploadManager() {
        if (mUploadManager == null) {
            mUploadManager = new UploadManager(this);
        }
        return mUploadManager;
    }

    public Recorder getRecorder() {
        if (mRecorder == null) {
            mRecorder = new Recorder(OSVApplication.this);
        }
        return mRecorder;
    }

    public boolean isReady() {
        return mReady;
    }

    public LoginManager getLoginManager() {
        if (mLoginManager == null) {
            mLoginManager = new LoginManager(this);
        }
        return mLoginManager;
    }
}
