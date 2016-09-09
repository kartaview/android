package com.telenav.osv.application;

import java.util.Date;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.support.multidex.MultiDexApplication;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.CameraManager;
import com.telenav.osv.manager.LocationManager;
import com.telenav.osv.manager.ObdBleManager;
import com.telenav.osv.manager.ObdManager;
import com.telenav.osv.manager.ObdWifiManager;
import com.telenav.osv.manager.SensorManager;
import com.telenav.osv.manager.ShutterManager;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Manages the application itself (on top of the activity), mainly to force Camera getting
 * closed in case of crash.
 */
public class OSVApplication extends MultiDexApplication {
    private final static String TAG = "OSVApplication";

    public static String PACKAGE_NAME;

    public static long sUiThreadId;

    public static String sOSVBackup;

    public static String sOSVBackupExt;

    public static Date runTime;

    public static String VERSION_NAME = "";

    private CameraManager mCamManager;

    private Thread.UncaughtExceptionHandler mDefaultExHandler;

    /**
     * Object for accessing application preferences
     */
    private ApplicationPreferences appPrefs;

    private UploadManager mUploadManager;

    private SequenceDB mSequenceDB;

    public boolean isDebug;

    private Thread.UncaughtExceptionHandler mExHandler = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread thread, Throwable ex) {

            Log.e(TAG, "uncaughtException: " + Log.getStackTraceString(ex));
            isDebug = Utils.isDebugBuild(OSVApplication.this);
//            if (!isDebug) {
//                try {
//                    Crashlytics.logException(ex);
//                } catch (Exception e) {
//                    Log.d(TAG, "uncaughtException: Crashlitics not initialized, cannot send logs.");
//                }
//            }
            if (thread.getId() == sUiThreadId) {
                if (mCamManager != null) {
                    Log.e(TAG, "Uncaught exception! Closing down camera safely firsthand");
                    mCamManager.forceCloseCamera();
                    stopService(new Intent(OSVApplication.this, CameraHandlerService.class));
                }
                Log.d(TAG, "uncaughtException: on ui thread");
                if (isDebug) {
                    mDefaultExHandler.uncaughtException(thread, ex);
                } else {
                    int restartedUntilNow = appPrefs.getIntPreference(PreferenceTypes.K_RESTART_COUNTER);
                    if (restartedUntilNow <= 2) {
                        appPrefs.saveIntPreference(PreferenceTypes.K_RESTART_COUNTER, restartedUntilNow + 1);
                        Intent mStartActivity = new Intent(OSVApplication.this, SplashActivity.class);
                        mStartActivity.putExtra(SplashActivity.RESTART_FLAG, true);
//                        if (restartedUntilNow == 0) {
//                            mStartActivity.putExtra(SplashActivity.RESTART_PAGE, MainActivity.sCurrentScreen);
//                            mStartActivity.putExtra(SplashActivity.RESTART_FRAGMENT, MainActivity.sFragmentOverlayTag);
//                            if (MainActivity.sLastSequence != -1) {
//                                mStartActivity.putExtra(SplashActivity.RESTART_SEQUENCE_ONLINE, MainActivity.sLastSequence);
//                                mStartActivity.putExtra(SplashActivity.RESTART_SEQUENCE_ID, MainActivity.sLastSequence);
//                                mStartActivity.putExtra(SplashActivity.RESTART_SEQUENCE_INDEX, MainActivity.sLastSequenceIndex);
//                            }
//                        }
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(OSVApplication.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
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

    private LocationManager mLocationManager;

    private SensorManager mSensorManager;

    private ObdManager mOBDManager;

    private ShutterManager mShutterManager;

    @Override
    public void onCreate() {
        super.onCreate();
        runTime = new Date(System.currentTimeMillis());
        try {
            if (getExternalFilesDir(null) != null) {
                Log.externalFilesDir = getExternalFilesDir(null).getPath();
                Log.deleteOldLogs(this);
            }
        } catch (Exception e) {
            Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
        }
        appPrefs = new ApplicationPreferences(this);
        isDebug = Utils.isDebugBuild(this);
        Utils.getSelectedStorage(this);
        Log.DEBUG = true;//isDebug || appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED);
        Utils.isDebugEnabled(this);
        try {
            if (!isDebug) {
//                Fabric fabric = new Fabric.Builder(OSVApplication.this)
//                        .kits(new Crashlytics())
////                        .debuggable(true)
//                        .build();
//                Fabric.with(fabric);
////                Fabric.with(this, new Crashlytics());
//                Crashlytics.setUserIdentifier(appPrefs.getStringPreference(PreferenceTypes.K_USER_ID));
//                Crashlytics.setUserName(appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME));
                Log.d(TAG, "Crashlytics: initialized");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                            float version = pInfo.versionCode;
                            float savedVersion = appPrefs.getFloatPreference(PreferenceTypes.K_VERSION_CODE);
                            if (savedVersion != version) {
                                if (version == 42) {
                                    if (mUploadManager != null) {
                                        mUploadManager.logOut();
                                    }
                                    SharedPreferences prefs = getSharedPreferences(ProfileFragment.PREFS_NAME, MODE_PRIVATE);
                                    prefs.edit().clear().apply();
                                }
                                appPrefs.saveFloatPreference(PreferenceTypes.K_VERSION_CODE, version);
//                                if (Fabric.isInitialized()) {
//                                    String arch = System.getProperty("os.arch");
//                                    Crashlytics.logException(new UpgradeException("OSV", "New versionCode detected! " + version + " architecture " + arch));
//                                    Crashlytics.log(3, "OSV", "New versionCode detected! " + version);
//                                    Crashlytics.log("New versionCode! " + version);
//                                }
                                Log.d(TAG, "onCreate: new versionCode! " + version);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "onCreate: " + e.getLocalizedMessage());
                        }
                    }
                }, 10000);
            }
        } catch (Exception e){
            Log.e(TAG,"onCreate: " + Log.getStackTraceString(e));
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
        mDefaultExHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mExHandler);
        SKReverseGeocoderManager.getInstance();
        int obdType = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE);
        setObdManager(obdType);
        new CameraManager(this);
        mCamManager = CameraManager.instance;
        mUploadManager = new UploadManager(this);
        mSequenceDB = new SequenceDB(this);
        mLocationManager = LocationManager.get(this);
        mSensorManager = new SensorManager(this);
        mShutterManager = new ShutterManager(this);
        consistencyCheck();
    }

    private void consistencyCheck() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SequenceDB.instance.consistencyCheck(OSVApplication.this);
                Sequence.getLocalSequences();
                mUploadManager.consistencyCheck();
                Log.d(TAG, "consistencyCheck: done.");
            }
        }).start();
    }

    public LocationManager getLocationManager() {
        return mLocationManager;
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

    public SensorManager getSensorManager() {
        return mSensorManager;
    }

    public ObdManager getOBDManager() {
        return mOBDManager;
    }

    public void setObdManager(int type) {
        switch (type){
            case PreferenceTypes.V_OBD_WIFI:
                mOBDManager = new ObdWifiManager(this);
                break;
            case PreferenceTypes.V_OBD_BLE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mOBDManager = new ObdBleManager(this);
                } else {
                    appPrefs.saveIntPreference(PreferenceTypes.K_OBD_TYPE, PreferenceTypes.V_OBD_WIFI);
                    mOBDManager = new ObdWifiManager(this);
                }
                break;
            case PreferenceTypes.V_OBD_BT:
                mOBDManager = new ObdWifiManager(this);//todo change
                break;
            default:
                break;
        }
    }

    public ShutterManager getShutterManager() {
        return mShutterManager;
    }
}
