package com.telenav.osv.activity;

import org.greenrobot.eventbus.Subscribe;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.playback.LocalPlaybackManager;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.utils.Log;
import io.fabric.sdk.android.Fabric;

public class PlayerActivity extends OSVActivity {

    public static final String EXTRA_SEQUENCE_ID = "extraSequenceId";

    private static final String TAG = "PlayerActivity";

    private TrackPreviewFragment trackPreviewFragment;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ProgressBar progressBar;

    private FrameLayout mapHolder;

    private FrameLayout largeHolder;

    private LinearLayout mLinearLayout;

    private boolean mMapDisabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPrefs = getApp().getAppPrefs();
        setContentView(R.layout.activity_player);
        if (Fabric.isInitialized()) {
            Crashlytics.setString(Log.PLAYBACK, "local-mp4");
        }
        SequenceDB.instantiate(this);

        mLinearLayout = (LinearLayout) findViewById(R.id.player_main_holder);
        mapHolder = (FrameLayout) findViewById(R.id.content_frame_map);
        largeHolder = (FrameLayout) findViewById(R.id.content_frame_large);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        View backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
        mMapDisabled = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false);
        if (!mMapDisabled) {
//            SKMaps.getInstance().setLogOption(SKMaps.NGXLoggingOption.LOGGING_OPTION_FILE_AND_CONSOLE, false);
//            SKMaps.getInstance().setLogOption(SKMaps.NGXLoggingOption.LOGGING_OPTION_DISABLED, true);
//            SKLogging.enableLogs(false);
        } else {
            resizeHolders(1f, isPortrait());
        }
        displayPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.register(this);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int orientation = getResources().getConfiguration().orientation;
                boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                resizeHolders(-1, portrait);
            }
        });
    }

    @Override
    protected void onPause() {
        EventBus.unregister(this);
        super.onPause();
    }

    public void openScreen(final Object extra) {
        PlaybackManager player;
        player = new LocalPlaybackManager(PlayerActivity.this, (Sequence) extra);

        trackPreviewFragment = new TrackPreviewFragment();
        trackPreviewFragment.setSource(player);
        trackPreviewFragment.hideDelete(false);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame_large, trackPreviewFragment, TrackPreviewFragment.TAG);
        if (!mMapDisabled) {
            MapFragment mapFragment = new MapFragment();
            mapFragment.setSource(player);
            ft.add(R.id.content_frame_map, mapFragment, MapFragment.TAG);
        }
        ft.commitAllowingStateLoss();
    }

    public void resizeHolders(float ratio, final boolean portrait) {
        Log.d(TAG, "resizeHolders: ratio = " + ratio);
        LinearLayout.LayoutParams lpu, lpl;
        if (ratio >= 0) {
            lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpu.weight = 1.0f - ratio;
            lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpl.weight = ratio;
        } else {
            float uw = ((LinearLayout.LayoutParams) mapHolder.getLayoutParams()).weight;
            float lw = ((LinearLayout.LayoutParams) largeHolder.getLayoutParams()).weight;
            lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpu.weight = uw;
            lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpl.weight = lw;
        }
        int intendedOrientation = portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL;
        mLinearLayout.setOrientation(intendedOrientation);
        mapHolder.setLayoutParams(lpu);
        largeHolder.setLayoutParams(lpl);
        mLinearLayout.invalidate();
    }

    @Subscribe
    public void onFullScreenRequested(FullscreenEvent event){
        if (event.fullscreen) {
            resizeHolders(1f,isPortrait());
        } else {
            resizeHolders(0.6f,isPortrait());
        }
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int orientation = newConfig.orientation;
                boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                resizeHolders(-1, portrait);
            }
        });
    }

    @Override
    public void cancelNearby() {

    }

    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        displayPreview();
    }

    private void displayPreview() {
        Intent intent2 = getIntent();
        String path = intent2.getStringExtra(EXTRA_SEQUENCE_ID);
        if (path == null) {
            finish();
        }
        Sequence sequence = new Sequence(new OSVFile(path));
        Log.d(TAG, "displayPreview: " + sequence);
        openScreen(sequence);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        if (trackPreviewFragment != null) {
            trackPreviewFragment.onBackPressed();
        } else {
            if (!getApp().isMainProcess()) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }

    @Override
    public void openScreen(int screenRecording) {

    }

    @Override
    public OSVApplication getApp() {
        return (OSVApplication) getApplication();
    }

    @Override
    public int getCurrentScreen() {
        return ScreenComposer.SCREEN_PREVIEW;
    }

    @Override
    public void resolveLocationProblem(boolean b) {

    }

    @Override
    public void showSnackBar(int tip_map_screen, int lengthLong, int got_it_label, Runnable runnable) {

    }

    @Override
    public void showSnackBar(CharSequence text, int duration, CharSequence button, Runnable onClick) {

    }

    @Override
    public void enableProgressBar(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    @Override
    public void openScreen(int screenNearby, Object extra) {

    }

    @Override
    protected void onDestroy() {
        if (trackPreviewFragment != null) {
            trackPreviewFragment.onDestroy();
        }
        super.onDestroy();
    }

}
