package com.telenav.osv.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.WorkerThread;

import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.event.AppReadyEvent;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;

import org.greenrobot.eventbus.Subscribe;

/**
 * Activity that installs required resources (from assets/MapResources.zip) to
 * the device
 */
public class SplashActivity extends Activity {

    public static final String RESTART_FLAG = "restartExtra";

    private static final int REQUEST_ENABLE_INTRO = 1;

    private static final String TAG = "SplashActivity";

    private ApplicationPreferences appPrefs;

    private KVApplication mApp;

    private Runnable goToMapRunnable = this::goToMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Fix for an issue which exists in some Android launchers where
        //the launch Activity is being restarted and added on top of the Activity stack,
        //when the app is being resumed by the launcher and not from recent activity.
        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_splash);
        mApp = ((KVApplication) getApplication());
        appPrefs = mApp.getAppPrefs();
        BackgroundThreadPool.post(() -> {
            startDataConsistencyMechanism();
            if ((appPrefs != null && !appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN))) {
                Intent intent = new Intent(SplashActivity.this, WalkthroughActivity.class);
                startActivityForResult(intent, REQUEST_ENABLE_INTRO);
            } else {
                if (appPrefs != null && appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED, false)) {
                    long time = System.currentTimeMillis();
                    Log.d(TAG, "run: initialized in " + (System.currentTimeMillis() - time) + " ms");
                }
                BackgroundThreadPool.post(goToMapRunnable);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BackgroundThreadPool.post(goToMapRunnable);
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
     * Stars the data consistency mechanism.
     */
    private void startDataConsistencyMechanism() {
        Context context = getApplicationContext();
        VideoLocalDataSource videoLocalDataSource = Injection.provideVideoDataSource(context);
        FrameLocalDataSource frameLocalDataSource = Injection.provideFrameLocalDataSource(context);
        LocationLocalDataSource locationLocalDataSource = Injection.provideLocationLocalDataSource(context);

        //starts the data consistency mechanism
        Injection.provideDataConsistency(
                Injection.provideSequenceLocalDataSource(context,
                        frameLocalDataSource,
                        Injection.provideScoreLocalDataSource(context),
                        locationLocalDataSource,
                        videoLocalDataSource),
                locationLocalDataSource,
                videoLocalDataSource,
                frameLocalDataSource,
                getApplicationContext()).start();
    }

    private boolean shouldOpenMainScreen() {
        return mApp.isReady() && appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN);
    }

    @WorkerThread
    private void goToMap() {
        if (shouldOpenMainScreen()) {
            BackgroundThreadPool.cancelTask(goToMapRunnable);
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            overridePendingTransition(0, 0);
            startActivity(i);
            finish();
        } else {
            if (appPrefs.getBooleanPreference(PreferenceTypes.K_INTRO_SHOWN)) {
                Handler h = new Handler(Looper.getMainLooper());
                h.postDelayed(() -> BackgroundThreadPool.post(goToMapRunnable), 300);
            }
        }
    }
}