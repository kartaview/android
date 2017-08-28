package com.telenav.osv.manager.network;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.io.File;

/**
 * abstract networking class
 * Created by Kalman on 02/05/2017.
 */
abstract class NetworkManager {

  public static final String[] URL_ENV =
      {"openstreetcam.org/", "staging.openstreetcam.org/", "testing.openstreetcam.org/", "beta.openstreetcam.org/"};

  /**
   * version number, when it will be added to backend
   */
  static final String URL_VER = "1.0/";

  static final int UPLOAD_REQUEST_TIMEOUT = 30000;

  private static final String TAG = "NetworkManager";

  /**
   * context used for operations, should use application context
   */
  final Context mContext;

  final ApplicationPreferences appPrefs;

  public int mCurrentServer = 0;

  /**
   * request queue for operations
   * adding a request here will be automatically run in the next available time
   */
  RequestQueue mQueue;

  String mAccessToken;

  private HandlerThread mQueueThread;

  private Handler backgroundHandler;

  NetworkManager(Context context) {
    this.mContext = context;
    mQueueThread = new HandlerThread("QueueThread", Process.THREAD_PRIORITY_BACKGROUND);
    mQueueThread.start();

    appPrefs = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs();
    mCurrentServer = appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE);
    mQueue = newRequestQueue(mContext, 4);
  }

  /**
   * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
   *
   * @param context A {@link Context} to use for creating the cache dir.
   *
   * @return A started {@link RequestQueue} instance.
   */
  RequestQueue newRequestQueue(Context context, int nrOfThreads) {
    File cacheDir = new File(context.getCacheDir(), "volley");
    HttpStack stack = new HurlStack();
    Network network = new BasicNetwork(stack);
    if (mQueueThread == null) {
      mQueueThread = new HandlerThread("QueueThread", Process.THREAD_PRIORITY_BACKGROUND);
    }
    backgroundHandler = new Handler(mQueueThread.getLooper());
    RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, nrOfThreads, new ExecutorDelivery(backgroundHandler));
    queue.start();

    return queue;
  }

  void runInBackground(Runnable runnable) {
    BackgroundThreadPool.post(runnable);
  }

  String getAccessToken() {
    if (mAccessToken == null) {
      mAccessToken = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);
    }
    return mAccessToken;
  }

  void setEnvironment() {
    if (!Utils.isDebugBuild(mContext) && !appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED)) {
      mCurrentServer = 0;
    }
    Log.d(TAG, "setEnvironment: " + URL_ENV[mCurrentServer]);
  }

  void destroy() {
    mQueue.cancelAll(request -> true);
    backgroundHandler.postDelayed(() -> {
      try {
        HandlerThread thread = mQueueThread;
        mQueueThread = null;
        thread.quit();
      } catch (Exception ignored) {
      }
    }, 300);
  }
}
