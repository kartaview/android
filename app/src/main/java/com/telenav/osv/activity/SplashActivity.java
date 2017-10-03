package com.telenav.osv.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitializationListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.ngx.versioning.SKMapVersioningListener;
import com.skobbler.ngx.versioning.SKVersioningManager;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.di.Injectable;
import com.telenav.osv.event.AppReadyEvent;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.service.RecentClearedService;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;

/**
 * Activity that installs required resources (from assets/MapResources.zip) to
 * the device
 */
public class SplashActivity extends AppCompatActivity implements SKMapsInitializationListener, SKMapVersioningListener, Injectable {

  public static final String RESTART_FLAG = "restartExtra";

  private static final long KILO = 1024;

  private static final long MEGA = KILO * KILO;

  private static final int REQUEST_ENABLE_INTRO = 1;

  private static final String TAG = "SplashActivity";

  /**
   * Path to the MapResources directory
   */
  public static String mapResourcesDirPath = "";

  @Inject
  Preferences appPrefs;

  private boolean update = false;

  private boolean mLibraryInitialized;

  private OSVApplication mApp;

  private Runnable goToMapRunnable = this::goToMap;

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

    SKLogging.writeLog(TAG, "There is not enough memory on any storage, but return internal memory", SKLogging.LOG_DEBUG);

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
   *
   * @return available memory size in bytes
   */
  private static long getAvailableMemorySize(String path) {
    StatFs statFs = null;
    try {
      statFs = new StatFs(path);
    } catch (IllegalArgumentException ex) {
      SKLogging.writeLog("SplashActivity", "Exception when creating StatF ; message = " + ex, SKLogging.LOG_DEBUG);
    }
    if (statFs != null) {
      Method getAvailableBytesMethod = null;
      try {
        getAvailableBytesMethod = statFs.getClass().getMethod("getAvailableBytes");
      } catch (NoSuchMethodException e) {
        SKLogging.writeLog(TAG, "Exception at getAvailableMemorySize method = " + e.getMessage(), SKLogging.LOG_DEBUG);
      }

      if (getAvailableBytesMethod != null) {
        try {
          SKLogging.writeLog(TAG, "Using new API for getAvailableMemorySize method !!!", SKLogging.LOG_DEBUG);
          return (Long) getAvailableBytesMethod.invoke(statFs);
        } catch (IllegalAccessException | InvocationTargetException e) {
          return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        }
      } else {
        return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
      }
    } else {
      return 0;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    BackgroundThreadPool.post(goToMapRunnable);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);
    mApp = ((OSVApplication) getApplication());
    if (!appPrefs.isMapEnabled() || SKMaps.getInstance().isSKMapsInitialized()) {
      mLibraryInitialized = true;
    }
    BackgroundThreadPool.post(() -> {
      startService(new Intent(getBaseContext(), RecentClearedService.class));
      if ((appPrefs != null && appPrefs.shouldShowWalkthrough())) {
        Intent intent = new Intent(SplashActivity.this, WalkthroughActivity.class);
        startActivityForResult(intent, REQUEST_ENABLE_INTRO);
      }
      SKMapSurfaceView.preserveGLContext = true;
      Utils.isMultipleMapSupportEnabled = false;
      String applicationPath = chooseStoragePath(SplashActivity.this);

      // determine path where map resources should be copied on the device
      if (applicationPath != null) {
        mapResourcesDirPath = applicationPath + "/" + "SKMaps/";
      }
      appPrefs.setMapResourcesPath(mapResourcesDirPath);
      checkForUpdate();
      if (!new OSVFile(mapResourcesDirPath).exists()) {
        SKVersioningManager.getInstance().setMapUpdateListener(SplashActivity.this);
        Utils.initializeLibrary(SplashActivity.this, appPrefs, SplashActivity.this);
      } else if (!update) {
        if (appPrefs.isMapEnabled() && !mLibraryInitialized) {
          long time = System.currentTimeMillis();
          SKVersioningManager.getInstance().setMapUpdateListener(SplashActivity.this);
          Utils.initializeLibrary(SplashActivity.this, appPrefs, SplashActivity.this);
          Log.d(TAG, "run: initialized in " + (System.currentTimeMillis() - time) + " ms");
        } else {
          if (!appPrefs.shouldShowWalkthrough() && mLibraryInitialized) {
            BackgroundThreadPool.post(goToMapRunnable);
          }
        }
      }
    });
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

  private boolean shouldOpenMainScreen() {
    return (!appPrefs.isMapEnabled() || SKMaps.getInstance().isSKMapsInitialized()) &&
        mApp.isReady() && !appPrefs.shouldShowWalkthrough();
  }

  @WorkerThread
  private void goToMap() {
    if (shouldOpenMainScreen()) {
      BackgroundThreadPool.cancelTask(goToMapRunnable);
      Intent i = new Intent(this, MainActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      overridePendingTransition(0, 0);
      SKVersioningManager.getInstance().setMapUpdateListener(null);
      startActivity(i);
      finish();
    } else {
      if (!appPrefs.shouldShowWalkthrough()) {
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> BackgroundThreadPool.post(goToMapRunnable), 300);
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
    int lastVersionCode = appPrefs.getVersionCodeForSdk();
    int versionCode = getVersionCode();
    if (lastVersionCode != versionCode) {
      update = true;
      appPrefs.setVersionCodeForSdk(versionCode);
      Utils.deleteFileOrDirectory(new OSVFile(mapResourcesDirPath));
      if (appPrefs.isMapEnabled() && !mLibraryInitialized) {
        SKVersioningManager.getInstance().setMapUpdateListener(SplashActivity.this);
        Utils.initializeLibrary(SplashActivity.this, appPrefs, SplashActivity.this);
      }
    }
  }

  @Override
  public void onLibraryInitialized(boolean isSuccessful) {
    mLibraryInitialized = true;
    if (isSuccessful) {
      SKVersioningManager.getInstance().setMapUpdateListener(this);
      if (!appPrefs.shouldShowWalkthrough()) {
        BackgroundThreadPool.post(goToMapRunnable);
      }
    } else {
      //map was not initialized successfully
      Log.d(TAG, "onLibraryInitialized: failure initializing SKMaps");
      finish();
    }
  }

  /**
   * Returns the current version code
   *
   * @return version code
   */
  private int getVersionCode() {
    int v = 0;
    try {
      v = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException ignored) {
      Log.d(TAG, Log.getStackTraceString(ignored));
    }
    return v;
  }

  @Override
  public void onNewVersionDetected(int i) {
    Log.e("", " New version = " + i);
  }

  @Override
  public void onMapVersionSet(int i) {
    //nothing
  }

  @Override
  public void onVersionFileDownloadTimeout() {
    //nothing
  }

  @Override
  public void onNoNewVersionDetected() {
    //nothing
  }
}