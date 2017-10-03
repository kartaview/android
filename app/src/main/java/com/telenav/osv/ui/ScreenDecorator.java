package com.telenav.osv.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.StringSignature;
import com.telenav.osv.R;
import com.telenav.osv.activity.LoginActivity;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.command.SendReportCommand;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.network.upload.UploadCancelledEvent;
import com.telenav.osv.event.network.upload.UploadEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.network.upload.UploadPausedEvent;
import com.telenav.osv.event.network.upload.UploadProgressEvent;
import com.telenav.osv.event.network.upload.UploadStartedEvent;
import com.telenav.osv.event.network.upload.UploadingSequenceEvent;
import com.telenav.osv.event.ui.RecordingVisibleEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.ui.custom.FixedFrameLayout;
import com.telenav.osv.ui.custom.ScoreView;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Class holding all the code related to the navigation drawer
 * Created by Kalman on 13/02/2017.
 * todo onScreenChanged takes way too much time on the UiThread
 */
//@DebugLog //todo enable this to see method run time
class ScreenDecorator {

  private static final String TAG = "ScreenDecorator";

  private final OSVActivity activity;

  private final TextView signatureActionBarText;

  private final ScoreView scoreText;

  private final Recorder mRecorder;

  private final ImageView mSendButton;

  private Snackbar mSnackBar;

  private AppBarLayout mAppBar;

  private Toolbar mToolbar;

  private ActionBar mActionBar;

  private NavigationView navigationView;

  private TextView mUsernameTextView;

  private ImageView mLogOutImage;

  private DrawerLayout mDrawer;

  private Handler mHandler = new Handler(Looper.getMainLooper());

  private Preferences appPrefs;

  private Navigator mNavigator;

  private View mRecordingHintLayoutLandscape;

  private boolean mBackButtonShown = false;

  private View.OnClickListener mMenuListener = v -> onHomePressed();

  private FixedFrameLayout mRecordingFeedbackLayout;

  private ImageView mUserPhotoImageView;

  private LoginManager mLoginManager;

  private View.OnClickListener logoutOnClickListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
      builder.setMessage(getString(R.string.logout_confirmation_message)).setTitle(getString(R.string.log_out))
          .setNegativeButton(R.string.cancel_label, (dialog, which) -> {

          }).setPositiveButton(R.string.log_out, (dialog, which) -> mLoginManager.logout()).create().show();
    }
  };

  private View.OnClickListener loginOnClickListener = new View.OnClickListener() {

    @Override
    public void onClick(View v) {
      if (Utils.isInternetAvailable(activity)) {
        closeDrawerIfOpen();
        activity.startActivity(new Intent(activity, LoginActivity.class));
      } else {
        showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
      }
    }
  };

  ScreenDecorator(OSVActivity activity, Recorder recorder, Preferences prefs) {
    this.activity = activity;
    appPrefs = prefs;
    mRecorder = recorder;
    mDrawer = activity.findViewById(R.id.activity_main_root);
    mAppBar = activity.findViewById(R.id.app_bar);
    mToolbar = activity.findViewById(R.id.app_toolbar);
    mSendButton = activity.findViewById(R.id.issue_report_send_button);
    mSendButton.setOnClickListener(v -> {
      if (getCurrentScreen() == Navigator.SCREEN_REPORT) {
        EventBus.post(new SendReportCommand());
      }
    });
    signatureActionBarText = mToolbar.findViewById(R.id.signature_action_bar_text);

    mRecordingHintLayoutLandscape = activity.findViewById(R.id.record_hint_layout_landscape);

    mRecordingFeedbackLayout = activity.findViewById(R.id.recording_feedback_layout);

    scoreText = activity.findViewById(R.id.score_text);
    navigationView = activity.findViewById(R.id.navigation_view);
    mToolbar.setNavigationOnClickListener(mMenuListener);
    activity.setSupportActionBar(mToolbar);
    initNavigationDrawer();
    mActionBar = activity.getSupportActionBar();
    appPrefs.getUserTypeLive().observe(activity, this::onUserTypeChanged);
    appPrefs.observeLogin().observe(activity, this::onLoginChanged);
    appPrefs.getUserPhotoUrlLive().observe(activity, this::setUserProfilePicture);
    appPrefs.getGamificationEnabledLive().observe(activity, value -> {
      if (scoreText != null) {
        scoreText.setVisibility(value == null || value ? View.VISIBLE : View.GONE);
      }
    });
  }

  void setLoginManager(LoginManager mLoginManager) {
    this.mLoginManager = mLoginManager;
  }

  void setNavigator(Navigator listener) {
    this.mNavigator = listener;
  }

  private void initNavigationDrawer() {
    mToolbar.setNavigationOnClickListener(mMenuListener);
    navigationView.setNavigationItemSelectedListener(menuItem -> {
      int id = menuItem.getItemId();
      switch (id) {
        case R.id.menu_settings:
          mNavigator.openScreen(Navigator.SCREEN_SETTINGS);
          mDrawer.closeDrawers();
          break;
        case R.id.menu_upload:
          if (!UploadManager.isUploading()) {
            if (LocalSequence.getLocalSequencesNumber() <= 0) {
              mDrawer.closeDrawers();
              showSnackBar(getString(R.string.no_local_recordings_message), Snackbar.LENGTH_LONG);
              return true;
            }
            mNavigator.openScreen(Navigator.SCREEN_WAITING);
          } else {
            mNavigator.openScreen(Navigator.SCREEN_UPLOAD_PROGRESS);
          }
          mDrawer.closeDrawers();
          break;
        case R.id.menu_profile:
          if (!appPrefs.isLoggedIn()) {
            showSnackBar(R.string.login_to_see_online_warning, Snackbar.LENGTH_LONG, getString(R.string.login_label), () -> {
              if (Utils.isInternetAvailable(activity)) {
                activity.startActivity(new Intent(activity, LoginActivity.class));
              } else {
                showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
              }
            });
          } else {
            mNavigator.openScreen(Navigator.SCREEN_MY_PROFILE);
          }
          mDrawer.closeDrawers();
          break;
        case R.id.menu_leaderboard:
          mNavigator.openScreen(Navigator.SCREEN_LEADERBOARD);
          mDrawer.closeDrawers();
          break;
      }
      return true;
    });

    String userName = appPrefs.getUserName();
    String displayName = appPrefs.getUserDisplayName();
    String userPhoto = appPrefs.getUserPhotoUrl();

    View mHeader = navigationView.getHeaderView(0);
    mUsernameTextView = mHeader.findViewById(R.id.username_label);
    mUserPhotoImageView = mHeader.findViewById(R.id.profile_picture);
    mLogOutImage = mHeader.findViewById(R.id.log_out_image_button);

    if (appPrefs.isDriver()) {
      MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
      item.setVisible(false);
    }
    if (!appPrefs.isLoggedIn()) {
      mUserPhotoImageView
          .setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null));
      mUsernameTextView.setText(R.string.login_label);
      mUsernameTextView.setOnClickListener(loginOnClickListener);
      mUserPhotoImageView.setOnClickListener(loginOnClickListener);
      mLogOutImage.setOnClickListener(loginOnClickListener);
      mLogOutImage.setVisibility(View.VISIBLE);
    } else {
      Log.d(TAG, "initNavigationDrawer: profile picture is: " + userPhoto);
      if (userPhoto.length() > 0) {
        Log.d(TAG, "initNavigationDrawer: loading profile picture");
        Glide.with(activity).load(userPhoto).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false).dontAnimate()
            .signature(new StringSignature("profile " + userName)).priority(Priority.NORMAL)
            .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
            .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
            .listener(MainActivity.mGlideRequestListener).into(mUserPhotoImageView);
      }
      mUsernameTextView.setText(displayName);
      mUsernameTextView.setOnClickListener(v -> mNavigator.openScreen(Navigator.SCREEN_MY_PROFILE));
      mUserPhotoImageView.setOnClickListener(v -> mNavigator.openScreen(Navigator.SCREEN_MY_PROFILE));
      mLogOutImage.setOnClickListener(logoutOnClickListener);
    }

    //        actionBarDrawerToggle = new ActionBarDrawerToggle(activity, mDrawer, mToolbar, R.string.drawer_open, R.string.drawer_close) {
    //
    //            @Override
    //            public void onDrawerClosed(View v) {
    //                super.onDrawerClosed(v);
    //            }
    //
    //            @Override
    //            public void onDrawerOpened(View v) {
    //                super.onDrawerOpened(v);
    //            }
    //        };
    //        mDrawer.addDrawerListener(actionBarDrawerToggle);
    mToolbar.setNavigationOnClickListener(mMenuListener);
  }

  private String getString(int id) {
    return activity.getString(id);
  }

  void hideSnackBar() {
    if (mSnackBar != null && mSnackBar.isShown()) {
      mSnackBar.dismiss();
    }
  }

  boolean closeDrawerIfOpen() {
    if (mDrawer.isDrawerOpen(navigationView)) {
      mDrawer.closeDrawers();
      return true;
    }
    return false;
  }

  private void refreshSignatureValue(final String signatureText) {
    if (signatureActionBarText != null) {
      mHandler.post(() -> {
        Log.d(TAG, "refreshSignatureValue: signature " + signatureText);
        signatureActionBarText.setText(signatureText);
        if (getCurrentScreen() == Navigator.SCREEN_UPLOAD_PROGRESS || getCurrentScreen() == Navigator.SCREEN_WAITING) {
          signatureActionBarText.setVisibility(View.VISIBLE);
        }
      });
    }
  }

  private int getCurrentScreen() {
    return mNavigator.getCurrentScreen();
  }

  private void showActionBar(String title, boolean showBackButton, boolean showSend, int backgroundColor, int textColor, boolean light,
                             int signatureColor) {
    showActionBar(title, showBackButton, showSend, backgroundColor, textColor, light);
    if (mActionBar == null) {
      return;
    }
    if (signatureColor != -1) {
      signatureActionBarText.setTextColor(ResourcesCompat.getColor(activity.getResources(), signatureColor, null));
      signatureActionBarText.setVisibility(View.VISIBLE);
    } else {
      signatureActionBarText.setVisibility(View.GONE);
    }
  }

  private void animateAppBar(final AppBarLayout appBar, final boolean show) {
    appBar.setVisibility(show ? View.VISIBLE : View.GONE);
  }

  @SuppressWarnings("deprecation")
  private void showActionBar(String title, boolean showBackButton, boolean showSend, int backgroundColor, int textColor, boolean light) {
    //        mAppBar.setVisibility(View.VISIBLE);
    // if background color for the action bar is white set darker_grey color for the back icon
    animateAppBar(mAppBar, true);

    if (mActionBar == null) {
      return;
    }
    if (showSend) {
      mSendButton.setVisibility(View.VISIBLE);
    } else {
      mSendButton.setVisibility(View.GONE);
    }
    if (showBackButton) {
      lockDrawer(true);
      Drawable upArrow;
      if (light) {
        upArrow = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_back_black, null);
      } else {
        upArrow = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_back_white, null);
      }
      mActionBar.setDisplayHomeAsUpEnabled(true);
      mActionBar.setHomeAsUpIndicator(upArrow);
      mBackButtonShown = true;
    } else {
      lockDrawer(getCurrentScreen() == Navigator.SCREEN_RECORDING);
      Drawable menuButton = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_menu, null);
      if (menuButton != null) {
        if (light) {
          menuButton.setColorFilter(ResourcesCompat.getColor(activity.getResources(), R.color.darker_grey, null), PorterDuff.Mode.SRC_ATOP);
        } else {
          menuButton.setColorFilter(ResourcesCompat.getColor(activity.getResources(), R.color.white, null), PorterDuff.Mode.SRC_ATOP);
        }
      }
      mActionBar.setDisplayHomeAsUpEnabled(true);
      mActionBar.setHomeAsUpIndicator(menuButton);
      mBackButtonShown = false;
    }

    if (title != null) {
      mActionBar.setTitle(title);
      mToolbar.setTitleTextColor(ResourcesCompat.getColor(activity.getResources(), textColor, null));
    }
    mActionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(activity.getResources(), backgroundColor, null));
    mToolbar.setClickable(backgroundColor != R.color.transparent);
  }

  private void setStatusBarColor(int statusBarColor) {
    if (statusBarColor != -1) {
      Window window = activity.getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      int clr = activity.getResources().getColor(statusBarColor);
      window.setStatusBarColor(clr);
    } else {
      Window window = activity.getWindow();
      window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }
  }

  private void hideActionBar() {
    animateAppBar(mAppBar, false);
  }

  void onScreenChanged() {
    String title = "";
    boolean showBack = false;
    boolean showSend = false;
    int backgroundColor = R.color.darker_grey, textColor = R.color.white;
    int statusBarColor = -1;
    boolean isLight = false;
    int colorSignature = -1;
    boolean stopLocationUpdates = false;
    boolean startLocationUpdates = false;
    lockDrawer(true);
    boolean showActionBar = true;
    switch (getCurrentScreen()) {
      case Navigator.SCREEN_MAP:
        title = "";
        backgroundColor = R.color.white;
        isLight = true;
        startLocationUpdates = true;
        lockDrawer(false);
        break;
      case Navigator.SCREEN_SETTINGS:
        stopLocationUpdates = true;
        showActionBar = false;
        break;
      case Navigator.SCREEN_MY_PROFILE:
        statusBarColor = R.color.status_bar_blue_darker;
        stopLocationUpdates = true;
        showActionBar = false;
        break;
      case Navigator.SCREEN_LEADERBOARD:
        isLight = false;
        showBack = true;
        title = getString(R.string.leaderboard);
        backgroundColor = R.color.leaderboard_green;
        statusBarColor = R.color.leaderboard_green_darker;
        textColor = R.color.white;
        stopLocationUpdates = true;
        break;
      case Navigator.SCREEN_RECORDING:
        hideActionBar();
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        setRecordingHintVisibility(portrait);
        EventBus.post(new RecordingVisibleEvent());
        mHandler.postDelayed(() -> EventBus.postSticky(new GpsCommand(true)), 1000);
        if (mRecordingFeedbackLayout != null) {
          mRecordingFeedbackLayout.setVisibility(View.VISIBLE);
          boolean gamification = appPrefs.isGamificationEnabled();
          scoreText.setVisibility(gamification ? View.VISIBLE : View.GONE);
        }
        return;
      case Navigator.SCREEN_UPLOAD_PROGRESS:
        backgroundColor = R.color.dark_grey_action_bar;
        showBack = true;
        isLight = false;
        textColor = R.color.gray_text_color;
        colorSignature = R.color.white;
        title = getString(R.string.uploading_track_label);
        stopLocationUpdates = true;
        break;
      case Navigator.SCREEN_WAITING:
        isLight = true;
        showBack = true;
        backgroundColor = R.color.white;
        textColor = R.color.dark_grey_action_bar;
        title = getString(R.string.upload_label);
        colorSignature = R.color.signature_waiting_upload;
        refreshSignatureValue(Utils.formatSize(LocalSequence.getTotalSize()));
        stopLocationUpdates = true;
        break;
      case Navigator.SCREEN_RECORDING_HINTS:
        hideActionBar();
        if (mRecordingFeedbackLayout != null) {
          mRecordingFeedbackLayout.setVisibility(View.GONE);
        }
        orientation = activity.getResources().getConfiguration().orientation;
        portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        setRecordingHintVisibility(portrait);
        return;
      case Navigator.SCREEN_PREVIEW:
        isLight = true;
        showBack = true;
        title = "";
        backgroundColor = R.color.transparent;
        textColor = R.color.white;
        break;
      case Navigator.SCREEN_PREVIEW_FULLSCREEN:
        isLight = false;
        showBack = true;
        title = "";
        backgroundColor = R.color.transparent;
        textColor = R.color.white;
        break;
      case Navigator.SCREEN_NEARBY:
        backgroundColor = R.color.status_bar_blue_darker;
        Window window = activity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        int clr = activity.getResources().getColor(backgroundColor);
        window.setStatusBarColor(clr);
        stopLocationUpdates = true;
        showActionBar = false;
        break;
      case Navigator.SCREEN_SUMMARY:
        isLight = false;
        showBack = true;
        title = getString(R.string.summary_label);
        backgroundColor = R.color.gray_summary_background;
        textColor = R.color.gray_summary_secondary_text;
        break;
      case Navigator.SCREEN_REPORT:
        isLight = false;
        showBack = true;
        showSend = true;
        title = getString(R.string.issue_report_label);
        backgroundColor = R.color.issue_report_blue;
        textColor = R.color.white;
        break;
    }

    if (mRecordingFeedbackLayout != null) {
      mRecordingFeedbackLayout.setVisibility(View.GONE);
    }
    int orientation = activity.getResources().getConfiguration().orientation;
    boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
    setRecordingHintVisibility(portrait);
    if (showActionBar) {
      showActionBar(title, showBack, showSend, backgroundColor, textColor, isLight, colorSignature);
    } else {
      hideActionBar();
    }
    setStatusBarColor(statusBarColor);
    if (stopLocationUpdates) {
      if (!mRecorder.isRecording()) {
        mHandler.postDelayed(() -> EventBus.postSticky(new GpsCommand(false)), 1000);
      }
    }
    if (startLocationUpdates) {
      mHandler.postDelayed(() -> EventBus.postSticky(new GpsCommand(true)), 1000);
    }
  }

  public void showSnackBar(final int resId, final int duration) {
    showSnackBar(resId, duration, null, null);
  }

  private void showSnackBar(final CharSequence text, final int duration) {
    showSnackBar(text, duration, null, null);
  }

  private void showSnackBar(final int resId, final int duration, final String button, final Runnable onClick) {
    showSnackBar(activity.getText(resId), duration, button, onClick);
  }

  public void showSnackBar(final int resId, final int duration, final int buttonResId, final Runnable onClick) {
    showSnackBar(activity.getText(resId), duration, activity.getText(buttonResId), onClick);
  }

  public void showSnackBar(final CharSequence text, final int duration, final CharSequence button, final Runnable onClick) {
    mHandler.post(new Runnable() {

      boolean shouldGoUp;

      @Override
      public void run() {
        if (mSnackBar != null && mSnackBar.isShown()) {
          mSnackBar.setText(text);
          mSnackBar.setDuration(duration);
          if (button != null && onClick != null) {
            mSnackBar.setAction(button, v -> onClick.run());
          }
          return;
        }
        mSnackBar = Snackbar.make(mDrawer, text, duration);
        if (button != null && onClick != null) {
          mSnackBar.setAction(button, v -> onClick.run());
        }
        shouldGoUp = true;
        mSnackBar.show();
      }
    });
  }

  private void setRecordingHintVisibility(boolean portrait) {
    if (portrait || getCurrentScreen() != Navigator.SCREEN_RECORDING || mRecorder.isRecording()) {
      mRecordingHintLayoutLandscape.setVisibility(View.INVISIBLE);
    } else {
      mRecordingHintLayoutLandscape.setVisibility(View.VISIBLE);
    }
  }

  void setRecordingHintPosition(int x) {
    final int xold = x - mRecordingHintLayoutLandscape.getWidth();
    Log.d(TAG, "setRecordingHintPosition: x = " + xold);
    mRecordingHintLayoutLandscape.post(() -> {
      mRecordingHintLayoutLandscape.setTranslationX(xold);//X(xold);
      mRecordingHintLayoutLandscape.requestLayout();
    });
  }

  private boolean isBackButtonEnabled() {
    return mBackButtonShown;
  }

  private void lockDrawer(boolean lock) {
    if (lock) {
      mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navigationView);
    } else {
      mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, navigationView);
    }
  }

  private void toggleMenu() {
    if (mDrawer.isDrawerOpen(navigationView) || mDrawer.isDrawerVisible(navigationView)) {
      mDrawer.closeDrawer(navigationView);
    } else {
      mDrawer.openDrawer(navigationView);
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onRecordingStateChanged(RecordingEvent event) {
    int orientation = activity.getResources().getConfiguration().orientation;
    boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
    setRecordingHintVisibility(portrait);
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onUploadCancelled(UploadCancelledEvent event) {
    showSnackBar(R.string.upload_cancelled, Snackbar.LENGTH_SHORT);
    mHandler.postDelayed(() -> {
      EventBus.clear(UploadEvent.class);
      EventBus.clear(UploadProgressEvent.class);
    }, 1500);
  }

  @Subscribe
  public void onUploadingSequence(UploadingSequenceEvent event) {
    int size = event.numberOfSequences;
    Log.d(TAG, "onIndexingSequence: remaining recordings " + event.remainingSequences);
    refreshSignatureValue(Math.max(0, size - event.remainingSequences) + "/" + size);
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onUploadFinished(UploadFinishedEvent event) {
    showSnackBar(getString(R.string.upload_finished), Snackbar.LENGTH_LONG);
    mHandler.postDelayed(() -> {
      EventBus.clear(UploadEvent.class);
      EventBus.clear(UploadProgressEvent.class);
    }, 1500);
  }

  @Subscribe
  public void onUploadStarted(UploadStartedEvent event) {
    int size = event.numberOfSequences;
    refreshSignatureValue(Math.max(0, size - event.remainingSequences) + "/" + size);
  }

  @Subscribe
  public void onUploadPaused(UploadPausedEvent event) {
    if (event.paused) {
      showSnackBar(R.string.upload_paused, Snackbar.LENGTH_SHORT);
    } else {
      showSnackBar(R.string.resuming_upload, Snackbar.LENGTH_SHORT);
    }
  }

  private void onUserTypeChanged(int type) {
    if (UserData.isDriver(type)) {
      MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
      item.setVisible(false);
    } else {
      MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
      item.setVisible(true);
    }
  }

  private void onLoginChanged(boolean logged) {
    if (logged) {
      String displayName = appPrefs.getUserDisplayName();
      showSnackBar("Logged in as " + displayName, Snackbar.LENGTH_SHORT);
      if (mLogOutImage != null) {
        mLogOutImage.setOnClickListener(logoutOnClickListener);
      }
      if (mUserPhotoImageView != null) {
        mUserPhotoImageView.setOnClickListener(v -> mNavigator.openScreen(Navigator.SCREEN_MY_PROFILE));
      }
      if (mUsernameTextView != null) {
        mUsernameTextView.setText(displayName);
        mUsernameTextView.setOnClickListener(v -> mNavigator.openScreen(Navigator.SCREEN_MY_PROFILE));
      }
    } else {
      Log.d(TAG, "onLoginChanged: removing profile picture");
      if (mUserPhotoImageView != null) {
        mUserPhotoImageView.setOnClickListener(loginOnClickListener);
      }
      if (mUsernameTextView != null) {
        mUsernameTextView.setText(R.string.login_label);
        mUsernameTextView.setOnClickListener(loginOnClickListener);
      }
      if (mLogOutImage != null) {
        mLogOutImage.setOnClickListener(loginOnClickListener);
      }
      closeDrawerIfOpen();
    }
  }

  private void setUserProfilePicture(String url) {
    if (url != null && !url.equals("")) {
      if (mUserPhotoImageView != null) {
        String pictureUrl = appPrefs.getUserPhotoUrl();
        String username = appPrefs.getUserName();
        if (pictureUrl != null && pictureUrl.length() > 0) {
          Log.d(TAG, "onLoginChanged: loading profile picture");
          Glide.with(activity).load(pictureUrl).centerCrop().dontAnimate()
              .diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
              .signature(new StringSignature("profile " + username + "-" + pictureUrl))
              .priority(Priority.IMMEDIATE)
              .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
              .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
              .listener(MainActivity.mGlideRequestListener).into(mUserPhotoImageView);
        }
      }
    } else {
      if (mUserPhotoImageView != null) {
        mUserPhotoImageView
            .setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null));
      }
    }
  }

  @Subscribe(priority = 1)
  public void onRefreshNeeded(SequencesChangedEvent event) {
    if (!event.online && (!UploadManager.isUploading())) {
      refreshSignatureValue(Utils.formatSize(LocalSequence.getTotalSize()));
    }
  }

  void onHomePressed() {
    if (isBackButtonEnabled()) {
      mNavigator.onBackPressed();
    } else {
      toggleMenu();
    }
  }

  public void onConfigurationChanged(Configuration newConfig) {
    setRecordingHintVisibility(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
  }
}
