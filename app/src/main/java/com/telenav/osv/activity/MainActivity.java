package com.telenav.osv.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.ui.OrientationChangedEvent;
import com.telenav.osv.event.ui.UserTypeChangedEvent;
import com.telenav.osv.item.AccountData;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.obd.ObdActivity;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Activity displaying the map - camera view - profile screen
 */

public class MainActivity extends OSVActivity {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = MainActivity.class.getSimpleName();

    /*
     * This represents bundle parameter to state if login needs to be initiated on launch of this activity
     */
    public static final String KEY_INIT_LOGIN = "INIT_LOGIN";

    public static final RequestListener<Drawable> mGlideRequestListener =
            new RequestListener<Drawable>() {

                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    if (e != null) {
                        Log.w(TAG, "Glide: " + e.getLocalizedMessage());
                    } else {
                        Log.w(TAG, "Glide: exception during image load, no details");
                    }
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            };

    private AlertDialog mSafeModeDialog;

    private AlertDialog mClearRecentsDialog;

    private AlertDialog mDriverModeDialog;

    private ScreenComposer mScreenComposer;

    /**
     * Instance for {@code UserDataSource} which represents the user repository.
     */
    private UserDataSource userRepository;

    /**
     * Container for Rx disposables which will automatically dispose them after execute.
     */
    @NonNull
    private CompositeDisposable compositeDisposable;

    /**
     * Instance for the {@code SequenceLocalDataSource}.
     * @see SequenceLocalDataSource
     */
    private SequenceLocalDataSource sequenceLocalDataSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPrefs = ((KVApplication) getApplication()).getAppPrefs();
        compositeDisposable = new CompositeDisposable();
        Context context = getApplicationContext();
        userRepository = Injection.provideUserRepository(context);
        this.sequenceLocalDataSource = Injection.provideSequenceLocalDataSource(context,
                Injection.provideFrameLocalDataSource(context),
                Injection.provideScoreLocalDataSource(context),
                Injection.provideLocationLocalDataSource(context),
                Injection.provideVideoDataSource(context));
        setContentView(R.layout.activity_main);
        KVApplication.sUiThreadId = Thread.currentThread().getId();
        uploadManager = Injection.provideUploadManager();
        mScreenComposer = new ScreenComposer(this, userRepository, sequenceLocalDataSource);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getBoolean(KEY_INIT_LOGIN)) {
            mScreenComposer.initLogin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        compositeDisposable.clear();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        EventBus.post(new OrientationChangedEvent());
        mScreenComposer.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUserDataManager = new UserDataManager(this, userRepository);
        EventBus.register(this);
        mScreenComposer.register();
        mScreenComposer.onStart();
        checkUserInfo();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (mUserDataManager != null) {
            mUserDataManager.destroy();
            mUserDataManager = null;
        }
        appPrefs.saveIntPreference(PreferenceTypes.K_RESTART_COUNTER, 0);
        mScreenComposer.unregister();
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        compositeDisposable.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                mScreenComposer.onHomePressed();
                Log.d(TAG, "onHome button pressed");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public KVApplication getApp() {
        return (KVApplication) getApplication();
    }

    public int getCurrentScreen() {
        return mScreenComposer.getCurrentScreen();
    }

    @Override
    public void hideSnackBar() {
        mScreenComposer.hideSnackBar();
    }

    @Override
    public void showSnackBar(final CharSequence text, final int duration, final CharSequence button, final Runnable onClick) {
        mScreenComposer.showSnackBar(text, duration, button, onClick);
    }

    public void enableProgressBar(final boolean enable) {
        mScreenComposer.enableProgressBar(enable);
    }

    @Override
    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public void openScreen(int screen, Object extra) {
        mScreenComposer.openScreen(screen, extra);
    }

    public void resolveLocationProblem() {
        showSnackBar("Please enable your GPS, select \"High accuracy\" in the location settings.", Snackbar.LENGTH_LONG, "Enable",
                () -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                });
    }

    public void resolveRecordingProblem() {
        showSnackBar(R.string.camera_permission_required, Snackbar.LENGTH_LONG);
    }

    @Override
    public void onBackPressed() {
        mScreenComposer.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.splash_background_no_drawable));
        Log.d(TAG, "onResume: ");
        Utils.checkGooglePlayServices(this);
        enableProgressBar(false);
        if (getIntent() != null && getIntent().getBooleanExtra(SplashActivity.RESTART_FLAG, false)) {
            getIntent().removeExtra(SplashActivity.RESTART_FLAG);
            showSnackBar(R.string.app_restarted, Snackbar.LENGTH_LONG);
        }
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false)) {
            if (mSafeModeDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mSafeModeDialog =
                        builder.setMessage(R.string.warning_problems_recording).setTitle(R.string.jpeg_recording_label).setCancelable(false)
                                .setNegativeButton(R.string.disable_label, (dialog, which) -> {
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED, true);
                                    appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false);
                                }).setPositiveButton(R.string.enable_label, (dialog, which) -> appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false))
                                .create();
            }
            if (!mSafeModeDialog.isShowing()) {
                mSafeModeDialog.show();
            }
        }
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_SHOW_CLEAR_RECENTS_WARNING, false)) {
            if (mClearRecentsDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mClearRecentsDialog = builder.setMessage(R.string.warning_swipe_recents_text).setTitle(R.string.warning_label).setCancelable(false)
                        .setPositiveButton("ok", (dialog, which) -> appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_CLEAR_RECENTS_WARNING, false)).create();
            }
            if (!mClearRecentsDialog.isShowing()) {
                mClearRecentsDialog.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: ");
        int i;
        switch (requestCode) {
            case KVApplication.LOCATION_PERMISSION:
                i = 0;
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResults.length > i && grantResults[i] >= 0) {
                            if (!Utils.isGPSEnabled(getApplicationContext())) {
                                resolveLocationProblem();
                            }
                            notifyLocationPermissionListenersGranted();
                            return;
                        } else {
                            notifyLocationPermissionListenersDenied();
                        }
                    }
                    i++;
                }
                break;
            case KVApplication.CAMERA_PERMISSION:
                i = 0;
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.CAMERA)) {
                        if (grantResults.length > i && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            resolveRecordingProblem();
                            Log.d(TAG, "Camera permission was denied.");
                        }
                    }
                }
                break;
        }
    }

    public void goToRecordingScreen() {
        if (checkPermissionsForCamera()) {
            uploadManager.stop();
            startActivity(ObdActivity.newIntent(this, ObdActivity.SESSION_RECORDING));
            overridePendingTransition(R.anim.enter_from_down, R.anim.enter_to_up);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoginChanged(LoginChangedEvent event) {
        if (event.logged) {
            showSnackBar("Logged in as " + appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME), Snackbar.LENGTH_SHORT);
        } else {
            uploadManager.stop();
        }
    }

    /**
     * @return {@code true} if the camera permissions are granted, {@code false} otherwise.
     */
    public boolean checkPermissionsForCamera() {
        ArrayList<String> needed = new ArrayList<>();
        int cameraPermitted = ContextCompat.checkSelfPermission(getApplication().getApplicationContext(), Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }

        if (needed.size() > 0) {
            String[] array = new String[needed.size()];
            needed.toArray(array);
            ActivityCompat.requestPermissions(this, array, KVApplication.CAMERA_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    private void checkUserInfo() {
        Disposable disposable = userRepository
                .getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onSuccess
                        user -> {
                            //checks if the user is logged in
                            int userType = user.getUserType();
                            Log.d(TAG, String.format("checkUserInfo. Status: success. Id: %s. Message: User found.", user.getID()));
                            if (userType == PreferenceTypes.USER_TYPE_UNKNOWN) {
                                if (mUserDataManager != null) {
                                    Log.d(TAG, String.format("checkUserInfo userDetails. Status: init. Id: %s. Message: Requesting user details.", user.getID()));
                                    mUserDataManager.getUserProfileDetails(new NetworkResponseDataListener<UserData>() {

                                        @Override
                                        public void requestFailed(int status, UserData details) {
                                            Log.d(TAG, "checkUserInfo: " + status + ", details: " + details);
                                        }

                                        @Override
                                        public void requestFinished(int status, UserData userdata) {
                                            Log.d(TAG,
                                                    String.format("checkUserInfo userDetails. Status: init. Id: %s. Status code: %s. Message: Requesting user details.",
                                                            user.getID(),
                                                            status));
                                            if (userdata != null) {
                                                String id = appPrefs.getStringPreference(PreferenceTypes.K_USER_ID);
                                                String loginType = appPrefs.getStringPreference(PreferenceTypes.K_LOGIN_TYPE);
                                                String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
                                                String displayName = appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME);
                                                EventBus.postSticky(new LoginChangedEvent(true, new AccountData(id, userName, displayName, appPrefs
                                                        .getStringPreference(PreferenceTypes.K_USER_PHOTO_URL), userType, AccountData.getAccountTypeForString(loginType))));
                                                EventBus.postSticky(new UserTypeChangedEvent(userType));
                                            }
                                        }
                                    });
                                }
                            }
                        },
                        //onError
                        throwable -> Log.d(TAG, String.format("checkUserInfo. Status: error. Message: %s", throwable.getMessage())),
                        //OnComplete
                        () -> Log.d(TAG, "checkUserInfo. Status: complete. Message: User not found.")
                );
        compositeDisposable.add(disposable);
    }
}
