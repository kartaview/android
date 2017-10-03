package com.telenav.osv.application;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatDelegate;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.squareup.leakcanary.LeakCanary;
import com.telenav.osv.R;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.di.AppInjector;
import com.telenav.osv.event.AppReadyEvent;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.ui.binding.viewmodel.DefaultBindingComponent;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import io.fabric.sdk.android.Fabric;
import java.util.Date;
import javax.inject.Inject;

/**
 * Manages the application itself (on top of the activity), mainly to force Camera getting
 * closed in case of crash.
 */
public class OSVApplication extends Application implements HasActivityInjector, HasServiceInjector {

  public static final int START_RECORDING_PERMISSION = 111;

  public static final int CAMERA_PERMISSION = 112;

  public static final int LOCATION_PERMISSION = 113;

  public static final int LOCATION_PERMISSION_BT = 114;

  public static final Date runTime = new Date(System.currentTimeMillis());

  private static final String TAG = "OSVApplication";

  public static String VERSION_NAME = "";

  @Inject
  DispatchingAndroidInjector<Activity> activityInjector;

  @Inject
  DispatchingAndroidInjector<Service> serviceInjector;

  /**
   * Object for accessing application preferences
   */
  @Inject
  Preferences appPrefs;//todo check order

  @Inject
  SequenceDB mSequenceDB;

  @Inject
  Recorder mRecorder;

  private boolean isDebug;

  private Thread.UncaughtExceptionHandler mDefaultExHandler;

  private boolean mIsMainProcess;

  private Thread.UncaughtExceptionHandler mExHandler = (thread, ex) -> {
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
    appPrefs.setCrashed(false);
    if (Looper.myLooper() == Looper.getMainLooper()) {
      if (mRecorder != null) {
        Log.e(TAG, "Uncaught exception! Closing down camera safely firsthand");
        mRecorder.forceCloseCamera();
        stopService(new Intent(OSVApplication.this, CameraHandlerService.class));
      }
      Log.w(TAG, "uncaughtException: on ui thread");
      if (isDebug) {
        mDefaultExHandler.uncaughtException(thread, ex);
      } else {
        int restartedUntilNow = appPrefs.getRestartCounter();
        if (restartedUntilNow <= 2) {
          appPrefs.setRestartCounter(restartedUntilNow + 1);
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
  };

  private boolean mReady;

  public boolean isMainProcess() {
    return mIsMainProcess;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    AppInjector.init(this);
    DataBindingUtil.setDefaultComponent(new DefaultBindingComponent());
    LeakCanary.install(this);
    try {
      VERSION_NAME = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception e) {
      Log.w(TAG, "onCreate: " + Log.getStackTraceString(e));
    }
    Log.d(TAG, "onCreate: time " + System.currentTimeMillis());
    Log.d(TAG, "onCreate: " + Build.MANUFACTURER + " " + android.os.Build.MODEL + ";" + Build.VERSION.RELEASE + ";" +
        OSVApplication.VERSION_NAME);

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
        boolean crashed = appPrefs.getCrashed();
        boolean safe = appPrefs.isSafeMode();
        int counter = appPrefs.getFfmpegCrashCounter();
        if (crashed && !safe && counter >= 2) {
          Log.d(TAG, "onCreate: K_CRASHED is true, showing message and setting safe mode");
          appPrefs.setFfmpegCrashCounter(0);
          appPrefs.setShouldShowSafeModeMessage(true);
          appPrefs.setSafeMode(true);
        }

        appPrefs.setCrashed(true);
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
        Log.w(TAG, "onCreate: " + e.getLocalizedMessage());
      }
      Utils.getSelectedStorage(OSVApplication.this, appPrefs);
      try {
        Fabric.with(new Fabric.Builder(OSVApplication.this).kits(new Crashlytics(), new CrashlyticsNdk(), new Answers()).build());
        Crashlytics.setBool(Log.RECORD_STATUS, false);
        Crashlytics.setString(Log.LOG_FILE, Log.getLogFile().getAbsolutePath());
        Crashlytics.setBool(Log.SDK_ENABLED, appPrefs.isMapEnabled());
        Answers.getInstance()
            .logCustom(new CustomEvent("New app session")
                           .putCustomAttribute(Log.SDK_ENABLED, "" + appPrefs.isMapEnabled()));
        Crashlytics.setBool(Log.POINTS_ENABLED, appPrefs.isGamificationEnabled());
        Crashlytics.setBool(Log.UPLOAD_STATUS, false);
        Crashlytics.setString(Log.PLAYBACK, "none");
        int type = appPrefs.getUserType();
        Crashlytics.setInt(Log.USER_TYPE, type);
        Crashlytics.setBool(Log.SAFE_RECORDING, appPrefs.isSafeMode());
        Crashlytics.setBool(Log.STATIC_FOCUS, appPrefs.isStaticFocus());
        Crashlytics.setBool(Log.CAMERA_API_NEW, appPrefs.isNewCameraApi());
        Crashlytics.setUserIdentifier(appPrefs.getUserId());
        Crashlytics.setUserName(appPrefs.getUserName());
        Log.d(TAG, "Crashlytics: initialized");
        if (!isDebug && mIsMainProcess) {
          new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
              PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
              int version = pInfo.versionCode;
              String versionName = pInfo.versionName;
              int savedVersion = (int) appPrefs.getVersionCode();
              if (savedVersion != version) {
                if (Fabric.isInitialized()) {
                  Answers.getInstance().logCustom(new CustomEvent("UpdateEvent").putCustomAttribute(Log.OLD_VERSION, savedVersion)
                                                      .putCustomAttribute(Log.NEW_VERSION, version));
                }
                if (version == 68 || (savedVersion < 68 && version >= 69)) {
                  appPrefs.setSafeMode(false);
                  appPrefs.setShouldShowSafeModeMessage(false);
                }
                appPrefs.setVersionCode(version);
                appPrefs.setVersionName(versionName);
                Log.d(TAG, "onCreate: new versionCode! " + version);
              }
            } catch (Exception e) {
              Log.w(TAG, "onCreate: " + e.getLocalizedMessage());
            }
          }, 10000);
        }
      } catch (Exception e) {
        Log.e(TAG, "onCreate: " + Log.getStackTraceString(e));
      }
      String arch = System.getProperty("os.arch");
      Log.d(TAG, "onCreate: architecture is " + arch);
      if (mIsMainProcess) {
        mDefaultExHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mExHandler);
      }
      SKReverseGeocoderManager.getInstance();
      if (mIsMainProcess) {
        consistencyCheck();
      }
      mReady = true;
      EventBus.postSticky(new AppReadyEvent());
    });
  }

  public void consistencyCheck() {
    BackgroundThreadPool.post(() -> {
      mSequenceDB.consistencyCheck(OSVApplication.this);
      LocalSequence.forceRefreshLocalSequences();

      Log.d(TAG, "consistencyCheck: starting");
      try {
        OSVFile osv = Utils.generateOSVFolder(OSVApplication.this, appPrefs);
        for (OSVFile folder : osv.listFiles()) {
          if (folder.getName().contains("&")) {
            Log.d(TAG, "consistencyCheck: renaming " + folder.getName());
            OSVFile file = new OSVFile(folder.getParentFile(), folder.getName().replace("&", ""));
            folder.renameTo(file);
            folder = file;
            Log.d(TAG, "consistencyCheck: renamed to " + folder.getName());
          }
          OSVFile[] imgs = folder.listFiles((dir, filename) -> filename.contains(".jpg") || filename.contains(".mp4"));
          if (imgs.length == 0) {
            folder.delete();
          } else {
            for (OSVFile img : imgs) {
              try {
                if (img.getName().endsWith("tmp")) {
                  String seqId = folder.getName().split("_")[1];
                  String index = img.getName().split("\\.")[0];
                  mSequenceDB.deleteVideo(Integer.valueOf(seqId), Integer.valueOf(index));
                  img.delete();
                }
              } catch (Exception e) {
                Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
              }
            }
            if (folder.listFiles((dir, filename) -> filename.contains(".jpg") || filename.contains(".mp4")).length == 0) {
              folder.delete();
            }
          }
        }
      } catch (Exception e) {
        Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
      }
      Log.d(TAG, "consistencyCheck: done.");
    });
  }

  public boolean isReady() {
    return mReady;
  }

  @Override
  public DispatchingAndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }

  @Override
  public DispatchingAndroidInjector<Service> serviceInjector() {
    return serviceInjector;
  }
}
