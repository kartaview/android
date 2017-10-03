package com.telenav.osv.activity;

import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.Target;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.api.Status;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.util.SKLogging;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.ValueFormatter;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.command.ObdResetCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.CameraPermissionEvent;
import com.telenav.osv.event.hardware.LocationPermissionEvent;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.event.hardware.camera.FrameQueueEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.AccuracyEvent;
import com.telenav.osv.event.hardware.obd.ObdSpeedEvent;
import com.telenav.osv.event.network.matcher.CoverageEvent;
import com.telenav.osv.event.network.matcher.ScoreChangedEvent;
import com.telenav.osv.event.ui.OrientationChangedEvent;
import com.telenav.osv.event.ui.PositionerEvent;
import com.telenav.osv.event.ui.ShutterPressEvent;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.network.LoginManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.service.UploadJobService;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.ScoreView;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.fabric.sdk.android.Fabric;
import static com.telenav.osv.manager.obd.ObdManager.STATE_CONNECTED;
import static com.telenav.osv.manager.obd.ObdManager.STATE_CONNECTING;
import static com.telenav.osv.manager.obd.ObdManager.STATE_DISCONNECTED;

/**
 * Activity displaying the map - camera view - profile screen
 */

public class MainActivity extends OSVActivity implements HasSupportFragmentInjector {

    /**
     * Intent extra used when opening app from recording notification
     * to go directly to camera view
     */
    public static final String K_OPEN_CAMERA = "open_camera";

    private static final String TAG = "MainActivity";

    public static final com.bumptech.glide.request.RequestListener<String, GlideDrawable> mGlideRequestListener =
            new com.bumptech.glide.request.RequestListener<String, GlideDrawable>() {

                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                    if (e != null) {
                        Log.w(TAG, "Glide: " + e.getLocalizedMessage());
                    } else {
                        Log.w(TAG, "Glide: exception during image load, no details");
                    }
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache,
                                               boolean isFirstResource) {
                    return false;
                }
            };

    private static final int REQUEST_CODE_GPS = 10113;

    private static final int REQUEST_CODE_GPS_RECORD = 20113;

    @Inject
    Recorder mRecorder;

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Inject
    LoginManager mLoginManager;

    /**
     * the camera handler service that is used to open and hold the camera and keep the app in foreground during recording
     */
    private CameraHandlerService mCameraHandlerService;

    private boolean mBoundCameraHandler;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Status locationResolution;

    private TextView mOBDIcon;

    private FrameLayout mOBDIconHolder;

    private TextView mOBDUnit;

    private ImageView mGPSIcon;

    private ScoreView scoreText;

    private AlertDialog mSafeModeDialog;

    private AlertDialog mClearRecentsDialog;

    private AlertDialog mDriverModeDialog;

    private ScreenComposer mScreenComposer;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mCameraHandlerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            CameraHandlerService.CameraHandlerBinder binder = (CameraHandlerService.CameraHandlerBinder) service;
            mCameraHandlerService = binder.getService();
            mBoundCameraHandler = true;
            if (mRecorder.isRecording()) {
                openScreen(SCREEN_RECORDING);
            }
            enableProgressBar(false);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCameraHandlerService = null;
            mBoundCameraHandler = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SKLogging.enableLogs(false);
        mScreenComposer = new ScreenComposer(this, mRecorder, appPrefs);
        mScreenComposer.setLoginManager(mLoginManager);
        mOBDIcon = findViewById(R.id.obd_icon);
        mGPSIcon = findViewById(R.id.gps_icon);
        scoreText = findViewById(R.id.score_text);
        mOBDIconHolder = findViewById(R.id.obd_icon_holder);
        mOBDUnit = findViewById(R.id.obd_icon_unit);

        scoreText.setOnClickListener(
                v -> showSnackBar(getString(R.string.hint_score_coverage_incentive_message), Snackbar.LENGTH_LONG));
        if (!mBoundCameraHandler) {
            enableProgressBar(true);
        }
        appPrefs.getObdStatusLive().observe(this, i -> {
            if (i != null) {
                onObdStatusEvent(i);
            }
        });
        appPrefs.getAutoUploadLive().observe(this, aBoolean -> {
            if (aBoolean != null && aBoolean) {
                UploadJobService.scheduleAutoUpload(MainActivity.this, appPrefs);
            } else {
                UploadJobService.cancelAutoUpload(MainActivity.this);
            }
        });
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        EventBus.post(new OrientationChangedEvent());
        mScreenComposer.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SKMaps.getInstance().destroySKMaps();
        if (mBoundCameraHandler) {
            mCameraHandlerService.stopSelf();
            unbindService(mCameraHandlerConnection);
            mBoundCameraHandler = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                mScreenComposer.onHomePressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public OSVApplication getApp() {
        return (OSVApplication) getApplication();
    }

    public void resolveLocationProblem(boolean record) {
        if (locationResolution != null) {
            try {
                // Show the dialog by calling startResolutionForResult(),
                // and check the result in onActivityResult().
                int code = REQUEST_CODE_GPS;
                if (record) {
                    code = REQUEST_CODE_GPS_RECORD;
                }
                locationResolution.startResolutionForResult(this, code);
            } catch (IntentSender.SendIntentException e) {
                // Ignore the error.
            }
        }
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

    public int getCurrentScreen() {
        return mScreenComposer.getCurrentScreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
        appPrefs.setCrashed(true);
        Log.d(TAG, "onStart: K_CRASHED is set to true");
        EventBus.register(this);
        mScreenComposer.register();
        mScreenComposer.onStart();
        tryToConnectOBD();
        if (!mBoundCameraHandler) {
            Intent service = new Intent(getApplicationContext(), CameraHandlerService.class);
            bindService(service, mCameraHandlerConnection, BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "Service not killed, connecting to camera fragment");
        }

        appPrefs.getUserTypeLive().observe(this, this::onUserTypeChanged);
        checkUserInfo();
    }

    @Override
    protected void onStop() {
        if (mBoundCameraHandler && !mRecorder.isRecording()) {
            if (mCameraHandlerService != null) {
                mCameraHandlerService.stopSelf();
            }
            unbindService(mCameraHandlerConnection);
            mBoundCameraHandler = false;
        }
        appPrefs.setRestartCounter(0);
        if (!mRecorder.isRecording()) {
            EventBus.postSticky(new ObdCommand(false));
            appPrefs.setCrashed(false);
            Log.d(TAG, "onStop: K_CRASHED is set to false");
        }
        mScreenComposer.unregister();
        EventBus.unregister(this);
        if (Fabric.isInitialized()) {
            Crashlytics.setInt(Log.CURRENT_SCREEN, -1);//means background
        }
        super.onStop();
    }

    @Override
    public boolean hasPosition() {
        return mRecorder != null && mRecorder.hasPosition();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (resultCode == -1) {
            //SUCCESS
            if (requestCode == REQUEST_CODE_GPS_RECORD) {
                if (mCameraHandlerService != null && getCurrentScreen() == SCREEN_RECORDING && mRecorder.hasPosition()) {
                    mRecorder.startRecording();
                }
            } else if (requestCode == REQUEST_CODE_GPS) {
                EventBus.postSticky(new GpsCommand(true));
                mHandler.postDelayed(() -> EventBus.post(new PositionerEvent()), 700);
            }
        } else {
            Log.d(TAG, "onActivityResult: result is error for " + requestCode);
            //DO NOTHING
            showSnackBar(R.string.warning_enable_gps_message, Snackbar.LENGTH_LONG, R.string.enable_label, () -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            });
        }
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

        if (getIntent().getBooleanExtra(K_OPEN_CAMERA, false)) {//go directly to camera view, removing any fragments over the pager
            runOnUiThread(() -> openScreen(SCREEN_RECORDING));
        }

        if (getIntent() != null && getIntent().getBooleanExtra(SplashActivity.RESTART_FLAG, false)) {
            getIntent().removeExtra(SplashActivity.RESTART_FLAG);
            showSnackBar(R.string.app_restarted, Snackbar.LENGTH_LONG);
        }
        if (appPrefs.shouldShowSafeModeMessage()) {
            if (mSafeModeDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mSafeModeDialog =
                        builder.setMessage(R.string.warning_problems_recording).setTitle(R.string.jpeg_recording_label).setCancelable(false)
                                .setNegativeButton(R.string.disable_label, (dialog, which) -> {
                                    appPrefs.setSafeMode(false);
                                    appPrefs.setShouldShowSafeModeMessage(false);
                                }).setPositiveButton(R.string.enable_label,
                                (dialog, which) -> appPrefs.setShouldShowSafeModeMessage(false))
                                .create();
            }
            if (!mSafeModeDialog.isShowing()) {
                mSafeModeDialog.show();
            }
        }
        if (appPrefs.shouldShowClearRecentsWarning()) {
            if (mClearRecentsDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mClearRecentsDialog = builder.setMessage(R.string.warning_swipe_recents_text).setTitle(R.string.warning_label).setCancelable(false)
                        .setPositiveButton(R.string.ok_label, (dialog, which) -> appPrefs.setShouldShowClearRecentsWarning(false))
                        .create();
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
            case OSVApplication.START_RECORDING_PERMISSION:
                i = 0;
                for (String ignored : permissions) {
                    if (grantResults.length > i && grantResults[i] < 0) {
                        return;
                    }
                    i++;
                }
                EventBus.postSticky(new GpsCommand(true));
                EventBus.clear(CameraPermissionEvent.class);
                mHandler.postDelayed(() -> {
                    if (getCurrentScreen() == SCREEN_RECORDING) {
                        EventBus.post(new ShutterPressEvent());
                    }
                }, 500);
                break;
            case OSVApplication.CAMERA_PERMISSION:
                i = 0;
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.CAMERA)) {
                        if (grantResults.length > i && grantResults[i] >= 0) {
                            EventBus.clear(CameraPermissionEvent.class);
                            mRecorder.openCamera();
                            return;
                        } else {
                            mHandler.postDelayed(() -> EventBus.postSticky(new CameraPermissionEvent()), 1000);
                            break;
                        }
                    }
                    i++;
                }
                openScreen(SCREEN_MAP);
                break;
            case OSVApplication.LOCATION_PERMISSION:
                i = 0;
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResults.length > i && grantResults[i] >= 0) {
                            EventBus.postSticky(new GpsCommand(true));
                            mHandler.postDelayed(() -> EventBus.post(new PositionerEvent()), 500);
                            return;
                        }
                    }
                    i++;
                }
                break;
            case OSVApplication.LOCATION_PERMISSION_BT:
                i = 0;
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResults.length > i && grantResults[i] >= 0) {
                            EventBus.postSticky(new GpsCommand(true));
                            return;
                        }
                    }
                    i++;
                }
                break;
        }
    }

    @Override
    public DispatchingAndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }

    public void onUserTypeChanged(int type) {
        if (UserData.isDriver(type) && appPrefs.shouldShowDriverDialog()) {
            boolean mapWasEnabled = appPrefs.isMapEnabled();
            boolean miniMap = appPrefs.isMiniMapEnabled();
            boolean gamification = appPrefs.isGamificationEnabled();

            boolean needed = mapWasEnabled || miniMap || gamification;
            if (mDriverModeDialog == null && needed) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mDriverModeDialog =
                        builder.setMessage(R.string.warning_optimization_message).setTitle(R.string.warning_optimization_label).setCancelable(false)
                                .setPositiveButton(R.string.ok_label, (dialog, which) -> {
                                    boolean mapWasEnabled1 = appPrefs.isMapEnabled();
                                    appPrefs.setMapEnabled(false);
                                    appPrefs.setMiniMapEnabled(false);
                                    appPrefs.setGamificationEnabled(false);
                                    if (mapWasEnabled1) {
                                        Intent mStartActivity = new Intent(MainActivity.this, SplashActivity.class);
                                        int mPendingIntentId = 123456;
                                        PendingIntent mPendingIntent =
                                                PendingIntent.getActivity(MainActivity.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                                        AlarmManager mgr = (AlarmManager) MainActivity.this.getSystemService(Context.ALARM_SERVICE);
                                        if (mgr != null) {
                                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                                        }
                                        appPrefs.setCrashed(false);
                                        android.os.Process.killProcess(android.os.Process.myPid());
                                        System.exit(0);
                                    }
                                }).create();
            }
            if (needed && !mDriverModeDialog.isShowing()) {
                mDriverModeDialog.show();
                appPrefs.setShouldShowDriverDialog(false);
                if (Fabric.isInitialized()) {
                    Answers.getInstance().logCustom(
                            new CustomEvent("Drivermode dialog displayed")
                                    .putCustomAttribute("user_name", appPrefs.getUserName()));
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRecordingStatusChanged(final RecordingEvent event) {
        final boolean started = event.started;
        if (scoreText != null && appPrefs.isGamificationEnabled()) {
            scoreText.setScore(0);
            scoreText.setActive(event.started);
        }
        if (!started && appPrefs.shouldShowRecordingSummary()) {
            if (event.sequence != null && event.sequence.getScore() >= 10) {
                openScreen(SCREEN_SUMMARY, event.sequence);
                event.sequence = null;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScoreChanged(ScoreChangedEvent event) {
        Log.d(TAG, "onScoreChanged: " + event.score);
        if (scoreText != null) {
            scoreText.setCoverageAvailable(event.multiplier > 0);
            scoreText.setScore((int) event.score);
            scoreText.setMultiplier(event.multiplier);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCoverageChanged(CoverageEvent event) {
        Log.d(TAG, "onCoverageChanged: available = " + event.available);
        if (scoreText != null) {
            scoreText.setCoverageAvailable(event.available);
        }
    }

    /**
     * callback with the number of frames held in-memory waiting to be encoded,  currently this is not capped
     * snackbar only displayed when debug mode enabled
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFrameQueueChanged(FrameQueueEvent event) {
        if (Utils.DEBUG) {
            if (event.queueSize > 3) {
                showSnackBar(getString(R.string.partial_frames_in_queue_message) + event.queueSize, Snackbar.LENGTH_INDEFINITE);
            } else {
                hideSnackBar();
            }
        }
    }

    @Subscribe(sticky = true)
    public void onResolutionNeeded(LocationPermissionEvent event) {
        this.locationResolution = event.status;
    }

    @Subscribe(sticky = true)
    public void onCameraReady(final CameraInitEvent event) {
        if (event.type == CameraInitEvent.TYPE_READY) {
            EventBus.post(new OrientationChangedEvent());
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onCameraPermissionNeeded(CameraPermissionEvent event) {
        Log.d(TAG, "onCameraPermissionNeeded: ");
        checkPermissionsForCamera();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onAccuracyChanged(AccuracyEvent event) {
        int type = event.type;
        if (mGPSIcon != null) {
            switch (type) {
                case LocationManager.ACCURACY_GOOD:
                    Log.d(TAG, "onAccuracyChanged: " + getString(R.string.gps_ok_label));
                    mGPSIcon.setImageDrawable(getResources().getDrawable(R.drawable.vector_gps_good));
                    break;
                case LocationManager.ACCURACY_MEDIUM:
                    Log.d(TAG, "onAccuracyChanged: " + getString(R.string.gps_medium_label));
                    mGPSIcon.setImageDrawable(getResources().getDrawable(R.drawable.vector_gps_medium));
                    break;
                case LocationManager.ACCURACY_BAD:
                default:
                    Log.d(TAG, "onAccuracyChanged: " + getString(R.string.gps_bad_label));
                    mGPSIcon.setImageDrawable(getResources().getDrawable(R.drawable.vector_gps_low));
                    break;
            }
        }
    }

    public void onObdStatusEvent(int type) {
        switch (type) {
            case STATE_CONNECTED:
                if (scoreText != null) {
                    scoreText.setObdConnected(true);
                }
                if (mOBDIcon != null) {
                    mOBDIconHolder.setVisibility(View.VISIBLE);
                    mOBDIcon.setText("0");
                    mOBDUnit.setText(formatSpeedFromKmph(0)[1]);
                    mOBDIcon.setOnClickListener(v -> BackgroundThreadPool.post(() -> EventBus.post(new ObdResetCommand())));
                }
                break;
            case STATE_CONNECTING:
                if (mOBDIcon != null) {
                    mOBDIconHolder.setVisibility(View.VISIBLE);
                    mOBDIcon.setText("-");
                    mOBDUnit.setText(null);
                    mOBDIcon.setOnClickListener(null);
                }
                break;
            case STATE_DISCONNECTED:
                if (scoreText != null) {
                    scoreText.setObdConnected(false);
                }
                if (mOBDIconHolder != null) {
                    mOBDIconHolder.setVisibility(View.GONE);
                }
                break;
        }
    }

    public String[] formatSpeedFromKmph(int speed) {
        int ret = speed;
        if (!appPrefs.isUsingMetricUnits()) {
            ret = (int) (speed * ValueFormatter.KILOMETER_TO_MILE);
            return new String[]{ret + "", getString(R.string.mph_label)};
        } else {
            return new String[]{ret + "", getString(R.string.kmh_label)};
        }
    }

    //todo// recording info overlay, which contains stuff like obd indicator, gps iondicator, recording button hints, etc
    //todo// needs to be put into a RecordingOverlayFragment that gets added above the recording fragments when opening Recording Screen
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onObdSpeed(ObdSpeedEvent event) {
        if (mOBDIcon != null) {
            if (event.data.getSpeed() != -1) {
                String[] speed = formatSpeedFromKmph(event.data.getSpeed());
                mOBDIcon.setText(speed[0]);
                mOBDUnit.setText(speed[1]);
            } else {
                mOBDIcon.setText("-");
                mOBDUnit.setText("");
            }
        }
    }

    /**
     * this method was to check if usertype is unknown
     * if type is unknown we obtain it from the server
     * and if its a driver than we display the driver popup if needed
     */
    private void checkUserInfo() {
        if (appPrefs.isLoggedIn()) {
            int userType = appPrefs.getUserType();
            if (UserData.isDriver(userType)) {
                onUserTypeChanged(userType);
            }
        }
    }

    private void tryToConnectOBD() {
        EventBus.postSticky(new ObdCommand(true));
    }
}
