package com.telenav.osv.ui;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.activity.LoginActivity;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.LogoutCommand;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.ui.UserTypeChangedEvent;
import com.telenav.osv.ui.custom.FixedFrameLayout;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.UiUtils;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Class holding all the code related to the navigation drawer
 * Created by Kalman on 13/02/2017.
 */
class ScreenDecorator {

    private final static String TAG = "ScreenDecorator";

    private final OSVActivity activity;

    private final TextView signatureActionBarText;

    private final SharedPreferences profilePrefs;

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

    private boolean mBackButtonShown = false;

    /**
     * Instance for {@code UserDataSource} which represents the user repository.
     */
    private UserDataSource userRepository;

    /**
     * Instance for {@code SequenceLocalDataSource} which represents the local data source for the sequence repository.
     */
    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * Container for Rx disposables which will automatically dispose them after execute.
     */
    @NonNull
    private CompositeDisposable compositeDisposable;

    private View.OnClickListener mMenuListener = v -> onHomePressed();

    private FixedFrameLayout mRecordingFeedbackLayout;

    private ImageView mUserPhotoImageView;

    private View.OnClickListener logoutOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialog);
            builder.setMessage(getString(R.string.logout_confirmation_message)).setTitle(getString(R.string.log_out))
                    .setNegativeButton(R.string.cancel_label, (dialog, which) -> {

                    }).setPositiveButton(R.string.log_out, (dialog, which) -> EventBus.post(new LogoutCommand())).create().show();
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

    ScreenDecorator(OSVActivity activity, @NonNull UserDataSource userRepository, @NonNull SequenceLocalDataSource sequenceLocalDataSource) {
        this.userRepository = userRepository;
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        compositeDisposable = new CompositeDisposable();
        this.activity = activity;
        appPrefs = activity.getApp().getAppPrefs();
        profilePrefs = activity.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
        mDrawer = activity.findViewById(R.id.activity_main_root);
        mAppBar = activity.findViewById(R.id.app_bar);
        mToolbar = activity.findViewById(R.id.app_toolbar);

        signatureActionBarText = mToolbar.findViewById(R.id.signature_action_bar_text);

        mRecordingFeedbackLayout = activity.findViewById(R.id.recording_feedback_layout);
        navigationView = activity.findViewById(R.id.navigation_view);
        mToolbar.setNavigationOnClickListener(mMenuListener);
        activity.setSupportActionBar(mToolbar);
        initNavigationDrawer();
        mActionBar = activity.getSupportActionBar();
    }

    public void showSnackBar(final int resId, final int duration) {
        showSnackBar(resId, duration, null, null);
    }

    public void showSnackBar(final int resId, final int duration, final int buttonResId, final Runnable onClick) {
        showSnackBar(activity.getText(resId), duration, activity.getText(buttonResId), onClick);
    }

    public void showSnackBar(final CharSequence text, final int duration, final CharSequence button, final Runnable onClick) {
        UiUtils.showSnackBar(activity.getApplicationContext(), mDrawer, text.toString(), duration, button, onClick);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUserTypeChanged(UserTypeChangedEvent event) {
        Log.d(TAG, "onUserTypeChanged: " + event.type);
        if (event.isDriver()) {

            MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
            item.setVisible(false);

            if (event.type == PreferenceTypes.USER_TYPE_BYOD) {
                setupVisibilityOfDriverGuideItem(navigationView);
            }
        } else {
            MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
            item.setVisible(true);
            navigationView.getMenu().findItem(R.id.menu_driver_guide).setVisible(false);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoginChanged(LoginChangedEvent event) {
        if (event.logged) {
            if (mLogOutImage != null) {
                mLogOutImage.setOnClickListener(logoutOnClickListener);
            }
            if (mUserPhotoImageView != null) {
                if (event.accountData.getProfilePictureUrl() != null && event.accountData.getProfilePictureUrl().length() > 0) {
                    Log.d(TAG, "onLoginChanged: loading profile picture");
                    Glide.with(activity).load(event.accountData.getProfilePictureUrl()).centerCrop().dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                            .signature(new StringSignature("profile " + event.accountData.getUserName() + "-" + event.accountData.getProfilePictureUrl()))
                            .priority(Priority.IMMEDIATE)
                            .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                            .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                            .listener(MainActivity.mGlideRequestListener).into(mUserPhotoImageView);
                }
                mUserPhotoImageView.setOnClickListener(v -> mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE));
            }
            if (mUsernameTextView != null) {
                mUsernameTextView.setText(event.accountData.getDisplayName());
                mUsernameTextView.setOnClickListener(v -> mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE));
            }
        } else {
            Log.d(TAG, "onLoginChanged: removing profile picture");
            if (mUserPhotoImageView != null) {
                mUserPhotoImageView
                        .setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null));
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
        EventBus.clear(LoginChangedEvent.class);
    }

    void setBackListener(OnNavigationListener listener) {
        this.mNavigationListener = listener;
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

    void onScreenChanged() {
        String title = "";
        boolean showBack = false;
        boolean showSend = false;
        int backgroundColor = R.color.darker_grey, textColor = R.color.default_white;
        int statusBarColor = -1;
        boolean isLight = false;
        int colorSignature = -1;
        lockDrawer(true);
        boolean showActionBar = true;
        switch (getCurrentScreen()) {
            case ScreenComposer.SCREEN_MAP:
                title = getString(R.string.app_title);
                textColor = R.color.default_black;
                backgroundColor = R.color.default_white;
                isLight = true;
                lockDrawer(false);
                break;
            case ScreenComposer.SCREEN_SETTINGS:
                showBack = true;
                title = getString(R.string.settings);
                isLight = true;
                backgroundColor = R.color.default_white;
                textColor = R.color.default_black_lighter;
                break;
            case ScreenComposer.SCREEN_MY_PROFILE:
                statusBarColor = R.color.default_blue;
                showActionBar = false;
                break;
            case ScreenComposer.SCREEN_LEADERBOARD:
                isLight = false;
                showBack = true;
                title = getString(R.string.leaderboard);
                backgroundColor = R.color.default_green;
                statusBarColor = R.color.default_green_darker;
                textColor = R.color.default_white;
                break;
            case ScreenComposer.SCREEN_PREVIEW:
                isLight = true;
                showBack = true;
                title = "";
                backgroundColor = R.color.default_transparent;
                textColor = R.color.default_white;
                break;
            case ScreenComposer.SCREEN_PREVIEW_FULLSCREEN:
                isLight = false;
                showBack = true;
                title = "";
                backgroundColor = R.color.default_transparent;
                textColor = R.color.default_white;
                break;
            case ScreenComposer.SCREEN_NEARBY:
                backgroundColor = R.color.default_blue;
                Window window = activity.getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                int clr = activity.getResources().getColor(backgroundColor);
                window.setStatusBarColor(clr);
                showActionBar = false;
                break;
            case ScreenComposer.SCREEN_REPORT:
                isLight = false;
                showBack = true;
                showSend = true;
                title = getString(R.string.issue_report_label);
                backgroundColor = R.color.default_blue;
                textColor = R.color.default_white;
                break;
            case ScreenComposer.SCREEN_DRIVER_GUIDE:
                isLight = false;
                showBack = true;
                showSend = false;
                title = "";
                backgroundColor = R.color.default_blue;
                textColor = R.color.default_white;
                statusBarColor = R.color.default_blue;
                break;
            default:
                //nothing to be done here
                // the return is required as a work-around fix for fragments which were moved to obd activity, this will be completely removed once all fragment transition
                return;
        }

        if (mRecordingFeedbackLayout != null) {
            mRecordingFeedbackLayout.setVisibility(View.GONE);
        }
        stopImmersiveMode();
        if (showActionBar) {
            showActionBar(title, showBack, showSend, backgroundColor, textColor, isLight, colorSignature);
        } else {
            hideActionBar();
        }
        setStatusBarColor(statusBarColor);
    }

    void onHomePressed() {
        if (isBackButtonEnabled()) {
            mNavigationListener.onBackPressed();
        } else {
            toggleMenu();
        }
    }

    private void setupVisibilityOfDriverGuideItem(NavigationView navigationView) {
        boolean byod20 = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10).equals(ProfileFragment
                .PAYMENT_MODEL_VERSION_20);
        if (byod20) {
            navigationView.getMenu().findItem(R.id.menu_driver_guide).setVisible(true);
        } else {
            navigationView.getMenu().findItem(R.id.menu_driver_guide).setVisible(false);
        }
    }

    private void initNavigationDrawer() {
        mToolbar.setNavigationOnClickListener(mMenuListener);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            switch (id) {
                case R.id.menu_settings:
                    mNavigationListener.openScreen(ScreenComposer.SCREEN_SETTINGS);
                    mDrawer.closeDrawers();
                    break;
                case R.id.menu_upload:
                    Disposable openWaitingDisposable = Completable.create(emitter -> {
                        if (sequenceLocalDataSource.isPopulated()) {
                            emitter.onComplete();
                        } else {
                            emitter.onError(new Throwable("Empty sequences in db."));
                        }
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    //onComplete
                                    () -> {
                                        Log.d(TAG, "initNavigationDrawer. Status: success. Message: Sequences db is populated.");
                                        mNavigationListener.openScreen(ScreenComposer.SCREEN_WAITING);
                                        /*if (!mUploadManager.isUploading()) {

                                        } else {
                                            Log.d(TAG, "initNavigationDrawer. Status: abort. Message: Upload in progress, opening upload progress screen.");
                                            mNavigationListener.openScreen(ScreenComposer.SCREEN);
                                        }*/
                                        mDrawer.closeDrawers();
                                    },
                                    //onError
                                    throwable -> {
                                        Log.d(TAG, String.format("initNavigationDrawer. Status: error. Message: %s", throwable.getLocalizedMessage()));
                                        mDrawer.closeDrawers();
                                        showSnackBar(getString(R.string.no_local_recordings_message), Snackbar.LENGTH_LONG);
                                    });
                    compositeDisposable.add(openWaitingDisposable);
                    break;
                case R.id.menu_profile:
                    Disposable disposable =
                            userRepository
                                    .getUser()
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            //onComplete
                                            optionalUser -> {
                                                Log.d(TAG, "initNavigationDrawer profileTap. Status: success. Message: User found, opening my profile screen.");
                                                mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE);
                                                mDrawer.closeDrawers();
                                            },
                                            //onError
                                            throwable -> Log.d(TAG, String.format("initNavigationDrawer profileTap. Status: error. Message: %s", throwable.getMessage())),
                                            //onComplete
                                            () -> {
                                                Log.d(TAG, "initNavigationDrawer profileTap. Status: complete. Message: User not found.");
                                                showSnackBar(R.string.login_to_see_online_warning, Snackbar.LENGTH_LONG, getString(R.string.login_label), () -> {
                                                    if (Utils.isInternetAvailable(activity)) {
                                                        activity.startActivity(new Intent(activity, LoginActivity.class));
                                                    } else {
                                                        showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
                                                    }
                                                });
                                            }
                                    );
                    compositeDisposable.add(disposable);
                    break;
                case R.id.menu_leaderboard:
                    mNavigationListener.openScreen(ScreenComposer.SCREEN_LEADERBOARD);
                    mDrawer.closeDrawers();
                    break;
                case R.id.menu_driver_guide:
                    mNavigationListener.openScreen(ScreenComposer.SCREEN_DRIVER_GUIDE);
                    mDrawer.closeDrawers();
                    break;
            }
            return true;
        });

        View mHeader = navigationView.getHeaderView(0);
        mUsernameTextView = mHeader.findViewById(R.id.username_label);
        mUserPhotoImageView = mHeader.findViewById(R.id.profile_picture);
        mLogOutImage = mHeader.findViewById(R.id.log_out_image_button);

        Disposable disposable =
                userRepository
                        .getUser()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                //onSuccess
                                user -> {
                                    String userPhoto = appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL);
                                    Log.d(TAG, "initNavigationDrawer. Status: success. Message: User found. Updating profile info.");
                                    int userType = user.getUserType();
                                    if (userPhoto.length() > 0) {
                                        Log.d(TAG, "initNavigationDrawer. Status: updating profile info. Message: Loading profile picture from url.");
                                        Glide.with(activity).load(userPhoto).centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false).dontAnimate()
                                                .signature(new StringSignature("profile " + user.getUserName())).priority(Priority.NORMAL)
                                                .placeholder(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                                                .error(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null))
                                                .listener(MainActivity.mGlideRequestListener).into(mUserPhotoImageView);
                                    }
                                    mUsernameTextView.setText(user.getDisplayName());
                                    mUsernameTextView.setOnClickListener(v ->
                                            mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE));

                                    mUserPhotoImageView.setOnClickListener(v ->
                                            mNavigationListener.openScreen(ScreenComposer.SCREEN_MY_PROFILE));
                                    mLogOutImage.setOnClickListener(logoutOnClickListener);

                                    boolean isDriver = isDriver(userType);
                                    Log.d(TAG, String.format("initNavigationDrawer. Status for driver: %s. Message: User found. Updating driver info.", isDriver));
                                    if (isDriver) {
                                        MenuItem item = navigationView.getMenu().findItem(R.id.menu_leaderboard);
                                        item.setVisible(false);
                                        if (userType == PreferenceTypes.USER_TYPE_BYOD) {
                                            setupVisibilityOfDriverGuideItem(navigationView);
                                        } else {
                                            navigationView.getMenu().findItem(R.id.menu_driver_guide).setVisible(false);
                                        }
                                    } else {
                                        navigationView.getMenu().findItem(R.id.menu_driver_guide).setVisible(false);
                                    }
                                },
                                //onError
                                throwable -> Log.d(TAG, String.format("Get user invalid. Status: error. Message: %s", throwable.getMessage())),
                                //onComplete
                                () -> {
                                    Log.d(TAG, "initNavigationDrawer. Status: success. Message: User not found. Updating profile info.");
                                    mUserPhotoImageView
                                            .setImageDrawable(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_profile_placeholder, null));
                                    mUsernameTextView.setText(R.string.login_label);
                                    mUsernameTextView.setOnClickListener(loginOnClickListener);
                                    mUserPhotoImageView.setOnClickListener(loginOnClickListener);
                                    mLogOutImage.setOnClickListener(loginOnClickListener);
                                    mLogOutImage.setVisibility(View.VISIBLE);
                                    navigationView.getMenu().findItem(R.id.menu_driver_guide).setVisible(false);
                                }
                        );
        compositeDisposable.add(disposable);
    }

    private boolean isDriver(int userType) {
        return userType == PreferenceTypes.USER_TYPE_BYOD || userType == PreferenceTypes.USER_TYPE_BAU || userType == PreferenceTypes.USER_TYPE_DEDICATED;
    }

    private String getString(int id) {
        return activity.getString(id);
    }

    private int getCurrentScreen() {
        return mNavigationListener.getCurrentScreen();
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
            /*if (mUploadManager != null && mUploadManager.isUploading()) {
                signatureActionBarText.setOnClickListener(v -> {
                    if (getCurrentScreen() == ScreenComposer.SCREEN_UPLOAD_PROGRESS) {
                        if (mUploadManager.isUploading()) {
                            mNavigationListener.onBackPressed();
                        }
                    }
                });
            }*/
        } else {
            signatureActionBarText.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("deprecation")
    private void showActionBar(String title, boolean showBackButton, boolean showSend, int backgroundColor, int textColor, boolean light) {
        //        mAppBar.setVisibility(View.VISIBLE);
        // if background color for the action bar is white set darker_grey color for the back icon
        mToolbar.setVisibility(View.VISIBLE);

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
            Drawable menuButton = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.vector_menu, null);
            if (menuButton != null) {
                if (light) {
                    menuButton.setColorFilter(ResourcesCompat.getColor(activity.getResources(), R.color.darker_grey, null), PorterDuff.Mode.SRC_ATOP);
                } else {
                    menuButton.setColorFilter(ResourcesCompat.getColor(activity.getResources(), R.color.default_white, null), PorterDuff.Mode.SRC_ATOP);
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
        mToolbar.setClickable(backgroundColor != R.color.default_transparent);

        stopImmersiveMode();
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
        mToolbar.setVisibility(View.GONE);
    }

    private void showSnackBar(final CharSequence text, final int duration) {
        showSnackBar(text, duration, null, null);
    }

    private void showSnackBar(final int resId, final int duration, final String button, final Runnable onClick) {
        showSnackBar(activity.getText(resId), duration, button, onClick);
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

    interface OnNavigationListener {

        void onBackPressed();

        void openScreen(int screen, Object extra);

        void openScreen(int screen);

        int getCurrentScreen();
    }
}
