package com.telenav.osv.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraInfoEvent;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.event.ui.FullscreenEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.ui.custom.FixedFrameLayout;
import com.telenav.osv.ui.fragment.ByodProfileFragment;
import com.telenav.osv.ui.fragment.CameraControlsFragment;
import com.telenav.osv.ui.fragment.CameraPreviewFragment;
import com.telenav.osv.ui.fragment.FullscreenFragment;
import com.telenav.osv.ui.fragment.HintsFragment;
import com.telenav.osv.ui.fragment.IssueReportFragment;
import com.telenav.osv.ui.fragment.LeaderboardFragment;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.NearbyFragment;
import com.telenav.osv.ui.fragment.OSVFragment;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.ui.fragment.RecordingSummaryFragment;
import com.telenav.osv.ui.fragment.SettingsFragment;
import com.telenav.osv.ui.fragment.SimpleProfileFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.ui.fragment.UploadProgressFragment;
import com.telenav.osv.ui.fragment.UserProfileFragment;
import com.telenav.osv.ui.fragment.WaitingFragment;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;
import java.util.Arrays;
import java.util.Stack;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.telenav.osv.item.network.UserData.TYPE_BAU;
import static com.telenav.osv.item.network.UserData.TYPE_BYOD;
import static com.telenav.osv.item.network.UserData.TYPE_CONTRIBUTOR;
import static com.telenav.osv.item.network.UserData.TYPE_DEDICATED;
import static com.telenav.osv.item.network.UserData.TYPE_QA;

/**
 * The class responsible for displaying the different screens
 * Created by Kalman on 17/01/2017.
 * todo onBackPressed takes too much time on UiThread
 */
//@DebugLog //todo enable this to see method run time
public class ScreenComposer implements Navigator {

  private static final String TAG = "ScreenComposer";

  //  ==========================STATES==============================

  private static final int STATE_MAP = 1;

  private static final int STATE_RECORDING = 3;

  private static final int STATE_SPLIT = 5;

  private static final int STATE_SPLIT_FULLSCREEN = 7;

  private static final float SPLIT_RATIO_LARGE = 0.64f;

  private static final float SPLIT_RATIO_SMALL = 0.36f;

  private final Recorder mRecorder;

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

  private Preferences appPrefs;

  private ProgressBar progressBar;

  private FrameLayout smallHolder;

  private int mCurrentState = STATE_MAP;

  public ScreenComposer(OSVActivity activity, Recorder recorder, Preferences prefs) {
    this.activity = activity;
    appPrefs = prefs;
    mRecorder = recorder;
    mFragmentManager = activity.getSupportFragmentManager();
    mDecorator = new ScreenDecorator(activity, recorder, prefs);
    mDecorator.setNavigator(this);
    progressBar = activity.findViewById(R.id.progressbar);
    //noinspection deprecation
    progressBar.getIndeterminateDrawable()
        .setColorFilter(activity.getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
    mapHolder = activity.findViewById(R.id.content_frame_map);
    previewHolder = activity.findViewById(R.id.content_frame_camera);
    controlsHolder = activity.findViewById(R.id.content_frame_controls);
    largeHolder = activity.findViewById(R.id.content_frame_large);
    mRecordingFeedbackLayout = activity.findViewById(R.id.recording_feedback_layout);
    initFragments();

    if (!isPortrait()) {
      mRecordingFeedbackLayout.refreshChildren(isPortrait());
    }
  }

  public void setLoginManager(LoginManager loginManager) {
    mDecorator.setLoginManager(loginManager);
  }

  public void onStart() {
    checkUpperFragment();
    onScreenChanged(getCurrentScreen());
  }

  private void checkUpperFragment() {
    if (mFragmentManager.getBackStackEntryCount() > 0 && mScreenStack.size() < 2) {
      FragmentManager.BackStackEntry entry = mFragmentManager.getBackStackEntryAt(mFragmentManager.getBackStackEntryCount() - 1);
      switch (entry.getName()) {
        case UploadProgressFragment.TAG:
          mScreenStack.add(SCREEN_UPLOAD_PROGRESS);
          break;
        case WaitingFragment.TAG:
          mScreenStack.add(SCREEN_WAITING);
          break;
        case ProfileFragment.TAG:
          mScreenStack.add(SCREEN_MY_PROFILE);
          break;
        case LeaderboardFragment.TAG:
          mScreenStack.add(SCREEN_LEADERBOARD);
          break;
        case SettingsFragment.TAG:
          mScreenStack.add(SCREEN_SETTINGS);
          break;
        case HintsFragment.TAG:
          mScreenStack.add(SCREEN_RECORDING_HINTS);
          break;
        case TrackPreviewFragment.TAG:
          mScreenStack.add(SCREEN_PREVIEW);
          break;
        case FullscreenFragment.TAG:
          mScreenStack.add(SCREEN_PREVIEW_FULLSCREEN);
          break;
        case RecordingSummaryFragment.TAG:
          mScreenStack.add(SCREEN_SUMMARY);
          break;
        case IssueReportFragment.TAG:
          mScreenStack.add(SCREEN_REPORT);
          break;
        case NearbyFragment.TAG:
          mScreenStack.add(SCREEN_NEARBY);
          break;
        default:
          break;
      }
    }
  }

  private void initFragments() {
    if (mapFragment == null) {
      mapFragment = new MapFragment();
    }
    CameraPreviewFragment cameraPreviewFragment = new CameraPreviewFragment();
    CameraControlsFragment cameraControlsFragment = new CameraControlsFragment();
    FragmentTransaction ft = mFragmentManager.beginTransaction();
    ft.add(R.id.content_frame_map, mapFragment, MapFragment.TAG);
    ft.add(R.id.content_frame_camera, cameraPreviewFragment, CameraPreviewFragment.TAG);
    ft.add(R.id.content_frame_controls, cameraControlsFragment, CameraControlsFragment.TAG);
    ft.commit();
    mScreenStack.push(SCREEN_MAP);

    onScreenChanged(getCurrentScreen());
  }

  private OSVFragment getProfileFragmentForType(int userType) {
    Log.d(TAG, "getProfileFragmentForType: " + userType);
    switch (userType) {
      case TYPE_BYOD:
        return new ByodProfileFragment();
      //return new ProfileByodFragment();//todo only used in new profile fragment impl.
      case TYPE_CONTRIBUTOR:
      case TYPE_QA:
        if (appPrefs.isGamificationEnabled()) {
          return new UserProfileFragment();
        }
        //no break, we default to simple profile fragment
      case TYPE_DEDICATED:
      case TYPE_BAU:
      default:
        return new SimpleProfileFragment();
    }
  }

  private void setCameraPreviewRatio(int previewWidth, int previewHeight) {
    mPreviewSizeLarge = Math.max(previewHeight, previewWidth);
    mPreviewSizeSmall = Math.min(previewHeight, previewWidth);
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
    if (!portrait) {
      mDecorator.setRecordingHintPosition(mFittedPreviewLength + mFittedControlsLength / 3);
    }
  }

  private void setRecordingHoldersSize(boolean portrait) {
    if (portrait) {
      recordLargeLP.width = FrameLayout.LayoutParams.MATCH_PARENT;
      recordLargeLP.height = mFittedPreviewLength;
      recordLargeLP.setMargins(0, 0, 0, 0);

      controlsHolderLP.width = FrameLayout.LayoutParams.MATCH_PARENT;
      controlsHolderLP.height = mFittedControlsLength;
      controlsHolderLP.setMargins(0, 0, 0, 0);
      controlsHolderLP.gravity = Gravity.BOTTOM;
    } else {
      recordLargeLP.height = FrameLayout.LayoutParams.MATCH_PARENT;
      recordLargeLP.width = mFittedPreviewLength;
      recordLargeLP.setMargins(0, 0, 0, 0);

      controlsHolderLP.height = FrameLayout.LayoutParams.MATCH_PARENT;
      controlsHolderLP.width = mFittedControlsLength;
      controlsHolderLP.setMargins(0, 0, 0, 0);
      controlsHolderLP.gravity = Gravity.RIGHT;
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
      splitSixLP.gravity = Gravity.RIGHT;
      splitSixLP.setMargins(0, 0, 0, 0);
      splitFourLP.height = FrameLayout.LayoutParams.MATCH_PARENT;
      splitFourLP.width = (int) (screenSizeLarge * SPLIT_RATIO_SMALL + 0.5f);
      splitFourLP.gravity = Gravity.LEFT;
      splitFourLP.setMargins(0, 0, 0, 0);
      splitHiddenLP.gravity = Gravity.LEFT;
      splitHiddenLP.height = 0;
    }
  }

  private void setSmallPreviewSize(boolean portrait) {
    recordSmallLP.bottomMargin = (int) Utils.dpToPx(activity, 5);
    recordSmallLP.rightMargin = (int) Utils.dpToPx(activity, 5);
    if (portrait) {
      recordSmallLP.width = (int) Utils.dpToPx(activity, 110);
      recordSmallLP.height = (int) Utils.dpToPx(activity, 150);
      recordSmallLP.topMargin = (int) (mFittedPreviewLength - recordSmallLP.height - Utils.dpToPx(activity, 5));
      recordSmallLP.leftMargin = (int) Utils.dpToPx(activity, 5);
    } else {
      recordSmallLP.width = (int) Utils.dpToPx(activity, 150);
      recordSmallLP.height = (int) Utils.dpToPx(activity, 110);
      recordSmallLP.topMargin = (int) Utils.dpToPx(activity, 5);
      recordSmallLP.leftMargin = (int) (mFittedPreviewLength - recordSmallLP.width - Utils.dpToPx(activity, 5));
    }
  }

  private void onScreenChanged(int screen) {
    Log.d(TAG, "onScreenChanged: " + Arrays.toString(mScreenStack.toArray()));
    switch (screen) {
      case SCREEN_MAP:
        transitionToState(mCurrentState, STATE_MAP);
        break;
      case SCREEN_SUMMARY:
      case SCREEN_RECORDING_HINTS:
      case SCREEN_RECORDING:
        transitionToState(mCurrentState, STATE_RECORDING);
        break;
      case SCREEN_PREVIEW:
        //            case SCREEN_PREVIEW_FULLSCREEN:
        transitionToState(mCurrentState, STATE_SPLIT);
        if (!appPrefs.isMapEnabled()) {
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
        mapHolder.bringToFront();
        largeHolder.bringToFront();
        animateToFullscreen(mapHolder, fullScreenLP);
        break;
      case STATE_MAP << STATE_RECORDING:
        previewHolder.setLayoutParams(recordLargeLP);
        previewHolder.requestLayout();
        mRecordingFeedbackLayout.setLayoutParams(recordLargeLP);
        previewHolder.requestLayout();
        controlsHolder.setLayoutParams(controlsHolderLP);
        controlsHolder.requestLayout();
        if (isMinimapAvailable()) {
          smallHolder = mapHolder;
          animateToSmall(mapHolder, recordSmallLP);
        } else {
          previewHolder.bringToFront();
          controlsHolder.bringToFront();
          largeHolder.bringToFront();
        }
        break;
      case STATE_SPLIT << STATE_SPLIT:
      case STATE_MAP << STATE_SPLIT:
        mapHolder.bringToFront();
        largeHolder.bringToFront();
        animateToSplitRatioSmall(mapHolder, splitFourLP);
        mapHolder.requestLayout();
        animateToSplitRatioLarge(largeHolder, splitSixLP);
        animateToHidden(controlsHolder, splitHiddenLP);
        animateToHidden(previewHolder, splitHiddenLP);
        break;
      case STATE_RECORDING << STATE_RECORDING:
        if (isMinimapAvailable()) {
          smallHolder.bringToFront();
          largeHolder.bringToFront();
          animateToSmall(smallHolder, recordSmallLP);
          animateToLarge(smallHolder == mapHolder ? previewHolder : mapHolder, recordLargeLP);
        }
        break;
      case STATE_RECORDING << STATE_MAP:
        mapHolder.bringToFront();
        largeHolder.bringToFront();
        animateToFullscreen(mapHolder, fullScreenLP);
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

  private void animateToLarge(FrameLayout holder, FrameLayout.LayoutParams lp) {
    //animate to lp, right now its disabled so we set it directly
    holder.setLayoutParams(lp);
  }

  private void animateToSplitRatioLarge(FrameLayout holder, FrameLayout.LayoutParams lp) {
    //animate to lp, right now its disabled so we set it directly
    holder.setLayoutParams(lp);
  }

  private void animateToSplitRatioSmall(FrameLayout holder, FrameLayout.LayoutParams lp) {
    //animate to lp, right now its disabled so we set it directly
    holder.setLayoutParams(lp);
  }

  private void animateToHidden(FrameLayout holder, FrameLayout.LayoutParams lp) {
    //animate to lp, right now its disabled so we set it directly
    holder.setLayoutParams(lp);
  }

  private void animateToSmall(FrameLayout holder, FrameLayout.LayoutParams lp) {
    //animate to lp, right now its disabled so we set it directly
    holder.setLayoutParams(lp);
  }

  private void animateToFullscreen(FrameLayout holder, FrameLayout.LayoutParams lp) {
    //animate to lp, right now its disabled so we set it directly
    holder.setLayoutParams(lp);
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onCameraReady(final CameraInitEvent event) {
    if (event.type == CameraInitEvent.TYPE_FAILED) {
      Log.e(TAG, "Could not open camera HAL");
      mDecorator.showSnackBar(R.string.cannot_connect_hal, Snackbar.LENGTH_LONG);
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onCameraInfoReceived(final CameraInfoEvent event) {
    setCameraPreviewRatio(event.previewWidth, event.previewHeight);
    setupLayoutBoundaries(isPortrait());
  }

  @Subscribe
  public void onFullscreenRequest(FullscreenEvent event) {
    if (appPrefs.isMapEnabled()) {
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
      } else {
        smallHolder = previewHolder;
      }
      transitionToState(mCurrentState, STATE_RECORDING);
    }
  }

  private boolean isMinimapAvailable() {
    return appPrefs.isMapEnabled() && appPrefs.isMiniMapEnabled();
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
    mDecorator.onConfigurationChanged(newConfig);
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

  public void onHomePressed() {
    mDecorator.onHomePressed();
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
        default:
        case SCREEN_MAP:
          removeUpperFragments();
          while (!mScreenStack.isEmpty()) {
            mScreenStack.pop();
          }
          break;
        case SCREEN_RECORDING:
          int cameraPermitted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
          if (
              cameraPermitted == PackageManager.PERMISSION_DENIED) {
            showSnackBar(activity.getString(R.string.camera_permission_warning), Snackbar.LENGTH_LONG, null, null);
            return;
          }
          removeUpperFragments();
          while (mScreenStack.size() > 1) {
            mScreenStack.pop();
          }
          break;
        case SCREEN_MY_PROFILE:
          tag = ProfileFragment.TAG;
          int userType = appPrefs.getUserType();
          fragment = getProfileFragmentForType(userType);
          break;
        case SCREEN_LEADERBOARD:
          tag = LeaderboardFragment.TAG;
          fragment = new LeaderboardFragment();
          break;
        case SCREEN_SETTINGS:
          tag = SettingsFragment.TAG;
          fragment = new SettingsFragment();
          break;
        case SCREEN_PREVIEW:
          animate = false;
          mapFragment.setDisplayData((Sequence) extra);
          TrackPreviewFragment trackPreviewFragment = new TrackPreviewFragment();
          trackPreviewFragment.setDisplayData((Sequence) extra);
          trackPreviewFragment.setListener(mapFragment);
          tag = TrackPreviewFragment.TAG;
          fragment = trackPreviewFragment;
          break;
        case SCREEN_PREVIEW_FULLSCREEN:
          tag = FullscreenFragment.TAG;
          FullscreenFragment fullscreenFragment = new FullscreenFragment();
          fragment = fullscreenFragment;
          fullscreenFragment.setDisplayData((Sequence) extra);
          break;
        case SCREEN_WAITING:
          tag = WaitingFragment.TAG;
          fragment = new WaitingFragment();
          break;
        case SCREEN_UPLOAD_PROGRESS:
          removeUpperFragments();
          while (mScreenStack.size() > 1) {
            mScreenStack.pop();
          }
          tag = UploadProgressFragment.TAG;
          fragment = new UploadProgressFragment();
          break;
        case SCREEN_SUMMARY:
          tag = RecordingSummaryFragment.TAG;
          RecordingSummaryFragment recordingSummaryFragment =
              new RecordingSummaryFragment();
          recordingSummaryFragment.setDisplayData((LocalSequence) extra);
          fragment = recordingSummaryFragment;
          break;
        case SCREEN_REPORT:
          tag = IssueReportFragment.TAG;
          fragment = new IssueReportFragment();
          break;
        case SCREEN_RECORDING_HINTS:
          tag = HintsFragment.TAG;
          fragment = new HintsFragment();
          break;
        case SCREEN_NEARBY:
          tag = NearbyFragment.TAG;
          NearbyFragment nearbyFragment = new NearbyFragment();
          fragment =
              nearbyFragment;
          nearbyFragment.setDisplayData((TrackCollection) extra);
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
    mHandler.post(() -> onScreenChanged(getCurrentScreen()));
  }

  public void openScreen(final int screen) {
    openScreen(screen, null);
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
      } else if (screen == SCREEN_RECORDING) {
        if (mRecorder.isRecording()) {
          showRecordingDialog();
        } else {
          openScreen(SCREEN_MAP);
        }
        return;
      }
      try {
        mFragmentManager.popBackStackImmediate();
        mScreenStack.pop();
        onScreenChanged(getCurrentScreen());
      } catch (Exception e) {
        Log.w(TAG, "error: popBackStackImmediate failed " + Log.getStackTraceString(e));
      }
    });
  }

  public int getCurrentScreen() {
    return mScreenStack.peek();
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
          .setPositiveButton(R.string.exit_label, (dialog, which) -> activity.finish())
          .setNegativeButton(R.string.cancel_label, (dialog, which) -> {
          }).create();
    }
    mExitDialog.show();
  }

  private void showRecordingDialog() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
    builder.setMessage(R.string.stop_recording_warning_message).setTitle(R.string.stop_recording_warning_title)
        .setNegativeButton(R.string.stop_value_string, (dialog, which) -> {
          mRecorder.stopRecording();
          openScreen(SCREEN_MAP);
        }).setPositiveButton(R.string.keep_value_string, (dialog, which) -> {
      Toast.makeText(activity, R.string.recording_in_background_toast_message, Toast.LENGTH_SHORT).show();
      Intent homeIntent = new Intent(Intent.ACTION_MAIN);
      homeIntent.addCategory(Intent.CATEGORY_HOME);
      homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      activity.startActivity(homeIntent);
    }).create().show();
  }

  private OSVApplication getApp() {
    return activity.getApp();
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
}
