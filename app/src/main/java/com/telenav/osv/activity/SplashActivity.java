package com.telenav.osv.activity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import com.skobbler.ngx.SKPrepareMapTextureListener;
import com.skobbler.ngx.SKPrepareMapTextureThread;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.ngx.versioning.SKMapUpdateListener;
import com.skobbler.ngx.versioning.SKVersioningManager;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Activity that installs required resources (from assets/MapResources.zip) to
 * the device
 */
public class SplashActivity extends Activity implements SKPrepareMapTextureListener, SKMapUpdateListener {

    public static final long KILO = 1024;

    public static final long MEGA = KILO * KILO;

    public static final String RESTART_FLAG = "restartExtra";


    public static final String RESTART_PAGE = "restartPage";

    public static final String RESTART_FRAGMENT = "restartFragment";

    public static final String RESTART_SEQUENCE_ONLINE = "restartSequenceOnline";

    public static final String RESTART_SEQUENCE_ID = "restartSequenceId";

    public static final String RESTART_SEQUENCE_INDEX = "restartSequenceIndex";

    private static final int REQUEST_ENABLE_INTRO = 1;

    private static final String TAG = "SplashActivity";

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 113;

    private static final long SPLASH_TIME = 700;

    /**
     * Path to the MapResources directory
     */
    public static String mapResourcesDirPath = "";

    public static int newMapVersionDetected = 0;

    private boolean update = false;


    private boolean afterCrash = false;

    private long mEnteredTime = 0;

    private ApplicationPreferences appPrefs;


    public static String chooseStoragePath(Context context) {
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
    public static long getAvailableMemorySize(String path) {
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
        mEnteredTime = System.currentTimeMillis();
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(getResources().getColor(R.color.md_grey_300));
//        }
        Intent intent = getIntent();
        if (intent != null) {
            afterCrash = intent.getBooleanExtra(RESTART_FLAG, false);
            if (afterCrash) {
//                MainActivity.sCurrentScreen = intent.getIntExtra(SplashActivity.RESTART_PAGE, -1);
//                MainActivity.sFragmentOverlayTag = intent.getStringExtra(SplashActivity.RESTART_FRAGMENT);
                int sequenceId = intent.getIntExtra(RESTART_SEQUENCE_ID, -1);
                if (sequenceId != -1) {
                    boolean online = intent.getBooleanExtra(SplashActivity.RESTART_SEQUENCE_ONLINE, false);
//                    MainActivity.sLastSequenceIndex = intent.getIntExtra(SplashActivity.RESTART_SEQUENCE_INDEX, 0);
//                    MainActivity.sLastSequence = sequenceId;
//                    Log.d(TAG, "onCreate: after crash page = " + MainActivity.sCurrentScreen + ", fragment = " + MainActivity.sFragmentOverlayTag + ", sequenceId = " +
//                            sequenceId +
//                            ", online = " + online);
                }
            }
        }
        boolean multipleMapSupport = false;

        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            if (bundle != null) {
                multipleMapSupport = bundle.getBoolean("provideMultipleMapSupport");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (multipleMapSupport) {
            SKMapSurfaceView.preserveGLContext = false;
            Utils.isMultipleMapSupportEnabled = true;
        }

        String applicationPath = chooseStoragePath(this);

        // determine path where map resources should be copied on the device
        if (applicationPath != null) {
            mapResourcesDirPath = applicationPath + "/" + "SKMaps/";
        } else {
            // show a dialog and then finish
        }
        ((OSVApplication) getApplication()).getAppPrefs().saveStringPreference("mapResourcesPath", mapResourcesDirPath);
        checkForUpdate();
        if (!new OSVFile(mapResourcesDirPath).exists()) {
            // copy some other resource needed
            new SKPrepareMapTextureThread(this, mapResourcesDirPath, "SKMaps.zip", this).start();
            copyOtherResources();
        } else if (!update) {
            final OSVApplication app = (OSVApplication) getApplication();
            Utils.initializeLibrary(this);
            SKVersioningManager.getInstance().setMapUpdateListener(this);
                long diff = System.currentTimeMillis() - mEnteredTime;
                if (diff < SPLASH_TIME) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            goToMap();
                        }
                    }, SPLASH_TIME - diff);
                } else {
                    goToMap();
                }
            }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        long diff = System.currentTimeMillis() - mEnteredTime;
        if (diff < SPLASH_TIME) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToMap();
                }
            }, SPLASH_TIME - diff);
        } else {
            goToMap();
        }
    }

    @Override
    public void onMapTexturesPrepared(boolean prepared) {
        SKVersioningManager.getInstance().setMapUpdateListener(this);
        if (Utils.initializeLibrary(this)) {
                long diff = System.currentTimeMillis() - mEnteredTime;
                if (diff < SPLASH_TIME) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            goToMap();
                        }
                    }, SPLASH_TIME - diff);
                } else {
                    goToMap();
                }
            }

    }

    private void goToMap() {
        Intent i = new Intent(this, MainActivity.class);
        if (afterCrash) {
            i.putExtra(SplashActivity.RESTART_FLAG, true);
        }
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    /**
     * Copy some additional resources from assets
     */
    private void copyOtherResources() {
        new Thread() {

            public void run() {
                try {
                    String tracksPath = mapResourcesDirPath + "GPXTracks";
                    OSVFile tracksDir = new OSVFile(tracksPath);
                    if (!tracksDir.exists()) {
                        tracksDir.mkdirs();
                    }
                    Utils.copyAssetsToFolder(getAssets(), "GPXTracks", mapResourcesDirPath + "GPXTracks");

                    String imagesPath = mapResourcesDirPath + "images";
                    OSVFile imagesDir = new OSVFile(imagesPath);
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs();
                    }
                    Utils.copyAssetsToFolder(getAssets(), "images", mapResourcesDirPath + "images");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    @Override
    public void onMapVersionSet(int newVersion) {

    }

    @Override
    public void onNewVersionDetected(int newVersion) {
        Log.e("", "new version " + newVersion);
        newMapVersionDetected = newVersion;
    }

    @Override
    public void onNoNewVersionDetected() {

    }

    @Override
    public void onVersionFileDownloadTimeout() {

    }

    /**
     * Checks if the current version code is grater than the previous and overwrites the map resources.
     */
    public void checkForUpdate() {
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
            new SKPrepareMapTextureThread(this, mapResourcesDirPath, "SKMaps.zip", this).start();
            copyOtherResources();
        }

    }

    /**
     * Returns the current version code
     * @return
     */
    public int getVersionCode() {
        int v = 0;
        try {
            v = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return v;
    }


}
