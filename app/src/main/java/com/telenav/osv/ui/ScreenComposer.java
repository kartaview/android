package com.telenav.osv.ui;

import java.util.Arrays;
import java.util.Stack;
import org.greenrobot.eventbus.Subscribe;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.R;
import com.telenav.osv.activity.LocationPermissionsListener;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.CameraPreviewContainerEvent;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.manager.playback.OnlinePlaybackManager;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.manager.playback.SafePlaybackManager;
import com.telenav.osv.manager.playback.VideoPlayerManager;
import com.telenav.osv.obd.ObdActivity;
import com.telenav.osv.ui.custom.FixedFrameLayout;
import com.telenav.osv.ui.fragment.ByodProfileFragment;
import com.telenav.osv.ui.fragment.DriverGuideFragment;
import com.telenav.osv.ui.fragment.FullscreenFragment;
import com.telenav.osv.ui.fragment.LeaderboardFragment;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.NearbyFragment;
import com.telenav.osv.ui.fragment.OSVFragment;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.ui.fragment.SimpleProfileFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.ui.fragment.UserProfileFragment;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import io.fabric.sdk.android.Fabric;

/**
 * The class responsible for displaying the different screens
 * Created by Kalman on 17/01/2017.
 */
public class ScreenComposer implements ScreenDecorator.OnNavigationListener {

    //  ==========================SCREENS==============================

    public static final int SCREEN_MAP = 0;

    public static final int SCREEN_RECORDING = 1;

    public static final int SCREEN_MY_PROFILE = 2;

    @SuppressWarnings("WeakerAccess")
    public static final int SCREEN_SETTINGS = 3;

    public static final int SCREEN_PREVIEW = 4;

    @SuppressWarnings("WeakerAccess")
    public static final int SCREEN_WAITING = 6;

    public static final int SCREEN_NEARBY = 9;

    public static final int SCREEN_LEADERBOARD = 10;

    public static final int SCREEN_REPORT = 12;

    @SuppressWarnings("WeakerAccess")
    public static final int SCREEN_PREVIEW_FULLSCREEN = 13;

    public static final int SCREEN_DRIVER_GUIDE = 14;

    private final static String TAG = "ScreenComposer";

    //  ==========================STATES==============================

    private static final int STATE_MAP = 1;

    private static final int STATE_RECORDING = 3;

    private static final int STATE_SPLIT = 5;

    private static final int STATE_SPLIT_FULLSCREEN = 7;

    private static final float SPLIT_RATIO_LARGE = 0.64f;

    private static final float SPLIT_RATIO_SMALL = 0.36f;

    private final OSVActivity activity;

    private final FragmentManager mFragmentManager;

    private final ScreenDecorator mDecorator;

    private final FrameLayout.LayoutParams fullScreenLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private MapFragment mapFragment;

    private Stack<Integer> mScreenStack = new Stack<>();

    private FrameLayout mapHolder;

    private FrameLayout previewHolder;

    private FrameLayout controlsHolder;

    private FrameLayout largeHolder;

    private View cameraPreviewBackgroundView;

    private FixedFrameLayout mRecordingFeedbackLayout;

    private FrameLayout.LayoutParams recordSmallLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private FrameLayout.LayoutParams recordLargeLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private FrameLayout.LayoutParams controlsHolderLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private FrameLayout.LayoutParams splitSixLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private FrameLayout.LayoutParams splitFourLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private FrameLayout.LayoutParams splitHiddenLP =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private int mPreviewSizeLarge;

    private int mPreviewSizeSmall;

    private int mFittedPreviewLength;

    private int mFittedControlsLength;

    private AlertDialog mExitDialog;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ApplicationPreferences appPrefs;

    private ProgressBar progressBar;

    private FrameLayout smallHolder;

    private int mCurrentState = STATE_MAP;

    public ScreenComposer(OSVActivity activity, @NonNull UserDataSource userRepository, @NonNull SequenceLocalDataSource sequenceLocalDataSource) {
        this.activity = activity;
        appPrefs = getApp().getAppPrefs();
        mFragmentManager = activity.getSupportFragmentManager();
        mDecorator = new ScreenDecorator(activity, userRepository, sequenceLocalDataSource);
        mDecorator.setBackListener(this);
        progressBar = activity.findViewById(R.id.progressbar);
        //noinspection deprecation
        progressBar.getIndeterminateDrawable()
                .setColorFilter(activity.getResources().getColor(R.color.default_purple), PorterDuff.Mode.SRC_IN);
        mapHolder = activity.findViewById(R.id.content_frame_map);
        previewHolder = activity.findViewById(R.id.content_frame_camera);
        controlsHolder = activity.findViewById(R.id.content_frame_controls);
        largeHolder = activity.findViewById(R.id.content_frame_large);
        cameraPreviewBackgroundView = activity.findViewById(R.id.view_camera_preview_background);
        mRecordingFeedbackLayout = activity.findViewById(R.id.recording_feedback_layout);
        initFragments();
        setupLayoutBoundaries(isPortrait());
        if (!isPortrait()) {
            mRecordingFeedbackLayout.refreshChildren(isPortrait());
        }
    }

    public void onBackPressed() {
        mHandler.post(() -> {
            if (mDecorator.closeDrawerIfOpen()) {
                return;
            }
            cancelAction();
            int screen = getCurrentScreen();

            Log.d(TAG, "onBackPressed: " + screen);
            if (screen == SCREEN_MAP) {
                showExitDialog();
                return;
            } else if (screen == SCREEN_DRIVER_GUIDE) {
                DriverGuideFragment driverGuideFragment = (DriverGuideFragment) mFragmentManager.findFragmentByTag(DriverGuideFragment.TAG);
                if (driverGuideFragment != null) {
                    if (driverGuideFragment.onBackPressed()) {
                        return;
                    }
                }
            }
            try {
                mFragmentManager.popBackStackImmediate();
                mScreenStack.pop();
                onScreenChanged(getCurrentScreen());
            } catch (Exception e) {
                Log.w(TAG, "error: popBackStackImmediate failed");
            }
        });
    }

    public void openScreen(final int screen, final Object extra) {
        if (getCurrentScreen() == screen) {
            return;
        }
        mHandler.post(() -> {
            enableProgressBar(false);
            OSVFragment fragment = null;
            String tag = "";
            boolean animate = true;
            mDecorator.closeDrawerIfOpen();
            mDecorator.hideSnackBar();
            Log.d(TAG, "openScreen: " + screen);
            switch (screen) {
                case SCREEN_MAP:
                    removeUpperFragments();
                    while (mScreenStack.size() > 0) {
                        mScreenStack.pop();
                    }
                    break;
                case SCREEN_MY_PROFILE:
                    tag = ProfileFragment.TAG;
                    int userType = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
                    fragment = getProfileFragmentForType(userType);
                    break;
                case SCREEN_LEADERBOARD:
                    tag = LeaderboardFragment.TAG;
                    fragment = new LeaderboardFragment();
                    break;
                case SCREEN_SETTINGS:
                    activity.startActivity(ObdActivity.newIntent(activity, ObdActivity.SESSION_SETTINGS));
                    return;
                case SCREEN_PREVIEW:
                    animate = false;
                    PlaybackManager player;
                    Sequence sequence = (Sequence) extra;
                    if (sequence.getType() != Sequence.SequenceTypes.LOCAL) {
                        player = new OnlinePlaybackManager(activity, sequence);
                    } else {
                        LocalSequence localSequence = (LocalSequence) extra;
                        if (sequence.getCompressionDetails() instanceof SequenceDetailsCompressionVideo) {
                            //ToDo: provide a way to provide injection without giving it to the constructor since is required only once in one case
                            player = new VideoPlayerManager(activity, localSequence, Injection.provideVideoDataSource(activity.getApplicationContext()));
                        } else {
                            player = new SafePlaybackManager(activity, localSequence, Injection.provideFrameLocalDataSource(getApp().getApplicationContext()));
                        }
                    }
                    mapFragment.setSource(player);
                    TrackPreviewFragment trackPreviewFragment = new TrackPreviewFragment();
                    trackPreviewFragment.setSource(player);
                    tag = TrackPreviewFragment.TAG;
                    fragment = trackPreviewFragment;
                    break;
                case SCREEN_PREVIEW_FULLSCREEN:
                    tag = FullscreenFragment.TAG;
                    FullscreenFragment fullscreenFragment = new FullscreenFragment();
                    fragment = fullscreenFragment;
                    fullscreenFragment.setSource(extra);
                    break;
                case SCREEN_WAITING:
                    activity.startActivity(ObdActivity.newIntent(activity, ObdActivity.SESSION_UPLOAD));
                    return;
                case SCREEN_NEARBY:
                    tag = NearbyFragment.TAG;
                    NearbyFragment nearbyFragment = new NearbyFragment();
                    fragment = nearbyFragment;
                    nearbyFragment.setSource(extra);
                    break;
                case SCREEN_DRIVER_GUIDE:
                    while (mScreenStack.size() > 1) {
                        mScreenStack.pop();
                    }
                    tag = DriverGuideFragment.TAG;
                    fragment = DriverGuideFragment.newInstance(appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE));
                    break;
            }
            try {
                if (fragment != null) {
                    displayFragment(fragment, tag, animate);
                }
                mScreenStack.push(screen);
            } catch (IllegalStateException e) {
                Log.d(TAG, "openScreen: " + Log.getStackTraceString(e));
            }
        });
        mHandler.post(() -> {
            onScreenChanged(getCurrentScreen());
        });
    }

    public void openScreen(final int screen) {
        openScreen(screen, null);
    }

    public int getCurrentScreen() {
        return mScreenStack.peek();
    }

    public void onStart() {
        checkUpperFragment();
        onScreenChanged(getCurrentScreen());
    }

    @Subscribe
    public void onFullscreenRequest(FullscreenEvent event) {
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED)) {
            if (event.fullscreen) {
                transitionToState(mCurrentState, STATE_SPLIT_FULLSCREEN);
            } else {
                transitionToState(mCurrentState, STATE_SPLIT);
            }
        }
    }

    @Subscribe
    public void onPreviewSwitch(PreviewSwitchEvent event) {
        if (isMinimapAvailable()) {
            if (event.cameraTapped) {
                smallHolder = mapHolder;
                EventBus.post(new CameraPreviewContainerEvent(recordLargeLP.width, recordLargeLP.height, false));
            } else {
                smallHolder = previewHolder;
                EventBus.post(new CameraPreviewContainerEvent(recordSmallLP.width, recordSmallLP.height, true));
            }
            transitionToState(mCurrentState, STATE_RECORDING);
        }
    }

    public void enableProgressBar(final boolean enable) {
        mHandler.post(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(enable ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void onConfigurationChanged(final Configuration newConfig) {
        mHandler.post(() -> {
            int orientation = newConfig.orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
            setupLayoutBoundaries(portrait);
            setPaddingToHolder(largeHolder, mCurrentState);
            mRecordingFeedbackLayout.requestLayout();
            previewHolder.requestLayout();
            controlsHolder.requestLayout();
            mapHolder.requestLayout();
            largeHolder.requestLayout();
            if (mRecordingFeedbackLayout != null) {
                mRecordingFeedbackLayout.refreshChildren(portrait);
            }
        });
    }

    public void onHomePressed() {
        mDecorator.onHomePressed();
    }

    public void showSnackBar(CharSequence text, int duration, CharSequence button, Runnable onClick) {
        mDecorator.showSnackBar(text, duration, button, onClick);
    }

    public void hideSnackBar() {
        mDecorator.hideSnackBar();
    }

    public void unregister() {
        EventBus.unregister(mDecorator);
        EventBus.unregister(this);
    }

    public void register() {
        EventBus.register(this);
        EventBus.register(mDecorator);
    }

    private void checkUpperFragment() {
        if (mFragmentManager.getBackStackEntryCount() > 0 && mScreenStack.size() < 2) {
            FragmentManager.BackStackEntry entry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 1);
            switch (entry.getName()) {
                case ProfileFragment.TAG:
                    mScreenStack.add(SCREEN_MY_PROFILE);
                    break;
                case LeaderboardFragment.TAG:
                    mScreenStack.add(SCREEN_LEADERBOARD);
                    break;
                case TrackPreviewFragment.TAG:
                    mScreenStack.add(SCREEN_PREVIEW);
                    break;
                case FullscreenFragment.TAG:
                    mScreenStack.add(SCREEN_PREVIEW_FULLSCREEN);
                    break;
                case NearbyFragment.TAG:
                    mScreenStack.add(SCREEN_NEARBY);
                    break;
            }
        }
    }

    private void initFragments() {
        if (mapFragment == null) {
            mapFragment = new MapFragment();
        }
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.add(R.id.content_frame_map, mapFragment, MapFragment.TAG);
        ft.commit();
        mScreenStack.push(SCREEN_MAP);
        activity.addLocationPermissionListener((LocationPermissionsListener) mapFragment);
        activity.checkPermissionsForGPS();
        onScreenChanged(getCurrentScreen());
    }

    private OSVFragment getProfileFragmentForType(int userType) {
        Log.d(TAG, "getProfileFragmentForType: " + userType);
        switch (userType) {
            default:
            case PreferenceTypes.USER_TYPE_BYOD:
                return new ByodProfileFragment();
            case PreferenceTypes.USER_TYPE_CONTRIBUTOR:
            case PreferenceTypes.USER_TYPE_QA:
                if (appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
                    return new UserProfileFragment();
                } else {
                    return new SimpleProfileFragment();
                }
            case PreferenceTypes.USER_TYPE_DEDICATED:
            case PreferenceTypes.USER_TYPE_BAU:
                return new SimpleProfileFragment();
        }
    }

    private void setupLayoutBoundaries(boolean portrait) {
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        int screenSizeLarge = Math.max(point.x, point.y);
        int screenSizeSmall = Math.min(point.x, point.y);
        float widthRatio = ((float) Math.min(screenSizeSmall, mPreviewSizeSmall)) / ((float) Math.max(screenSizeSmall, mPreviewSizeSmall));
        float resizedPreviewLarge = widthRatio * ((float) mPreviewSizeLarge);
        float previewRatio = Math.min(resizedPreviewLarge / ((float) screenSizeLarge), 0.25f);
        mFittedPreviewLength = (int) ((1.0f - previewRatio) * screenSizeLarge);
        mFittedControlsLength = (int) ((previewRatio) * screenSizeLarge);

        setRecordingHoldersSize(portrait);
        setSplitHoldersSize(portrait);
        setSmallPreviewSize(portrait);
    }

    private void setRecordingHoldersSize(boolean portrait) {
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        if (portrait) {
            recordLargeLP.width = point.x;
            recordLargeLP.height = mFittedPreviewLength;
            recordLargeLP.setMargins(0, 0, 0, 0);

            controlsHolderLP.width = FrameLayout.LayoutParams.MATCH_PARENT;
            controlsHolderLP.height = mFittedControlsLength;
            controlsHolderLP.setMargins(0, 0, 0, 0);
            controlsHolderLP.gravity = Gravity.BOTTOM;
        } else {
            recordLargeLP.height = point.y;
            recordLargeLP.width = mFittedPreviewLength;
            recordLargeLP.setMargins(0, 0, 0, 0);

            controlsHolderLP.height = FrameLayout.LayoutParams.MATCH_PARENT;
            controlsHolderLP.width = mFittedControlsLength;
            controlsHolderLP.setMargins(0, 0, 0, 0);
            controlsHolderLP.gravity = Gravity.END;
        }

        if (smallHolder != previewHolder) {
            EventBus.post(new CameraPreviewContainerEvent(recordLargeLP.width, recordLargeLP.height, false));
        } else {
            EventBus.post(new CameraPreviewContainerEvent(recordSmallLP.width, recordSmallLP.height, true));
        }
    }

    private void setSplitHoldersSize(boolean portrait) {
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        float screenSizeLarge = Math.max(point.x, point.y);
        if (portrait) {
            splitSixLP.width = FrameLayout.LayoutParams.MATCH_PARENT;
            splitSixLP.height = (int) (screenSizeLarge * SPLIT_RATIO_LARGE + 0.5f);
            splitSixLP.gravity = Gravity.BOTTOM;
            splitSixLP.setMargins(0, 0, 0, 0);
            splitFourLP.width = FrameLayout.LayoutParams.MATCH_PARENT;
            splitFourLP.height = (int) (screenSizeLarge * SPLIT_RATIO_SMALL + 0.5f);
            splitFourLP.gravity = Gravity.TOP;
            splitFourLP.setMargins(0, 0, 0, 0);
            splitHiddenLP.gravity = Gravity.TOP;
            splitHiddenLP.height = 0;
        } else {
            splitSixLP.height = FrameLayout.LayoutParams.MATCH_PARENT;
            splitSixLP.width = (int) (screenSizeLarge * SPLIT_RATIO_LARGE + 0.5f);
            splitSixLP.gravity = Gravity.END;
            splitSixLP.setMargins(0, 0, 0, 0);
            splitFourLP.height = FrameLayout.LayoutParams.MATCH_PARENT;
            splitFourLP.width = (int) (screenSizeLarge * SPLIT_RATIO_SMALL + 0.5f);
            splitFourLP.gravity = Gravity.START;
            splitFourLP.setMargins(0, 0, 0, 0);
            splitHiddenLP.gravity = Gravity.START;
            splitHiddenLP.height = 0;
        }
    }

    private void setSmallPreviewSize(boolean portrait) {
        int margin = (int) Utils.dpToPx(activity.getApplicationContext(), 16);
        if (portrait) {
            recordSmallLP.width = (int) Utils.dpToPx(activity, 110);
            recordSmallLP.height = (int) Utils.dpToPx(activity, 150);
            recordSmallLP.setMargins(margin, 0, margin, mFittedControlsLength + margin);
            recordSmallLP.gravity = Gravity.START | Gravity.BOTTOM;
        } else {
            recordSmallLP.width = (int) Utils.dpToPx(activity, 150);
            recordSmallLP.height = (int) Utils.dpToPx(activity, 110);
            recordSmallLP.setMargins(margin, 0, margin, margin);
            recordSmallLP.gravity = Gravity.START | Gravity.BOTTOM;
        }
    }

    private void onScreenChanged(int screen) {
        Log.d(TAG, "onScreenChanged: " + Arrays.toString(mScreenStack.toArray()));
        switch (screen) {
            case SCREEN_MAP:
                transitionToState(mCurrentState, STATE_MAP);
                break;
            case SCREEN_PREVIEW:
                //            case SCREEN_PREVIEW_FULLSCREEN:
                transitionToState(mCurrentState, STATE_SPLIT);
                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED)) {
                    transitionToState(mCurrentState, STATE_SPLIT_FULLSCREEN);
                }
                break;
            default:
                transitionToState(mCurrentState, STATE_MAP);
                break;
        }

        mDecorator.onScreenChanged();
        if (Fabric.isInitialized()) {
            Crashlytics.setInt(Log.CURRENT_SCREEN, screen);
        }
    }

    private void transitionToState(int current, int next) {
        Log.d(TAG, "transitionToState: from " + current + " to " + next + " is " + (current << next));
        switch (current << next) {
            default:
            case STATE_MAP << STATE_MAP:
                mapFragment.enableRecordingButton(true);
                mapHolder.bringToFront();
                largeHolder.bringToFront();
                animateToFullscreen(mapHolder, fullScreenLP);
                break;
            case STATE_SPLIT << STATE_SPLIT:
            case STATE_MAP << STATE_SPLIT:
                mapFragment.enableMapButtons(false);
                mapHolder.bringToFront();
                largeHolder.bringToFront();
                //                copy = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams)splitFourLP);
                //                copy.gravity = splitFourLP.gravity;
                animateToSplitRatioSmall(mapHolder, splitFourLP);
                mapHolder.invalidate();
                animateToSplitRatioLarge(largeHolder, splitSixLP);
                animateToHidden(controlsHolder, splitHiddenLP);
                animateToHidden(previewHolder, splitHiddenLP);
                break;
            case STATE_SPLIT << STATE_MAP:
                mapHolder.bringToFront();
                largeHolder.bringToFront();
                animateToFullscreen(mapHolder, fullScreenLP);
                animateToFullscreen(largeHolder, fullScreenLP);
                break;
            case STATE_SPLIT_FULLSCREEN << STATE_MAP:
                mapHolder.bringToFront();
                largeHolder.bringToFront();
                animateToFullscreen(mapHolder, fullScreenLP);
                animateToFullscreen(largeHolder, fullScreenLP);
                break;
            case STATE_SPLIT << STATE_SPLIT_FULLSCREEN:
                animateToFullscreen(largeHolder, fullScreenLP);
                animateToHidden(mapHolder, splitHiddenLP);
                break;
            case STATE_SPLIT_FULLSCREEN << STATE_SPLIT:
                animateToSplitRatioSmall(mapHolder, splitFourLP);
                mapHolder.requestLayout();
                animateToSplitRatioLarge(largeHolder, splitSixLP);
                break;
        }
        mCurrentState = next;
        setPaddingToHolder(largeHolder, mCurrentState);
    }

    private void setPaddingToHolder(FrameLayout holder, int state) {
        Resources res = activity.getResources();
        switch (state) {
            case STATE_SPLIT:
                largeHolder.setPadding((int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                        (int) res.getDimension(R.dimen.track_preview_card_padding_top),
                        (int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                        (int) res.getDimension(R.dimen.track_preview_card_padding_bottom));
                break;
            case STATE_SPLIT_FULLSCREEN:
                largeHolder.setPadding((int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                        (int) (res.getDimension(R.dimen.track_preview_card_padding_top) +
                                res.getDimension(R.dimen.track_preview_card_additional_padding_top)),
                        (int) res.getDimension(R.dimen.track_preview_card_padding_sides),
                        (int) res.getDimension(R.dimen.track_preview_card_padding_bottom));
                break;
            default:
                holder.setPadding(0, 0, 0, 0);
                break;
        }
    }

    private void animateToSplitRatioLarge(FrameLayout holder, FrameLayout.LayoutParams lp) {
        //        FrameLayout.LayoutParams copy = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams)lp);
        //        copy.gravity = lp.gravity;
        holder.setLayoutParams(lp);
    }

    private void animateToSplitRatioSmall(FrameLayout holder, FrameLayout.LayoutParams lp) {
        //        FrameLayout.LayoutParams copy = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams)lp);
        //        copy.gravity = lp.gravity;
        holder.setLayoutParams(lp);
    }

    private void animateToHidden(FrameLayout holder, FrameLayout.LayoutParams lp) {
        //        FrameLayout.LayoutParams copy = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams)lp);
        //        copy.gravity = lp.gravity;
        holder.setLayoutParams(lp);
    }

    private void animateToFullscreen(FrameLayout holder, FrameLayout.LayoutParams lp) {
        //        FrameLayout.LayoutParams copy = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams)lp);
        //        copy.gravity = lp.gravity;
        holder.setLayoutParams(lp);
    }

    private boolean isMinimapAvailable() {
        return isMapEnabled() && appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, true);
    }

    private boolean isMapEnabled() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED, false);
    }

    private void displayFragment(OSVFragment fragment, String tag, boolean animate) {
        Log.d(TAG, "displayFragment: " + fragment + " tag = " + tag + ", animate = " + animate);
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (animate) {
            ft.setCustomAnimations(fragment.getEnterAnimation(), fragment.getExitAnimation(), fragment.getEnterAnimation(),
                    fragment.getExitAnimation());
        }
        if (fragment.isAdded()) {
            ft.remove(fragment);
        }
        ft.commit();
        mFragmentManager.executePendingTransactions();
        ft = mFragmentManager.beginTransaction();
        if (animate) {
            ft.setCustomAnimations(fragment.getEnterAnimation(), fragment.getExitAnimation(), fragment.getEnterAnimation(),
                    fragment.getExitAnimation());
        }
        ft.addToBackStack(tag);
        //        if (mCurrentFragment != null && mCurrentFragment.getSharedElement() != null) {
        //            ft.replace(R.id.content_frame_large, fragment, tag);
        //            ft.addSharedElement(mCurrentFragment.getSharedElement(), mCurrentFragment.getSharedElementTransitionName());
        //        } else {
        ft.add(R.id.content_frame_large, fragment, tag);
        //        }
        ft.commit();
    }

    private void removeUpperFragments() {
        if (mFragmentManager.getBackStackEntryCount() > 0) {
            int counter = 0;
            while (mFragmentManager.getBackStackEntryCount() > 0 && counter < 100) {
                try {
                    mFragmentManager.popBackStackImmediate();
                } catch (IllegalStateException e) {
                    Log.w(TAG, "removeUpperFragments: popBackStackImmediate failed");
                    counter++;
                }
            }
        }
    }

    private void cancelAction() {
        enableProgressBar(false);
        if (mapFragment != null) {
            mapFragment.cancelAction();
        }
    }

    private boolean isPortrait() {
        int orientation = activity.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private void showExitDialog() {
        if (mExitDialog == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
            mExitDialog = builder.setMessage(R.string.exit_app_message).setTitle(R.string.app_name_formatted)
                    .setPositiveButton(R.string.exit_label, (dialog, which) -> activity.finish()).setNegativeButton(R.string.cancel_label, (dialog, which) -> {
                    }).create();
        }
        mExitDialog.show();
    }

    private OSVApplication getApp() {
        return activity.getApp();
    }
}
