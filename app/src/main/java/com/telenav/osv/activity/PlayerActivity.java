package com.telenav.osv.activity;

import javax.inject.Inject;
import javax.inject.Named;
import org.greenrobot.eventbus.Subscribe;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.crashlytics.android.Crashlytics;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.util.SKLogging;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.di.PlaybackModule;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.utils.Log;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.fabric.sdk.android.Fabric;

public class PlayerActivity extends OSVActivity implements HasSupportFragmentInjector {

    public static final String EXTRA_SEQUENCE_ID = "extraSequenceId";

    private static final String TAG = "PlayerActivity";

    @Inject
    @Named(PlaybackModule.SCOPE_MP4_LOCAL)
    PlaybackManager player;

    @Inject
    DispatchingAndroidInjector<Fragment> playerActivityFragmentInjector;

    @Inject
    SequenceDB db;

    private TrackPreviewFragment trackPreviewFragment;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ProgressBar progressBar;

    private FrameLayout mapHolder;

    private FrameLayout largeHolder;

    private LinearLayout mLinearLayout;

    private boolean mMapEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        if (Fabric.isInitialized()) {
            Crashlytics.setString(Log.PLAYBACK, "local-mp4");
        }

        mLinearLayout = findViewById(R.id.player_main_holder);
        mapHolder = findViewById(R.id.content_frame_map);
        largeHolder = findViewById(R.id.content_frame_large);
        progressBar = findViewById(R.id.progressbar);
        View backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
        mMapEnabled = appPrefs.isMapEnabled();
        if (mMapEnabled) {
            SKMaps.getInstance().setLogOption(SKMaps.NGXLoggingOption.LOGGING_OPTION_FILE_AND_CONSOLE, false);
            SKMaps.getInstance().setLogOption(SKMaps.NGXLoggingOption.LOGGING_OPTION_DISABLED, true);
            SKLogging.enableLogs(false);
        } else {
            resizeHolders(1f, isPortrait());
        }
        displayPreview();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mHandler.post(() -> {
            int orientation = newConfig.orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            resizeHolders(-1, portrait);
        });
    }

    @Override
    protected void onDestroy() {
        if (trackPreviewFragment != null) {
            trackPreviewFragment.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        displayPreview();
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
    protected void onPause() {
        EventBus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.splash_background_no_drawable));
        EventBus.register(this);
        mHandler.post(() -> {
            int orientation = getResources().getConfiguration().orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            resizeHolders(-1, portrait);
        });
    }

    @Override
    public OSVApplication getApp() {
        return (OSVApplication) getApplication();
    }

    @Override
    public void resolveLocationProblem(boolean b) {

    }

    @Override
    public void hideSnackBar() {

    }

    @Override
    public void showSnackBar(int tip_map_screen, int lengthLong, int got_it_label, Runnable runnable) {

    }

    @Override
    public void showSnackBar(CharSequence text, int duration, CharSequence button, Runnable onClick) {

    }

    @Override
    public void enableProgressBar(final boolean show) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public void openScreen(int screenNearby, Object extra) {

    }

    @Override
    public void openScreen(int screenRecording) {

    }

    @Override
    public int getCurrentScreen() {
        return SCREEN_PREVIEW;
    }

    @Override
    public boolean hasPosition() {
        return false;
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return playerActivityFragmentInjector;
    }

    @Subscribe
    public void onFullScreenRequested(FullscreenEvent event) {
        if (event.fullscreen) {
            resizeHolders(1f, isPortrait());
        } else {
            resizeHolders(0.6f, isPortrait());
        }
    }

    private void openScreen(final Object extra) {
        player.setSource((Sequence) extra);
        trackPreviewFragment = new TrackPreviewFragment();
        trackPreviewFragment.setDisplayData(player.getSequence());
        trackPreviewFragment.hideDelete(false);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.content_frame_large, trackPreviewFragment, TrackPreviewFragment.TAG);
        if (mMapEnabled) {
            MapFragment mapFragment = new MapFragment();
            mapFragment.setDisplayData(player.getSequence());
            ft.add(R.id.content_frame_map, mapFragment, MapFragment.TAG);
            trackPreviewFragment.setListener(mapFragment);
        }
        ft.commitAllowingStateLoss();
    }

    private void resizeHolders(float ratio, final boolean portrait) {
        Log.d(TAG, "resizeHolders: ratio = " + ratio);
        LinearLayout.LayoutParams lpu, lpl;
        if (ratio >= 0) {
            lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                    portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpu.weight = 1.0f - ratio;
            lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                    portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpl.weight = ratio;
        } else {
            float uw = ((LinearLayout.LayoutParams) mapHolder.getLayoutParams()).weight;
            float lw = ((LinearLayout.LayoutParams) largeHolder.getLayoutParams()).weight;
            lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                    portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpu.weight = uw;
            lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                    portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpl.weight = lw;
        }
        int intendedOrientation = portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL;
        mLinearLayout.setOrientation(intendedOrientation);
        mapHolder.setLayoutParams(lpu);
        largeHolder.setLayoutParams(lpl);
        Resources res = getResources();
        if (ratio >= 1.0f) {
            largeHolder.setPadding((int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                    (int) (res.getDimension(R.dimen.track_preview_card_padding_top) +
                            res.getDimension(R.dimen.track_preview_card_additional_padding_top)),
                    (int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                    (int) res.getDimension(R.dimen.track_preview_card_padding_bottom));
        } else {
            largeHolder.setPadding((int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                    (int) res.getDimension(R.dimen.track_preview_card_padding_top),
                    (int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                    (int) res.getDimension(R.dimen.track_preview_card_padding_bottom));
        }
        mLinearLayout.invalidate();
    }

    private void displayPreview() {
        Intent intent2 = getIntent();
        String path = intent2.getStringExtra(EXTRA_SEQUENCE_ID);
        if (path == null) {
            finish();
        }
        Sequence sequence = new LocalSequence(new OSVFile(path));
        Log.d(TAG, "displayPreview: " + sequence);
        openScreen(sequence);
    }
}
