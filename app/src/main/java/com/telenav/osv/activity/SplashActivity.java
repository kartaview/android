package com.telenav.osv.activity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.greenrobot.eventbus.Subscribe;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.support.annotation.WorkerThread;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitializationListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.ngx.versioning.SKMapVersioningListener;
import com.skobbler.ngx.versioning.SKVersioningManager;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.AppReadyEvent;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.service.RecentClearedService;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Activity that installs required resources (from assets/MapResources.zip) to
 * the device
 */
public class SplashActivity extends Activity implements SKMapsInitializationListener, SKMapVersioningListener {

    public static final String RESTART_FLAG = "restartExtra";

    private static final long KILO = 1024;

    private static final long MEGA = KILO * KILO;

    private static final int REQUEST_ENABLE_INTRO = 1;

    private static final String TAG = "SplashActivity";

    /**
     * Path to the MapResources directory
     */
    public static String mapResourcesDirPath = "";

    private static int newMapVersionDetected = 0;

    private boolean update = false;

    private ApplicationPreferences appPrefs;

    private boolean mLibraryInitialized;

    private OSVApplication mApp;

    private Runnable goToMapRunnable = new Runnable() {
        @Override
        public void run() {
            goToMap();
        }
    };

    private static String chooseStoragePath(Context context) {
        if (getAvailableMemorySize(Environment.getDataDirectory().getPath()) >= 50 * MEGA) {
            if (context != null && context.getFilesDir() != null) {
                return context.getFilesDir().getPath();
            }
        } else {
            if ((context != null) && (context.getExternalFilesDir(null) != null)) {
                if (getAvailableMemorySize(context.getExternalFilesDir(null).toString()) >= 50 * MEGA) {
                    return context.getExternalFilesDir(null).toString();
                }
            }
        }

        SKLogging.writeLog(TAG, "There is not enough memory on any storage, but return internal memory",
                SKLogging.LOG_DEBUG);

        if (context != null && context.getFilesDir() != null) {
            return context.getFilesDir().getPath();
        } else {
            if ((context != null) && (context.getExternalFilesDir(null) != null)) {
                return context.getExternalFilesDir(null).toString();
            } else {
                return null;
            }
        }
    }

    /**
     * get the available internal memory size
     * @return available memory size in bytes
     */
    private static long getAvailableMemorySize(String path) {
        StatFs statFs = null;
        try {
            statFs = new StatFs(path);
        } catch (IllegalArgumentException ex) {
            SKLogging.writeLog("SplashActivity", "Exception when creating StatF ; message = " + ex,
                    SKLogging.LOG_DEBUG);
        }
        if (statFs != null) {
            Method getAvailableBytesMethod = null;
            try {
                getAvailableBytesMethod = statFs.getClass().getMethod("getAvailableBytes");
            } catch (NoSuchMethodException e) {
                SKLogging.writeLog(TAG, "Exception at getAvailableMemorySize method = " + e.getMessage(),
                        SKLogging.LOG_DEBUG);
            }

            if (getAvailableBytesMethod != null) {
                try {
                    SKLogging.writeLog(TAG, "Using new API for getAvailableMemorySize method !!!", SKLogging.LOG_DEBUG);
                    return (Long) getAvailableBytesMethod.invoke(statFs);
                } catch (IllegalAccessException e) {
                    return (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
                } catch (InvocationTargetException e) {
                    return (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
                }
            } else {
                return (long) statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
            }
        } else {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mApp = ((OSVApplication) getApplication());
        appPrefs = mApp.getAppPrefs();
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false) || SKMaps.getInstance().isSKMapsInitialized()) {
            mLibraryInitialized = true;
        }
        BackgroundThreadPool.post(new Runnable() {
            @Override
            public void run() {
                startService(new Intent(getBaseContext(), RecentClearedService.class));
                if ((appPrefs != null && !appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN))) {
                    Intent intent = new Intent(SplashActivity.this, WalkthroughActivity.class);
                    startActivityForResult(intent, REQUEST_ENABLE_INTRO);
                }
                SKMapSurfaceView.preserveGLContext = true;
                Utils.isMultipleMapSupportEnabled = false;
                String applicationPath = chooseStoragePath(SplashActivity.this);

                // determine path where map resources should be copied on the device
                if (applicationPath != null) {
                    mapResourcesDirPath = applicationPath + "/" + "SKMaps/";
                } else {
                    // show a dialog and then finish
                }
                ((OSVApplication) getApplication()).getAppPrefs().saveStringPreference("mapResourcesPath", mapResourcesDirPath);
                checkForUpdate();
                if (!new OSVFile(mapResourcesDirPath).exists()) {
//                    copyOtherResources();
                    SKVersioningManager.getInstance().setMapUpdateListener(SplashActivity.this);
                    Utils.initializeLibrary(SplashActivity.this, SplashActivity.this);
                } else if (!update) {
                    if (!appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false) && !mLibraryInitialized) {
                        long time = System.currentTimeMillis();
                        SKVersioningManager.getInstance().setMapUpdateListener(SplashActivity.this);
                        Utils.initializeLibrary(SplashActivity.this, SplashActivity.this);
                        Log.d(TAG, "run: initialized in " + (System.currentTimeMillis() - time) + " ms");
                    } else {
                        if (appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN) && mLibraryInitialized) {
                            BackgroundThreadPool.post(goToMapRunnable);
                        }
                    }
                }
            }
        });
    }

    private boolean shouldOpenMainScreen() {
        return (appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false) || SKMaps.getInstance().isSKMapsInitialized()) && mApp.isReady() && appPrefs
                .getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.register(this);
    }

    @Override
    protected void onStop() {
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BackgroundThreadPool.post(goToMapRunnable);
    }

    @WorkerThread
    private void goToMap() {
        if (shouldOpenMainScreen()) {
            BackgroundThreadPool.cancelTask(goToMapRunnable);
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            overridePendingTransition(0,0);
            startActivity(i);
            finish();
        } else {
            if (appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN)) {
                Handler h = new Handler(Looper.getMainLooper());
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BackgroundThreadPool.post(goToMapRunnable);
                    }
                }, 300);
            }
        }
    }

    @Subscribe(sticky = true)
    public void onAppReady(AppReadyEvent event) {
        Log.d(TAG, "onAppReady: ");
        EventBus.clear(AppReadyEvent.class);
        if (shouldOpenMainScreen()) {
            BackgroundThreadPool.post(goToMapRunnable);
        }
    }

    /**
     * Checks if the current version code is grater than the previous and overwrites the map resources.
     */
    private void checkForUpdate() {
        OSVApplication appContext = (OSVApplication) getApplication();
        int currentVersionCode = appContext.getAppPrefs().getIntPreference(ApplicationPreferences.CURRENT_VERSION_CODE);
        int versionCode = getVersionCode();
        if (currentVersionCode == 0) {
            appContext.getAppPrefs().setCurrentVersionCode(versionCode);
        }

        if (0 < currentVersionCode && currentVersionCode < versionCode) {
            update = true;
            appContext.getAppPrefs().setCurrentVersionCode(versionCode);
            Utils.deleteFileOrDirectory(new OSVFile(mapResourcesDirPath));
//            copyOtherResources();
            if (!appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false) && !mLibraryInitialized) {
                SKVersioningManager.getInstance().setMapUpdateListener(SplashActivity.this);
                Utils.initializeLibrary(SplashActivity.this, SplashActivity.this);
            }
        }

    }

    @Override
    public void onLibraryInitialized(boolean isSuccessful) {
        mLibraryInitialized = true;
        if (isSuccessful) {
//            copyOtherResources();
            SKVersioningManager.getInstance().setMapUpdateListener(this);
            if (appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN)) {
                BackgroundThreadPool.post(goToMapRunnable);
            }
        } else {
            //map was not initialized successfully
            Log.d(TAG, "onLibraryInitialized: failure initializing SKMaps");
            finish();
        }
    }

//    /**
//     * Copy some additional resources from assets
//     */
//    private void copyOtherResources() {
//        new Thread() {
//
//            public void run() {
//                try {
//                    boolean resAlreadyExist;
//
//                    String tracksPath = mapResourcesDirPath + "GPXTracks";
//                    File tracksDir = new File(tracksPath);
//                    resAlreadyExist = tracksDir.exists();
//                    if (!resAlreadyExist || update) {
//                        if (!resAlreadyExist) {
//                            tracksDir.mkdirs();
//                        }
//                        DemoUtils.copyAssetsToFolder(getAssets(), "GPXTracks", mapResourcesDirPath + "GPXTracks");
//                    }
//
//                    String imagesPath = mapResourcesDirPath + "images";
//                    File imagesDir = new File(imagesPath);
//                    resAlreadyExist = imagesDir.exists();
//                    if (!resAlreadyExist || update) {
//                        if (!resAlreadyExist) {
//                            imagesDir.mkdirs();
//                        }
//                        DemoUtils.copyAssetsToFolder(getAssets(), "images", mapResourcesDirPath + "images");
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
//    }

    /**
     * Returns the current version code
     * @return version code
     */
    private int getVersionCode() {
        int v = 0;
        try {
            v = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {}
        return v;
    }

    @Override
    public void onNewVersionDetected(int i) {
        Log.e("", " New version = " + i);
        newMapVersionDetected = i;
    }

    @Override
    public void onMapVersionSet(int i) {

    }

    @Override
    public void onVersionFileDownloadTimeout() {

    }

    @Override
    public void onNoNewVersionDetected() {

    }

}