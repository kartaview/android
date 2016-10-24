package com.telenav.osv.activity;

import android.content.Intent;
import android.content.res.Configuration;
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
import com.google.android.gms.common.api.Status;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.util.SKLogging;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.LocalPlaybackManager;
import com.telenav.osv.manager.PlaybackManager;
import com.telenav.osv.ui.fragment.CameraControlsFragment;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

public class PlayerActivity extends OSVActivity {

    public static final String EXTRA_SEQUENCE_ID = "extraSequenceId";

    private static final String TAG = "PlayerActivity";

    private LinearLayout mFragmentsContainer;

    private FrameLayout mUpperFragmentHolder;

    private FrameLayout mLowerFragmentHolder;

    private TrackPreviewFragment trackPreviewFragment;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
        SKMaps.getInstance().setLogOption(SKMaps.NGXLoggingOption.LOGGING_OPTION_FILE_AND_CONSOLE, false);
        SKMaps.getInstance().setLogOption(SKMaps.NGXLoggingOption.LOGGING_OPTION_DISABLED, true);
        SKLogging.enableLogs(false);
        displayPreview();
    }

    public void openScreen(final Object extra) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MapFragment mapFragment = new MapFragment();
                mapFragment.enterMode(MapFragment.MODE_PREVIEW);
                try {
                    getSupportFragmentManager().popBackStackImmediate();
                } catch (IllegalStateException e) {
                    Log.d(TAG, "error: popBackStackImmediate failed");
                }
                PlaybackManager player;
                player = new LocalPlaybackManager(PlayerActivity.this, (Sequence) extra);
                mapFragment.setSource(player);
                trackPreviewFragment = new TrackPreviewFragment();
                trackPreviewFragment.setSource(player);
                trackPreviewFragment.hideDelete(false);
                displayFragmentsParallel(0.6f, mapFragment, trackPreviewFragment, TrackPreviewFragment.TAG, MapFragment.TAG);
            }
        });
    }

    private void resizeHolders(float ratio, final boolean portrait) {
        resizeHolderStatic(ratio, portrait);
    }

    public void resizeHolderStatic(float ratio, final boolean portrait) {
        Log.d(TAG, "resizeHolders: ratio = " + ratio);
        LinearLayout.LayoutParams lpu, lpl;
        if (ratio >= 0) {
            lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpu.weight = 1.0f - ratio;
            lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpl.weight = ratio;
        } else {
            float uw = ((LinearLayout.LayoutParams) mUpperFragmentHolder.getLayoutParams()).weight;
            float lw = ((LinearLayout.LayoutParams) mLowerFragmentHolder.getLayoutParams()).weight;
            lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpu.weight = uw;
            lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
            lpl.weight = lw;
        }
        int intendedOrientation = portrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL;
//        boolean reorient = mFragmentsContainer.getOrientation() != intendedOrientation;
//        if (reorient) {
        mFragmentsContainer.setOrientation(intendedOrientation);
//        }
        mUpperFragmentHolder.setLayoutParams(lpu);
        mLowerFragmentHolder.setLayoutParams(lpl);
        mFragmentsContainer.invalidate();
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
    public boolean needsCameraPermission() {
        return false;
    }

    @Override
    public void setNeedsCameraPermission(boolean needs) {

    }

    private void displayFragmentsParallel(float v, Fragment upper, Fragment lower, String tag, String secondaryTag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.addToBackStack(tag);
        if (upper != null) {
            if (upper.isAdded()) {
                ft.remove(upper);
            }
            ft.add(R.id.content_frame_upper, upper, tag);
        }
        if (lower != null) {
            if (lower.isAdded()) {
                ft.remove(lower);
            }
            ft.add(R.id.content_frame_lower, lower, secondaryTag);
        }
        resizeHolders(v, isPortrait());
        ft.commitAllowingStateLoss();
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
        mFragmentsContainer = (LinearLayout) findViewById(R.id.fragments_container);
        mUpperFragmentHolder = (FrameLayout) findViewById(R.id.content_frame_upper);
        mLowerFragmentHolder = (FrameLayout) findViewById(R.id.content_frame_lower);
        Sequence sequence = new Sequence(new OSVFile(path));
        Log.d(TAG, "displayPreview: " + sequence);
        openScreen(sequence);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        if (trackPreviewFragment != null){
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
        return MainActivity.SCREEN_PREVIEW;
    }

    @Override
    public void resolveLocationProblem(boolean b) {

    }

    @Override
    public void showSnackBar(CharSequence s, int lengthShort) {

    }
    @Override
    public void showSnackBar(int s, int lengthShort) {

    }

    @Override
    public String getCurrentFragment() {
        return TrackPreviewFragment.TAG;
    }

    @Override
    public void showSnackBar(int tip_map_screen, int lengthLong, int got_it_label, Runnable runnable) {

    }

    @Override
    public void continueAfterCrash() {

    }

    @Override
    public void switchPreviews() {

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
    public void setLocationResolution(Status status) {

    }

    @Override
    protected void onDestroy() {
        if (trackPreviewFragment != null){
            trackPreviewFragment.onDestroy();
        }
        super.onDestroy();
    }
}
