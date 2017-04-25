package com.telenav.osv.ui;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.animation.Animator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.command.LogoutCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.network.upload.UploadCancelledEvent;
import com.telenav.osv.event.network.upload.UploadEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.network.upload.UploadPausedEvent;
import com.telenav.osv.event.network.upload.UploadProgressEvent;
import com.telenav.osv.event.network.upload.UploadStartedEvent;
import com.telenav.osv.event.network.upload.UploadingSequenceEvent;
import com.telenav.osv.event.ui.GamificationSettingEvent;
import com.telenav.osv.event.ui.RecordingVisibleEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.ui.custom.FixedFrameLayout;
import com.telenav.osv.ui.custom.ScoreView;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Class holding all the code related to the navigation drawer
 * Created by Kalman on 13/02/2017.
 */
class ScreenDecorator {
    
    public final static String TAG = "ScreenDecorator";

    private final OSVActivity activity;

    private final TextView signatureActionBarText;

    private final UploadManager mUploadManager;

    private final ScoreView scoreText;

    private final Recorder mRecorder;

    private Snackbar mSnackBar;

    private AppBarLayout mAppBar;

    private Toolbar mToolbar;

    private ActionBar mActionBar;

    private NavigationView navigationView;

    private TextView mUsernameTextView;

    private ImageView mLogOutImage;

    private DrawerLayout mDrawer;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private ApplicationPreferences appPrefs;

    private OnNavigationListener mNavigationListener;

    private View mRecordingHintLayoutLandscape;

    private boolean mBackButtonShown = false;

    private View.OnClickListener mMenuListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onHomePressed();
        }
    };

    private FixedFrameLayout mRecordingFeedbackLayout;

    private View mHeader;

    private ImageView mUserPhotoImageView;

    private View.OnClickListener logoutOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
            builder.setMessage(getString(R.string.logout_confirmation_message)).setTitle(getString(R.string.log_out)).setNegativeButton(R.string.cancel_label,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setPositiveButton(R.string.log_out, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EventBus.post(new LogoutCommand());
                }
            }).create().show();
        }
    };

    private View.OnClickListener loginOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Utils.isInternetAvailable(activity)) {
                activity.startActivity(new Intent(activity, LoginActivity.class));
            } else {
                showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
            }
        }
    };

    ScreenDecorator(OSVActivity activity) {
        this.activity = activity;
        appPrefs = activity.getApp().getAppPrefs();
        mUploadManager = activity.getApp().getUploadManager();
        mRecorder = activity.getApp().getRecorder();
        mDrawer = (DrawerLayout) activity.findViewById(R.id.activity_main_root);
        mAppBar = (AppBarLayout) activity.findViewById(R.id.app_bar);
        mToolbar = (Toolbar) activity.findViewById(R.id.app_toolbar);
        signatureActionBarText = (TextView) mToolbar.findViewById(R.id.signature_action_bar_text);

        mRecordingHintLayoutLandscape = activity.findViewById(R.id.record_hint_layout_landscape);

        mRecordingFeedbackLayout = (FixedFrameLayout) activity.findViewById(R.id.recording_feedback_layout);

        scoreText = (ScoreView) activity.findViewById(R.id.score_text);
        navigationView = (NavigationView) activity.findViewById(R.id.navigation_view);
        mToolbar.setNavigationOnClickListener(mMenuListener);
        activity.setSupportActionBar(mToolbar);
        initNavigationDrawer();
        mActionBar = activity.getSupportActionBar();
    }

    void setBackListener(OnNavigationListener listener) {
        this.mNavigationListener = listener;
    }

    private void initNavigationDrawer() {
        mToolbar.setNavigationOnClickListener(mMenuListener);
        if (!appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true)) {
            MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
            item.setVisible(false);
        }
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                switch (id) {
                    case R.id.menu_settings:
                        mNavigationListener.openScreen(ScreenComposer.SCREEN_SETTINGS);
                        mDrawer.closeDrawers();
                        break;
                    case R.id.menu_upload:
                        if (!mUploadManager.isUploading()) {
                            if (Sequence.getLocalSequencesNumber() <= 0) {
                                mDrawer.closeDrawers();
                                showSnackBar(getString(R.string.no_local_recordings_message), Snackbar.LENGTH_LONG);
                                return true;
                            }
                            mNavigationListener.openScreen(ScreenComposer.SCREEN_WAITING);
                        } else {
                            mNavigationListener.openScreen(ScreenComposer.SCREEN_UPLOAD_PROGRESS);
                        }
                        mDrawer.closeDrawers();
                        break;
                    case R.id.menu_profile:
                        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
                        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

                        if (userName.equals("") || token.equals("")) {
                            showSnackBar(R.string.login_to_see_online_warning, Snackbar.LENGTH_LONG, getString(R.string.login_label), new Runnable() {
                                @Override
                                public void run() {
                                    if (Utils.isInternetAvailable(activity)) {
                                        activity.startActivity(new Intent(activity, LoginActivity.class));
                                    } else {
                                        showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
                                    }
                                }
                            });
                        } else {
                            mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE);
                        }
                        mDrawer.closeDrawers();
                        break;
                    case R.id.menu_leaderboard:
                        mNavigationListener.openScreen(ScreenComposer.SCREEN_LEADERBOARD);
                        mDrawer.closeDrawers();
                        break;
                }
                return true;
            }
        });

        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String displayName = appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME);
        String userPhoto = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);

        mHeader = navigationView.getHeaderView(0);
        mUsernameTextView = (TextView) mHeader.findViewById(R.id.username_label);
        mUserPhotoImageView = (ImageView) mHeader.findViewById(R.id.profile_picture);
        mLogOutImage = (ImageView) mHeader.findViewById(R.id.log_out_image_button);

        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("")) {
            mUserPhotoImageView.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_gray, null));
            mUsernameTextView.setText(R.string.login_label);
            mUsernameTextView.setOnClickListener(loginOnClickListener);
            mUserPhotoImageView.setOnClickListener(loginOnClickListener);
            mLogOutImage.setOnClickListener(loginOnClickListener);
            mLogOutImage.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "initNavigationDrawer: profile picture is: " + userPhoto);
            if (userPhoto.length() > 0) {
                Log.d(TAG, "initNavigationDrawer: loading profile picture");
                Glide.with(activity)
                        .load(userPhoto)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .dontAnimate()
                        .signature(new StringSignature("profile " + userName))
                        .priority(Priority.NORMAL)
                        .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_gray, null))
                        .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_gray, null))
                        .listener(MainActivity.mGlideRequestListener)
                        .into(mUserPhotoImageView);
            }
            mUsernameTextView.setText(displayName);
            mUsernameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE);
                }
            });
            mUserPhotoImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE);
                }
            });
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

    void getDrawerSize(Point point) {

        point.x = mDrawer.getMeasuredWidth();
        point.y = mDrawer.getMeasuredHeight();
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
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "refreshSignatureValue: signature " + signatureText);
                    signatureActionBarText.setText(signatureText);
                    if (getCurrentScreen() == ScreenComposer.SCREEN_UPLOAD_PROGRESS || getCurrentScreen() == ScreenComposer.SCREEN_WAITING) {
                        signatureActionBarText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    private int getCurrentScreen() {
        return mNavigationListener.getCurrentScreen();
    }

    private void showActionBar(String title, boolean showBackButton, int backgroundColor, int textColor, boolean light, int signatureColor) {
        showActionBar(title, showBackButton, backgroundColor, textColor, light);
        if (mActionBar == null) {
            return;
        }
        if (signatureColor != -1) {
            signatureActionBarText.setTextColor(ResourcesCompat.getColor(activity.getResources(), signatureColor, null));
            signatureActionBarText.setVisibility(View.VISIBLE);
            if (mUploadManager != null && mUploadManager.isUploading()) {
                signatureActionBarText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getCurrentScreen() == ScreenComposer.SCREEN_UPLOAD_PROGRESS) {
                            if (mUploadManager.isUploading()) {
                                mNavigationListener.onBackPressed();
                            }
                        }
                    }
                });
            }
        } else {
            signatureActionBarText.setVisibility(View.GONE);
        }
    }

    private void animateAppBar(final AppBarLayout appBar, final boolean show) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                appBar.animate()
                        .translationY(0)
                        .setDuration(300).start();
            }
        };
        appBar.animate()
                .translationY(-appBar.getHeight())
                .setDuration(show ? 0 : 300).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (show) {
                    runnable.run();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).start();
    }

    @SuppressWarnings("deprecation")
    private void showActionBar(String title, boolean showBackButton, int backgroundColor, int textColor, boolean light) {
//        mAppBar.setVisibility(View.VISIBLE);
        // if background color for the action bar is white set darker_grey color for the back icon
        animateAppBar(mAppBar, true);

        if (mActionBar == null) {
            return;
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
            lockDrawer(getCurrentScreen() == ScreenComposer.SCREEN_RECORDING);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!light) {
                Window window = activity.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                int clr = activity.getResources().getColor(backgroundColor);
                window.setStatusBarColor(clr);
            } else {
                Window window = activity.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
        }
        stopImmersiveMode();
    }

    private void hideActionBar() {
        animateAppBar(mAppBar, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
    }

    void onScreenChanged() {
        String title = "";
        boolean showBack = false;
        int backgroundColor = R.color.darker_grey, textColor = R.color.white;
        boolean isLight = false;
        int colorSignature = -1;
        boolean stopLocationUpdates = false;
        boolean startLocationUpdates = false;
        lockDrawer(true);
        switch (getCurrentScreen()) {
            case ScreenComposer.SCREEN_MAP:
                title = "";
                backgroundColor = R.color.white;
                isLight = true;
                startLocationUpdates = true;
                lockDrawer(false);
                break;
            case ScreenComposer.SCREEN_SETTINGS:
                showBack = true;
                title = getString(R.string.settings_label);
                isLight = true;
                backgroundColor = R.color.white;
                textColor = R.color.dark_grey_action_bar;
//                stopLocationUpdates = true;
                break;
            case ScreenComposer.SCREEN_MY_PROFILE:
                isLight = false;
                showBack = true;
                title = getString(R.string.hey_label) + appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME) + "!";
                backgroundColor = R.color.action_bar_blue;
                textColor = R.color.white;
//                stopLocationUpdates = true;
                break;
            case ScreenComposer.SCREEN_LEADERBOARD:
                isLight = false;
                showBack = true;
                title = getString(R.string.leaderboard);
                backgroundColor = R.color.leaderboard_green;
                textColor = R.color.white;
//                stopLocationUpdates = true;
                break;
            case ScreenComposer.SCREEN_RECORDING:
                startImmersiveMode();
                hideActionBar();
                int orientation = activity.getResources().getConfiguration().orientation;
                boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                setRecordingHintVisibility(portrait);
                EventBus.post(new RecordingVisibleEvent());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.postSticky(new GpsCommand(true));
                    }
                }, 1000);
                if (mRecordingFeedbackLayout != null) {
                    mRecordingFeedbackLayout.setVisibility(View.VISIBLE);
                    boolean gamification = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
                    scoreText.setVisibility(gamification ? View.VISIBLE : View.GONE);
                }
                return;
            case ScreenComposer.SCREEN_UPLOAD_PROGRESS:
                backgroundColor = R.color.dark_grey_action_bar;
                showBack = true;
                isLight = false;
                textColor = R.color.gray_text_color;
                colorSignature = R.color.white;
                title = getString(R.string.uploading_track_label);
                stopLocationUpdates = true;
                break;
            case ScreenComposer.SCREEN_WAITING:
                isLight = true;
                showBack = true;
                backgroundColor = R.color.white;
                textColor = R.color.dark_grey_action_bar;
                title = getString(R.string.upload_label);
                colorSignature = R.color.signature_waiting_upload;
                refreshSignatureValue(Utils.formatSize(Sequence.getTotalSize()));
//                stopLocationUpdates = true;
                break;
            case ScreenComposer.SCREEN_RECORDING_HINTS:
                hideActionBar();
                if (mRecordingFeedbackLayout != null) {
                    mRecordingFeedbackLayout.setVisibility(View.GONE);
                }
                orientation = activity.getResources().getConfiguration().orientation;
                portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                setRecordingHintVisibility(portrait);
                return;
            case ScreenComposer.SCREEN_PREVIEW:
                isLight = true;
                showBack = true;
                title = "";
                backgroundColor = R.color.transparent;
                textColor = R.color.white;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.postSticky(new GpsCommand(true));
                    }
                }, 1000);
                break;
            case ScreenComposer.SCREEN_NEARBY:
                isLight = false;
                showBack = true;
                title = getString(R.string.nearby_label);
                backgroundColor = R.color.dark_grey_action_bar;
                textColor = R.color.white;
                startLocationUpdates = true;
                break;
            case ScreenComposer.SCREEN_SUMMARY:
                isLight = false;
                showBack = true;
                title = getString(R.string.summary_label);
                backgroundColor = R.color.gray_summary_background;
                textColor = R.color.gray_summary_secondary_text;
                break;
        }

        if (mRecordingFeedbackLayout != null) {
            mRecordingFeedbackLayout.setVisibility(View.GONE);
        }
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        setRecordingHintVisibility(portrait);
        stopImmersiveMode();
        showActionBar(title, showBack, backgroundColor, textColor, isLight, colorSignature);
        if (stopLocationUpdates) {
            if (!mRecorder.isRecording()) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.postSticky(new GpsCommand(false));
                    }
                }, 1000);
            }
        }
        if (startLocationUpdates) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    EventBus.postSticky(new GpsCommand(true));
                }
            }, 1000);
        }
    }

    public void showSnackBar(final int resId, final int duration) {
        showSnackBar(resId, duration, null, null);
    }

    public void showSnackBar(final CharSequence text, final int duration) {
        showSnackBar(text, duration, null, null);
    }

    public void showSnackBar(final int resId, final int duration, final String button, final Runnable onClick) {
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
                mSnackBar = Snackbar.make(mDrawer, text, duration);
                if (button != null && onClick != null) {
                    mSnackBar.setAction(button, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClick.run();
                        }
                    });
                }
                shouldGoUp = true;
//                if (animateDownTimerTask != null) {
//                    shouldGoUp = !animateDownTimerTask.cancel();
//                }
//                mSnackBar.getView().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (shouldGoUp) {
//                            int height = mSnackBar.getView().getHeight();
//                            if (height < 20) {
//                                height = (int) Math.max(height, Utils.dpToPx(activity, 58));
//                            }
//                                Log.d(TAG, "showSnackbar: goUp -" + height);
//                                animateFabToPosition(-height);
//                        }
//                    }
//                });
                mSnackBar.show();
            }
        });
    }

    void startImmersiveMode() {
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                getWindow().getDecorView().setSystemUiVisibility(
//                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
//                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
//                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//            }
//        } catch (Exception e) {
//
//        }
    }

    private void stopImmersiveMode() {
//        int orientation = getResources().getConfiguration().orientation;
//        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
//        if (portrait) {
//            try {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    getWindow().getDecorView().setSystemUiVisibility(
//                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//                }
//            } catch (Exception e) {
//
//            }
//        }
    }

    private void setRecordingHintVisibility(boolean portrait) {
        if (portrait || getCurrentScreen() != ScreenComposer.SCREEN_RECORDING || mRecorder.isRecording()) {
            mRecordingHintLayoutLandscape.setVisibility(View.INVISIBLE);
        } else {
            mRecordingHintLayoutLandscape.setVisibility(View.VISIBLE);
        }
    }

    void setRecordingHintPosition(int x) {
        final int xold = x - mRecordingHintLayoutLandscape.getWidth();
        Log.d(TAG, "setRecordingHintPosition: x = " + xold);
        mRecordingHintLayoutLandscape.post(new Runnable() {
            @Override
            public void run() {
                mRecordingHintLayoutLandscape.setTranslationX(xold);//X(xold);
                mRecordingHintLayoutLandscape.requestLayout();
            }
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
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EventBus.clear(UploadEvent.class);
                EventBus.clear(UploadProgressEvent.class);
            }
        }, 1500);
    }

    @Subscribe
    public void onUploadingSequence(UploadingSequenceEvent event) {
        int size = mUploadManager.getOriginalSequencesNumber();
        Log.d(TAG, "onIndexingSequence: remaining recordings " + event.remainingSequences);
        refreshSignatureValue(Math.max(0, size - event.remainingSequences) + "/" + size);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUploadFinished(UploadFinishedEvent event) {
        showSnackBar(getString(R.string.upload_finished), Snackbar.LENGTH_LONG);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EventBus.clear(UploadEvent.class);
                EventBus.clear(UploadProgressEvent.class);
            }
        }, 1500);
    }

    @Subscribe
    public void onUploadStarted(UploadStartedEvent event) {
        int size = mUploadManager.getOriginalSequencesNumber();
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

    @Subscribe
    public void onGamificationEnabled(GamificationSettingEvent event) {
        if (navigationView != null) {
            MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
            if (item != null) {
                item.setVisible(event.enabled);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoginChanged(LoginChangedEvent event) {
        if (event.logged) {
            if (mLogOutImage != null) {
                mLogOutImage.setOnClickListener(logoutOnClickListener);
            }
            if (mUserPhotoImageView != null) {
                if (event.userPhoto != null && event.userPhoto.length() > 0) {
                    Log.d(TAG, "onLoginChanged: loading profile picture");
                    Glide.with(activity)
                            .load(event.userPhoto)
                            .centerCrop()
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false)
                            .signature(new StringSignature("profile " + event.username))
                            .priority(Priority.IMMEDIATE)
                            .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_gray, null))
                            .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_gray, null))
                            .listener(MainActivity.mGlideRequestListener)
                            .into(mUserPhotoImageView);
                }
                mUserPhotoImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE);
                    }
                });
            }
            if (mUsernameTextView != null) {
                mUsernameTextView.setText(event.displayName);
                mUsernameTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE);
                    }
                });
            }
        } else {
            Log.d(TAG, "onLoginChanged: removing profile picture");
            if (mUserPhotoImageView != null) {
                mUserPhotoImageView.setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_gray, null));
                mUserPhotoImageView.setOnClickListener(loginOnClickListener);
            }
            if (mUsernameTextView != null) {
                mUsernameTextView.setText("Login");
                mUsernameTextView.setOnClickListener(loginOnClickListener);
            }
            if (mLogOutImage != null) {
                mLogOutImage.setOnClickListener(loginOnClickListener);
            }
            closeDrawerIfOpen();
        }
    }

    @Subscribe(priority = 1)
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (!event.online && (mUploadManager == null || !mUploadManager.isUploading())) {
            refreshSignatureValue(Utils.formatSize(Sequence.getTotalSize()));
        }
    }

    void onHomePressed() {
        if (isBackButtonEnabled()) {
            mNavigationListener.onBackPressed();
        } else {
            toggleMenu();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        setRecordingHintVisibility(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    interface OnNavigationListener {

        void onBackPressed();

        void openScreen(int screen, Object extra);

        void openScreen(int screen);

        int getCurrentScreen();
    }
}
