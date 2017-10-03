package com.telenav.osv.manager.network;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
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
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.io.File;

import static com.telenav.osv.data.Preferences.URL_ENV;

/**
 * abstract networking class
 * Created by Kalman on 02/05/2017.
 */
abstract class NetworkManager {

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

  final AccountPreferences appPrefs;

  private final MutableLiveData<Integer> serverType;

  private final LiveData<String> authToken;

  private final Observer<Integer> serverTypeObserver;

  private final Observer<String> authTokenObserver;

  int mCurrentServer = 0;

  /**
   * request queue for operations
   * adding a request here will be automatically run in the next available time
   */
  RequestQueue mQueue;

  private String mAccessToken;

  private HandlerThread mQueueThread;

  private Handler backgroundHandler;

  NetworkManager(Context context, AccountPreferences prefs) {
    this.mContext = context;
    mQueueThread = new HandlerThread("QueueThread", Process.THREAD_PRIORITY_BACKGROUND);
    mQueueThread.start();

    appPrefs = prefs;
    mCurrentServer = appPrefs.getServerType();
    mQueue = newRequestQueue(mContext, 4);
    authToken = appPrefs.getAuthTokenLive();
    authTokenObserver = s -> mAccessToken = s;
    authToken.observeForever(authTokenObserver);
    serverType = appPrefs.getServerTypeLive();
    serverTypeObserver = integer -> {
      mCurrentServer = integer == null ? 0 : integer;
      mQueue.cancelAll(request -> true);
      setEnvironment();
    };
    serverType.observeForever(serverTypeObserver);
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
      mAccessToken = appPrefs.getAuthToken();
    }
    return mAccessToken;
  }

  private void setEnvironment() {
    if (!Utils.isDebugBuild(mContext) && !appPrefs.isDebugEnabled()) {
      mCurrentServer = 0;
    }
    Log.d(TAG, "setEnvironment: " + URL_ENV[mCurrentServer]);
    setupUrls();
  }

  abstract void setupUrls();

  void destroy() {
    authToken.removeObserver(authTokenObserver);
    serverType.removeObserver(serverTypeObserver);
    mQueue.cancelAll(request -> true);
    backgroundHandler.postDelayed(() -> {
      try {
        HandlerThread thread = mQueueThread;
        mQueueThread = null;
        thread.quit();
      } catch (Exception ignored) {
        Log.d(TAG, Log.getStackTraceString(ignored));
      }
    }, 300);
  }

  public static class URL {

    public URL() {

    }
  }
}
