package com.telenav.osv.activity;

import org.greenrobot.eventbus.Subscribe;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.util.NGXLoggingOption;
import com.skobbler.ngx.util.SKLogging;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.manager.playback.SafePlaybackManager;
import com.telenav.osv.manager.playback.VideoPlayerManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.utils.Log;
import androidx.fragment.app.FragmentTransaction;
import io.fabric.sdk.android.Fabric;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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

    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPrefs = getApp().getAppPrefs();
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
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.default_purple), PorterDuff.Mode.SRC_IN);
        mMapDisabled = !appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED, false);
        if (!mMapDisabled) {
            SKMaps.getInstance().setLogOption(NGXLoggingOption.LOGGING_OPTION_FILE_AND_CONSOLE, false);
            SKMaps.getInstance().setLogOption(NGXLoggingOption.LOGGING_OPTION_DISABLED, true);
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
        if (disposable != null) {
            disposable.dispose();
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
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (progressBar != null) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public void openScreen(int screenRecording) {

    }

    @Override
    public void openScreen(int screenNearby, Object extra) {

    }

    @Override
    public boolean hasPosition() {
        return false;
    }

    @Subscribe
    public void onFullScreenRequested(FullscreenEvent event) {
        if (event.fullscreen) {
            resizeHolders(1f, isPortrait());
        } else {
            resizeHolders(0.6f, isPortrait());
        }
    }

    private void openScreen(String sequenceId) {
        Context context = getApplicationContext();
        VideoLocalDataSource videoLocalDataSource = Injection.provideVideoDataSource(context);
        FrameLocalDataSource frameLocalDataSource = Injection.provideFrameLocalDataSource(context);
        SequenceLocalDataSource sequenceLocalDataSource = Injection.provideSequenceLocalDataSource(context,
                frameLocalDataSource,
                Injection.provideScoreLocalDataSource(context),
                Injection.provideLocationLocalDataSource(context),
                videoLocalDataSource);

        disposable = sequenceLocalDataSource
                .getSequenceWithAll(sequenceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onSuccess
                        sequence -> {
                            Log.d(TAG, String.format("openScreen. Status: success. Sequence id: %s. Message: Sequences loaded successful. ", sequenceId));
                            PlaybackManager player;
                            SequenceDetailsCompressionBase compressionBase = sequence.getCompressionDetails();
                            if (compressionBase instanceof SequenceDetailsCompressionJpeg) {
                                player = new SafePlaybackManager(PlayerActivity.this, sequence, frameLocalDataSource);
                            } else {
                                player = new VideoPlayerManager(PlayerActivity.this, sequence, videoLocalDataSource);
                            }
                            trackPreviewFragment = new TrackPreviewFragment();
                            trackPreviewFragment.setSource(player);
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.add(R.id.content_frame_large, trackPreviewFragment, TrackPreviewFragment.TAG);
                            if (!mMapDisabled) {
                                MapFragment mapFragment = new MapFragment();
                                mapFragment.setSource(player);
                                ft.add(R.id.content_frame_map, mapFragment, MapFragment.TAG);
                            }
                            ft.commitAllowingStateLoss();
                        },
                        //onError
                        throwable -> {
                            Log.d(TAG, String.format("openScreen. Status: complete. Sequence id: %s. Message: %s. ", sequenceId, throwable.getLocalizedMessage()));
                            Toast.makeText(this, R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
                            finish();
                        },
                        //OnComplete
                        () -> {
                            Log.d(TAG, String.format("openScreen. Status: complete. Sequence id: %s. Message: Local sequence was not found. ", sequenceId));
                            Toast.makeText(this, R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                );
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
        if (ratio == 1) {
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
        Intent activityIntent = getIntent();
        openScreen(activityIntent.getStringExtra(EXTRA_SEQUENCE_ID));
    }
}
