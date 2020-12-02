package com.telenav.osv.manager.network;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl;
import com.telenav.osv.utils.BackgroundThreadPool;

import java.io.File;

/**
 * abstract networking class
 * Created by Kalman on 02/05/2017.
 */
public abstract class NetworkManager {

    static final int UPLOAD_REQUEST_TIMEOUT = 30000;

    private static final String TAG = "NetworkManager";

    /**
     * context used for operations, should use application context
     */
    final Context mContext;

    final ApplicationPreferences appPrefs;

    protected FactoryServerEndpointUrl factoryServerEndpointUrl;

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

        appPrefs = ((KVApplication) mContext.getApplicationContext()).getAppPrefs();
        //ToDo: remove the injection from inside the constructor to a parameter
        factoryServerEndpointUrl = Injection.provideNetworkFactoryUrl(appPrefs);
        mQueue = newRequestQueue(mContext, 4);
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     * @param context A {@link Context} to use for creating the cache dir.
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

    void destroy() {
        mQueue.cancelAll(new RequestQueue.RequestFilter() {

            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        backgroundHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                try {
                    HandlerThread thread = mQueueThread;
                    mQueueThread = null;
                    thread.quit();
                } catch (Exception ignored) {
                }
            }
        }, 300);
    }
}
