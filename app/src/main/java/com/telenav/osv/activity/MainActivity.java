package com.telenav.osv.activity;

import java.util.Objects;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
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
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.command.ObdCommand;
import com.telenav.osv.command.ObdResetCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.SdkEnabledEvent;
import com.telenav.osv.event.hardware.CameraPermissionEvent;
import com.telenav.osv.event.hardware.LocationPermissionEvent;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.event.hardware.camera.FrameQueueEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.AccuracyEvent;
import com.telenav.osv.event.hardware.gps.SpeedCategoryEvent;
import com.telenav.osv.event.hardware.obd.ObdSpeedEvent;
import com.telenav.osv.event.hardware.obd.ObdStatusEvent;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.network.matcher.CoverageEvent;
import com.telenav.osv.event.network.matcher.ScoreChangedEvent;
import com.telenav.osv.event.ui.ByodDriverPayRateUpdatedEvent;
import com.telenav.osv.event.ui.GamificationSettingEvent;
import com.telenav.osv.event.ui.ObdPressedEvent;
import com.telenav.osv.event.ui.OrientationChangedEvent;
import com.telenav.osv.event.ui.PositionerEvent;
import com.telenav.osv.event.ui.ShutterPressEvent;
import com.telenav.osv.event.ui.UserTypeChangedEvent;
import com.telenav.osv.item.AccountData;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.network.PayRateData;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.manager.location.ScoreManager;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.service.NetworkBroadcastReceiver;
import com.telenav.osv.service.UploadHandlerService;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.ByodPaymentIndicator;
import com.telenav.osv.ui.custom.ScoreIndicator;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * Activity displaying the map - camera view - profile screen
 */

public class MainActivity extends OSVActivity implements NetworkBroadcastReceiver.NetworkChangeListener {

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

    /**
     * the map fragment object
     */

    public View.OnClickListener resumeOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
            if (mUploadManager.isUploading()) {
                if (mUploadManager.isPaused()) {
                    if (NetworkUtils.isInternetAvailable(MainActivity.this)) {
                        if (dataSet || NetworkUtils.isWifiInternetAvailable(MainActivity.this)) {
                            mUploadManager.resumeUpload();
                        } else {
                            showSnackBar(R.string.no_wifi_label, Snackbar.LENGTH_SHORT);
                        }
                    } else {
                        showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);
                    }
                }
            }
        }
    };

    public View.OnClickListener pauseOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            mUploadManager.pauseUpload();
        }
    };

    /**
     * The fragments used in the pager
     */
    //    public List<Fragment> fragments = new ArrayList<>();

    private CameraHandlerService mCameraHandlerService;

    private UploadHandlerService mUploadHandlerService;

    public View.OnClickListener actionCancelListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mUploadHandlerService != null) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        mUploadManager.cancelUploadTasks();
                    }
                }).start();
            }
        }
    };

    private boolean mBoundCameraHandler;

    private boolean mBoundUploadHandler;

    private Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    private Status locationResolution;

    private Recorder mRecorder;

    private TextView distanceDebugText;

    private TextView speedDebugText;

    private TextView mOBDIcon;

    private FrameLayout mOBDIconHolder;

    private TextView mOBDUnit;

    private ImageView mGPSIcon;

    //    private ImageView signDetectionHolder;

    private ScoreIndicator scoreIndicator;

    private ByodPaymentIndicator byodPaymentIndicator;

    private AlertDialog mSafeModeDialog;

    private AlertDialog mClearRecentsDialog;

    private AlertDialog mDriverModeDialog;

    private ScreenComposer mScreenComposer;

    private OSVServiceConnection mUploadHandlerConnection;

    public View.OnClickListener actionUploadAllListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
            String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

            if (userName.equals("") || token.equals("")) {
                showSnackBar(R.string.login_to_upload_warning, Snackbar.LENGTH_LONG, getString(R.string.login_label), new Runnable() {

                    @Override
                    public void run() {
                        if (Utils.isInternetAvailable(MainActivity.this)) {
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        } else {
                            showSnackBar(R.string.check_internet_connection, Snackbar.LENGTH_LONG);
                        }
                    }
                });

                return;
            }
            if (!NetworkUtils.isInternetAvailable(MainActivity.this)) {
                showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);

                return;
            }
            if (LocalSequence.getStaticSequences().values().isEmpty()) {
                showSnackBar(R.string.no_local_recordings_message, Snackbar.LENGTH_SHORT);
                return;
            }
            if (!NetworkUtils.isWifiInternetAvailable(MainActivity.this) &&
                    !appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED)) {
                showSnackBar(R.string.no_wifi_label, Snackbar.LENGTH_SHORT);

                return;
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
            builder.setMessage(getString(R.string.upload_all_warning))
                    .setTitle(getString(R.string.upload_all_warning_title))
                    .setNegativeButton(R.string.cancel_label, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setPositiveButton(R.string.upload_all_label, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mUploadHandlerService == null || !mBoundUploadHandler) {
                        bindUploadService(new OSVServiceConnection() {

                            @Override
                            public void onServiceConnected(ComponentName className, IBinder service) {
                                super.onServiceConnected(className, service);
                                if (mUploadHandlerService != null && !mRecorder.isRecording()) {
                                    mUploadManager.uploadCache(LocalSequence.getStaticSequences().values());
                                    openScreen(ScreenComposer.SCREEN_UPLOAD_PROGRESS);
                                }
                            }
                        });
                    }
                }
            }).create().show();
        }
    };

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mCameraHandlerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            CameraHandlerService.CameraHandlerBinder binder = (CameraHandlerService.CameraHandlerBinder) service;
            mCameraHandlerService = binder.getService();
            mBoundCameraHandler = true;
            if (mRecorder == null) {
                mRecorder = getApp().getRecorder();
            }
            if (mRecorder != null && mRecorder.isRecording()) {
                openScreen(ScreenComposer.SCREEN_RECORDING);
            }
            if (mBoundUploadHandler) {
                enableProgressBar(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCameraHandlerService = null;
            mBoundCameraHandler = false;
        }
    };

    private BroadcastReceiver mNetworkBroadcastReceiver;

    private int loggedInUserType;

    private SharedPreferences profilePrefs;

    private boolean mInternetAvailable;

    private boolean mCoverageAvailable;

    private boolean mRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        profilePrefs = getSharedPreferences(ProfileFragment.PREFS_NAME, MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loggedInUserType = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, PreferenceTypes.USER_TYPE_UNKNOWN);
        SKLogging.enableLogs(false);
        mRecorder = getApp().getRecorder();
        OSVApplication.sUiThreadId = Thread.currentThread().getId();
        mUploadManager = getApp().getUploadManager();
        mScreenComposer = new ScreenComposer(this);
        mOBDIcon = findViewById(R.id.obd_icon);
        mGPSIcon = findViewById(R.id.gps_icon);
        mOBDIconHolder = findViewById(R.id.obd_icon_holder);
        mOBDUnit = findViewById(R.id.obd_icon_unit);
        //        signDetectionHolder = (ImageView) findViewById(R.id.sign_detection_container);
        distanceDebugText = findViewById(R.id.debug_distance_text);
        speedDebugText = findViewById(R.id.debug_speed_text);

        if (appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, false)) {
            distanceDebugText.setVisibility(View.VISIBLE);
            speedDebugText.setVisibility(View.VISIBLE);
        }
        if (!mBoundCameraHandler || !mBoundUploadHandler) {
            enableProgressBar(true);
        }
        mNetworkBroadcastReceiver = new NetworkBroadcastReceiver(MainActivity.this, appPrefs, mUploadManager);
        ((NetworkBroadcastReceiver) mNetworkBroadcastReceiver).setOnNetworkStatusChangedListener(this);
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
        registerReceiver(mNetworkBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mUserDataManager = new UserDataManager(this);
        appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, true);
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

        checkUserInfo();
    }

    @Override
    protected void onStop() {
        if (mUserDataManager != null) {
            mUserDataManager.destroy();
            mUserDataManager = null;
        }
        if (mBoundCameraHandler && !mRecorder.isRecording()) {
            if (mCameraHandlerService != null) {
                mCameraHandlerService.stopSelf();
            }
            unbindService(mCameraHandlerConnection);
            mBoundCameraHandler = false;
        }
        unbindUploadService();
        appPrefs.saveIntPreference(PreferenceTypes.K_RESTART_COUNTER, 0);
        if (!mRecorder.isRecording()) {
            EventBus.postSticky(new ObdCommand(false));
            appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
            Log.d(TAG, "onStop: K_CRASHED is set to false");
        }
        mScreenComposer.unregister();
        EventBus.unregister(this);
        if (Fabric.isInitialized()) {
            Crashlytics.setInt(Log.CURRENT_SCREEN, -1);//means background
        }
        unregisterReceiver(mNetworkBroadcastReceiver);
        super.onStop();
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
        unbindUploadService();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mScreenComposer.onWindowFocusChanged(hasFocus);
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
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public OSVApplication getApp() {
        return (OSVApplication) getApplication();
    }

    public int getCurrentScreen() {
        return mScreenComposer.getCurrentScreen();
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
                if (mCameraHandlerService != null && getCurrentScreen() == ScreenComposer.SCREEN_RECORDING && mRecorder.hasPosition()) {
                    mRecorder.startRecording();
                }
            } else if (requestCode == REQUEST_CODE_GPS) {
                EventBus.postSticky(new GpsCommand(true));
                mMainThreadHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        EventBus.post(new PositionerEvent());
                    }
                }, 700);
            }
        } else {
            Log.d(TAG, "onActivityResult: result is error for " + requestCode);
            //DO NOTHING
            showSnackBar("Please enable your GPS, select \"High accuracy\" in the location settings.", Snackbar.LENGTH_LONG, "Enable",
                    new Runnable() {

                        @Override
                        public void run() {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
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
        mUploadManager = getApp().getUploadManager();
        enableProgressBar(false);

        if (getIntent().getBooleanExtra(K_OPEN_CAMERA, false)) {//go directly to camera view, removing any fragments over the pager
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    openScreen(ScreenComposer.SCREEN_RECORDING);
                }
            });
        }

        if (getIntent() != null && getIntent().getBooleanExtra(SplashActivity.RESTART_FLAG, false)) {
            getIntent().removeExtra(SplashActivity.RESTART_FLAG);
            showSnackBar(R.string.app_restarted, Snackbar.LENGTH_LONG);
        }
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false)) {
            if (mSafeModeDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mSafeModeDialog =
                        builder.setMessage(R.string.warning_problems_recording).setTitle(R.string.jpeg_recording_label).setCancelable(false)
                                .setNegativeButton(R.string.disable_label, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false);
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false);
                                    }
                                }).setPositiveButton(R.string.enable_label, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false);
                            }
                        }).create();
            }
            if (!mSafeModeDialog.isShowing()) {
                mSafeModeDialog.show();
            }
        }
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_SHOW_CLEAR_RECENTS_WARNING, false)) {
            if (mClearRecentsDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mClearRecentsDialog = builder.setMessage(R.string.warning_swipe_recents_text).setTitle(R.string.warning_label).setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_CLEAR_RECENTS_WARNING, false);
                            }
                        }).create();
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
                mMainThreadHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (getCurrentScreen() == ScreenComposer.SCREEN_RECORDING) {
                            EventBus.post(new ShutterPressEvent());
                        }
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
                            mMainThreadHandler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    EventBus.postSticky(new CameraPermissionEvent());
                                }
                            }, 1000);
                            break;
                        }
                    }
                    i++;
                }
                openScreen(ScreenComposer.SCREEN_MAP);
                break;
            case OSVApplication.LOCATION_PERMISSION:
                i = 0;
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResults.length > i && grantResults[i] >= 0) {
                            EventBus.postSticky(new GpsCommand(true));
                            mMainThreadHandler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    EventBus.post(new PositionerEvent());
                                }
                            }, 500);
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
                            mMainThreadHandler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    EventBus.post(new ObdPressedEvent());
                                }
                            }, 500);
                            return;
                        }
                    }
                    i++;
                }
                break;
        }
    }

    @Override
    public void onNetworkConnectionChange(boolean internetAvailable) {
        Log.d(TAG, "Network event.Connected:" + internetAvailable);

        boolean previouslyHadEnoughData = isEnoughDataToDisplayPayRateIndicator();
        mInternetAvailable = internetAvailable;
        boolean currentlyHasEnoughData = isEnoughDataToDisplayPayRateIndicator();
        updatePayRateIndicatorState(previouslyHadEnoughData, currentlyHasEnoughData);

        if (internetAvailable && loggedInUserType == PreferenceTypes.USER_TYPE_BYOD) {
            boolean byod20 = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10)
                    .equals(ProfileFragment.PAYMENT_MODEL_VERSION_20);
            if (byod20 && !mRecorder.isPayRateDataAvailable()) {
                Log.d(TAG, "Byod data was not available until now, fetch it now.");
                fetchPayRateData();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onByodPayRateUpdated(ByodDriverPayRateUpdatedEvent payRateEvent) {
        Log.d(TAG, "onByodPayrateUpdated! payrate:" + payRateEvent.payRate);
        if ((int) payRateEvent.payRate != (int) ScoreManager.PAY_RATE_UNKNOWN && byodPaymentIndicator != null && mRecording) {
            String payRateToDisplay = payRateEvent.currency + Float.toString(payRateEvent.payRate);
            if (!byodPaymentIndicator.isActive()) {
                byodPaymentIndicator.showTextWithInitialValue(payRateToDisplay);
            } else {
                byodPaymentIndicator.updateScore(payRateToDisplay);
            }
        }
    }

    public void goToRecordingScreen() {
        openScreen(ScreenComposer.SCREEN_RECORDING);
        ensureScoreTextCorrectState();
        fetchPayRateData();
    }

    public void bindUploadService(OSVServiceConnection osvServiceConnection) {
        if (!mBoundUploadHandler) {
            mUploadHandlerConnection = osvServiceConnection;
            Intent service = new Intent(getApplicationContext(), UploadHandlerService.class);
            mBoundUploadHandler = bindService(service, osvServiceConnection, BIND_AUTO_CREATE);
        }
    }

    public void unbindUploadService() {
        if (mBoundUploadHandler) {
            mUploadHandlerService.stopSelf();
            unbindService(mUploadHandlerConnection);
            mBoundUploadHandler = false;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUserTypeChanged(UserTypeChangedEvent event) {
        if (event.isDriver()) {
            boolean mapWasDisabled = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false);
            boolean miniMap = appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, true);
            boolean gamification = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
            boolean signDetection = appPrefs.getBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED, false);

            boolean needed = !mapWasDisabled || miniMap || gamification || signDetection;
            if (mDriverModeDialog == null && needed) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mDriverModeDialog =
                        builder.setMessage(R.string.warning_optimization_message).setTitle(R.string.warning_optimization_label).setCancelable(false)
                                .setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        boolean mapWasDisabled = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false);
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_DISABLED, true);
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, false);
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_GAMIFICATION, false);
                                        EventBus.post(new GamificationSettingEvent(false));
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED, false);
                                        if (!mapWasDisabled) {
                                            EventBus.postSticky(new SdkEnabledEvent(false));
                                            Intent mStartActivity = new Intent(MainActivity.this, SplashActivity.class);
                                            int mPendingIntentId = 123456;
                                            PendingIntent mPendingIntent =
                                                    PendingIntent.getActivity(MainActivity.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                                            AlarmManager mgr = (AlarmManager) MainActivity.this.getSystemService(Context.ALARM_SERVICE);
                                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
                                            android.os.Process.killProcess(android.os.Process.myPid());
                                            System.exit(0);
                                        }
                                    }
                                }).create();
            }
            if (needed && !mDriverModeDialog.isShowing() && !appPrefs.getBooleanPreference(PreferenceTypes.K_DRIVER_MODE_DIALOG_SHOWN)) {
                mDriverModeDialog.show();
                appPrefs.saveBooleanPreference(PreferenceTypes.K_DRIVER_MODE_DIALOG_SHOWN, true);
                if (Fabric.isInitialized()) {
                    Answers.getInstance().logCustom(new CustomEvent("Drivermode dialog displayed").putCustomAttribute(PreferenceTypes.K_USER_NAME,
                            appPrefs.getStringPreference(
                                    PreferenceTypes
                                            .K_USER_NAME)));
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoginChanged(LoginChangedEvent event) {
        if (event.logged) {
            showSnackBar("Logged in as " + appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME), Snackbar.LENGTH_SHORT);
            loggedInUserType = event.accountData.getUserType();
            mRecorder.configureForUser(event.accountData);
        } else {
            loggedInUserType = PreferenceTypes.USER_TYPE_UNKNOWN;
            mRecorder.configureForLoggedOutUser();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRecordingStatusChanged(final RecordingEvent event) {
        mRecording = event.started;
        final boolean started = event.started;
        boolean gamification = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
        boolean byod20 = (appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE) == PreferenceTypes.USER_TYPE_BYOD) &&
                profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10).equals(ProfileFragment
                        .PAYMENT_MODEL_VERSION_20);

        if (gamification || byod20) {
            if (byod20 && byodPaymentIndicator != null) {
                if (event.started) {
                    if (NetworkUtils.isInternetAvailable(this) || mRecorder.isPayRateDataAvailable()) {
                        if (!mRecorder.isPayRateDataAvailable()) {
                            fetchPayRateData();
                        }
                    } else {
                        //byod 2.0...no internet. set score view active, set it as having no coverage...
                        byodPaymentIndicator.showDrawable(R.drawable.vector_no_wifi);
                    }

                } else {
                    byodPaymentIndicator.hide();
                }
            } else if (scoreIndicator != null) {/* always gamification */
                scoreIndicator.setScore(0);
                scoreIndicator.setActive(event.started);
                scoreIndicator.setObdConnected(ObdManager.isConnected());
            }
        }
        if (!started && !appPrefs.getBooleanPreference(PreferenceTypes.K_HIDE_RECORDING_SUMMARY)) {
            if (event.sequence != null && event.sequence.getScore() >= 10) {
                openScreen(ScreenComposer.SCREEN_SUMMARY, event.sequence);
                event.sequence = null;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onScoreChanged(ScoreChangedEvent event) {
        Log.d(TAG, "onScoreChanged: " + event.score);
        if (scoreIndicator != null) {
            scoreIndicator.setCoverageAvailable(event.multiplier > 0);
            scoreIndicator.setScore((int) event.score);
            scoreIndicator.setMultiplier(event.multiplier);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCoverageChanged(CoverageEvent event) {
        Log.d(TAG, "onCoverageChanged: available = " + event.available);
        boolean previouslyHadEnoughData = isEnoughDataToDisplayPayRateIndicator();
        mCoverageAvailable = event.available;
        boolean currentlyHasEnoughData = isEnoughDataToDisplayPayRateIndicator();

        if (scoreIndicator != null) {
            scoreIndicator.setCoverageAvailable(event.available);
        } else if (byodPaymentIndicator != null) {
            updatePayRateIndicatorState(previouslyHadEnoughData, currentlyHasEnoughData);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFrameQueueChanged(FrameQueueEvent event) {
        if (Utils.DEBUG) {
            if (event.queueSize > 3) {
                showSnackBar("Frames in queue: " + event.queueSize, Snackbar.LENGTH_INDEFINITE);
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

    @SuppressWarnings("deprecation")
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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onSpeedCategoryChanged(SpeedCategoryEvent event) {
        if (Utils.isDebugEnabled(this) && appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST) && distanceDebugText != null &&
                speedDebugText != null) {
            distanceDebugText.setVisibility(View.VISIBLE);
            speedDebugText.setVisibility(View.VISIBLE);
            distanceDebugText.setText(event.category.toString());
            speedDebugText.setText(Utils.formatDistanceFromKiloMeters(this, event.speed)[0]);
        } else {
            if (distanceDebugText != null && speedDebugText != null) {
                distanceDebugText.setVisibility(View.GONE);
                speedDebugText.setVisibility(View.GONE);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onObdStatusEvent(ObdStatusEvent event) {
        switch (event.type) {
            case ObdStatusEvent.TYPE_CONNECTED:
                if (scoreIndicator != null) {
                    scoreIndicator.setObdConnected(true);
                }
                if (mOBDIcon != null) {
                    mOBDIconHolder.setVisibility(View.VISIBLE);
                    mOBDIcon.setText("0");
                    mOBDUnit.setText(Utils.formatSpeedFromKmph(this, 0)[1]);
                    mOBDIcon.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    EventBus.post(new ObdResetCommand());
                                }
                            }).start();
                        }
                    });
                }
                break;
            case ObdStatusEvent.TYPE_CONNECTING:
                if (mOBDIcon != null) {
                    mOBDIconHolder.setVisibility(View.VISIBLE);
                    mOBDIcon.setText("-");
                    mOBDUnit.setText(null);
                    mOBDIcon.setOnClickListener(null);
                }
                break;
            case ObdStatusEvent.TYPE_DISCONNECTED:
                if (scoreIndicator != null) {
                    scoreIndicator.setObdConnected(false);
                }
                if (mOBDIconHolder != null) {
                    mOBDIconHolder.setVisibility(View.GONE);
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onObdSpeed(ObdSpeedEvent event) {
        if (mOBDIcon != null) {
            if (event.data.getSpeed() != -1) {
                String[] speed = Utils.formatSpeedFromKmph(this, event.data.getSpeed());
                mOBDIcon.setText(speed[0]);
                mOBDUnit.setText(speed[1]);
            } else {
                mOBDIcon.setText("-");
            }
        }
    }

    private void updatePayRateIndicatorState(boolean previouslyHadEnoughData, boolean currentlyHasEnoughData) {
        Log.d(TAG, "#updatePayRateIndicatorState. previouslyHadEnoughData: " + previouslyHadEnoughData + " currentlyHasEnoughData: " + currentlyHasEnoughData);

        if (previouslyHadEnoughData && !currentlyHasEnoughData && byodPaymentIndicator != null) {
            //had enough data before, but not anymore. display the no data symbol
            byodPaymentIndicator.showDrawable(R.drawable.vector_no_wifi);
        }

        //no change since previous situation, do nothing
    }

    private boolean isEnoughDataToDisplayPayRateIndicator() {
        return mCoverageAvailable && (mInternetAvailable || mRecorder.isPayRateDataAvailable());
    }

    private void fetchPayRateData() {
        Log.d(TAG, "#fetchPayRateData");
        BackgroundThreadPool.post(() -> {
            int userType = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, PreferenceTypes.USER_TYPE_UNKNOWN);
            if (userType == PreferenceTypes.USER_TYPE_BYOD) {
                String paymentModel = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10);
                boolean paymentModel20 = Objects.equals(paymentModel, ProfileFragment.PAYMENT_MODEL_VERSION_20);

                if (paymentModel20) {

                    //if internet is available, fetch payrate data...
                    mUserDataManager.getDriverPayRateDetails(new NetworkResponseDataListener<PayRateData>() {
                        @Override
                        public void requestFailed(int status, PayRateData details) {
                            Log.d(TAG, "Failed to fetch pay rate data from the backend. Do nothing.");
                        }

                        @Override
                        public void requestFinished(int status, PayRateData details) {
                            Log.d(TAG, "Success fetch pay rate data ");
                            mMainThreadHandler.post(() -> {
                                mRecorder.setByodPayRateData(details);
                            });
                        }
                    });
                } //else byod 1.0 we don't care
            }
        });
    }

    /**
     * This ensures that the correct blue indicator is displayed in the recording screen.
     * <p>
     * For the BYOD 2.0 user, this means the indicator which displays the payrate/km, for the given road segment. For BYOD1.0, the indicator is not
     * displayed.
     * <p>
     * For the normal user (i.e. no BYOD), a score estimate of the points to be received for the current track will be displayed.
     */
    private void ensureScoreTextCorrectState() {
        if (loggedInUserType == PreferenceTypes.USER_TYPE_BYOD) {
            String paymentModel = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10);
            boolean isByod20 = Objects.equals(ProfileFragment.PAYMENT_MODEL_VERSION_20, paymentModel);
            if (isByod20) {
                if (byodPaymentIndicator == null) {
                    byodPaymentIndicator = findViewById(R.id.byod_score_text);
                }
                byodPaymentIndicator.setVisibility(View.VISIBLE);
                byodPaymentIndicator.setScoreSuffix(ByodPaymentIndicator.ScoreSuffix.PER_KM);
                byodPaymentIndicator.setDisplayBubble(false);
                byodPaymentIndicator.setDefaultValue(getString(R.string.default_value_payrate_byod20_while_recording));
                byodPaymentIndicator.setOnClickListener((v) -> {
                    showSnackBar(getString(R.string.byod20_pay_rate_while_recording_snackbar_text), Snackbar.LENGTH_LONG);
                });
            } else {
                if (byodPaymentIndicator != null) {
                    byodPaymentIndicator.setVisibility(View.GONE);
                }
            }
            if (scoreIndicator != null) {
                scoreIndicator.setVisibility(View.GONE);
                scoreIndicator.setActive(false);
                scoreIndicator = null;
            }
        } else {
            if (byodPaymentIndicator != null) {
                byodPaymentIndicator.setVisibility(View.GONE);
                byodPaymentIndicator = null;
            }
            if (scoreIndicator == null) {
                scoreIndicator = findViewById(R.id.score_text);
            }
            if (appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION)) {
                scoreIndicator.setOnClickListener((v) -> {
                    showSnackBar(getString(R.string.gamification_points_while_recording_snackbar_text), Snackbar.LENGTH_LONG);
                });
                scoreIndicator.reset();
                scoreIndicator.setVisibility(View.VISIBLE);
            } else {
                scoreIndicator.reset();
                scoreIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void checkUserInfo() {
        boolean logged = !appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN).equals("");
        if (logged) {
            final int userType = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
            if (userType == -1) {
                if (mUserDataManager != null) {
                    mUserDataManager.getUserProfileDetails(new NetworkResponseDataListener<UserData>() {

                        @Override
                        public void requestFailed(int status, UserData details) {
                            Log.d(TAG, "checkUserInfo: " + status + ", details: " + details);
                        }

                        @Override
                        public void requestFinished(int status, UserData userdata) {
                            Log.d(TAG, "checkUserInfo: " + " status - > " + status + " result - > " + userdata);
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
            } else if (userType == PreferenceTypes.USER_TYPE_BYOD || userType == PreferenceTypes.USER_TYPE_BAU ||
                    userType == PreferenceTypes.USER_TYPE_DEDICATED) {
                EventBus.postSticky(new UserTypeChangedEvent(userType));
            }
        }
    }

    private void tryToConnectOBD() {
        EventBus.postSticky(new ObdCommand(true));
    }

    public abstract class OSVServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            UploadHandlerService.UploadHandlerBinder binder = (UploadHandlerService.UploadHandlerBinder) service;
            mUploadHandlerService = binder.getService();
            mBoundUploadHandler = true;
            if (mBoundCameraHandler) {
                enableProgressBar(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mUploadHandlerService = null;
            mBoundUploadHandler = false;
        }
    }

//    @Override
//    public void onSignDetected(SignType.enSignType type) {
//        Glide.with(activity).load("file:///android_asset/" + type.getFile()).into(signDetectionHolder);
//
//        Animation signAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
//        signDetectionHolder.startAnimation(signAnimation);
//        signDetectionHolder.setVisibility(View.VISIBLE);
//        mMainThreadHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Animation signAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
//
//                signDetectionHolder.startAnimation(signAnimation);
//                signDetectionHolder.setVisibility(View.GONE);
//            }
//        }, 5000);
//    }
}
