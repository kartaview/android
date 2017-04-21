package com.telenav.osv.activity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.AccuracyEvent;
import com.telenav.osv.event.hardware.gps.SpeedCategoryEvent;
import com.telenav.osv.event.hardware.obd.ObdSpeedEvent;
import com.telenav.osv.event.hardware.obd.ObdStatusEvent;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.network.matcher.CoverageEvent;
import com.telenav.osv.event.network.matcher.ScoreChangedEvent;
import com.telenav.osv.event.ui.ObdPressedEvent;
import com.telenav.osv.event.ui.OrientationChangedEvent;
import com.telenav.osv.event.ui.PositionerEvent;
import com.telenav.osv.event.ui.ShutterPressEvent;
import com.telenav.osv.http.RequestListener;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.obd.ObdManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.service.UploadHandlerService;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.ui.custom.ScoreView;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * Activity displaying the map - camera view - profile screen
 */

public class MainActivity extends OSVActivity {

    /**
     * Intent extra used when opening app from recording notification
     * to go directly to camera view
     */
    public static final String K_OPEN_CAMERA = "open_camera";

    private static final String TAG = "MainActivity";

    public static final com.bumptech.glide.request.RequestListener<String, GlideDrawable> mGlideRequestListener = new com.bumptech.glide.request.RequestListener<String,
            GlideDrawable>() {
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
        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
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

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Status locationResolution;

    private Recorder mRecorder;

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
            if (Sequence.getStaticSequences().values().isEmpty()) {
                showSnackBar(R.string.no_local_recordings_message, Snackbar.LENGTH_SHORT);
                return;
            }
            if (!NetworkUtils.isWifiInternetAvailable(MainActivity.this) && !appPrefs.getBooleanPreference(PreferenceTypes
                    .K_UPLOAD_DATA_ENABLED)) {
                showSnackBar(R.string.no_wifi_label, Snackbar.LENGTH_SHORT);

                return;
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
            builder.setMessage(getString(R.string.upload_all_warning)).setTitle(getString(R.string.upload_all_warning_title)).setNegativeButton(R.string
                            .cancel_label,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setPositiveButton(R.string.upload_all_label, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mUploadHandlerService != null && !mRecorder.isRecording()) {
                        mUploadManager.uploadCache(new RequestListener() {
                            @Override
                            public void requestFinished(final int status) {
                                if (status == STATUS_FAILED) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                        }
                                    });
                                }
                            }
                        }, Sequence.getStaticSequences().values());
                        openScreen(ScreenComposer.SCREEN_UPLOAD_PROGRESS);
                    }
                }
            }).create().show();
        }
    };

    private TextView distanceDebugText;

    private TextView speedDebugText;

    private TextView mOBDIcon;

    private FrameLayout mOBDIconHolder;

    private TextView mOBDUnit;

    //    private ImageView signDetectionHolder;

    private ImageView mGPSIcon;

    private ScoreView scoreText;

    private AlertDialog mSafeModeDialog;

    private AlertDialog mClearRecentsDialog;

    private AlertDialog mDriverModeDialog;

    private ScreenComposer mScreenComposer;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mUploadHandlerConnection = new ServiceConnection() {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecorder = getApp().getRecorder();
        OSVApplication.sUiThreadId = Thread.currentThread().getId();
        mUploadManager = getApp().getUploadManager();

        mScreenComposer = new ScreenComposer(this);
        mOBDIcon = (TextView) findViewById(R.id.obd_icon);
        mGPSIcon = (ImageView) findViewById(R.id.gps_icon);
        scoreText = (ScoreView) findViewById(R.id.score_text);
        mOBDIconHolder = (FrameLayout) findViewById(R.id.obd_icon_holder);
        mOBDUnit = (TextView) findViewById(R.id.obd_icon_unit);
//        signDetectionHolder = (ImageView) findViewById(R.id.sign_detection_container);
        distanceDebugText = (TextView) findViewById(R.id.debug_distance_text);
        speedDebugText = (TextView) findViewById(R.id.debug_speed_text);

        if (appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, false)) {
            distanceDebugText.setVisibility(View.VISIBLE);
            speedDebugText.setVisibility(View.VISIBLE);
        }
//        boolean gamification = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
        scoreText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSnackBar("You will get more points when the streets you drive have less coverage!", Snackbar.LENGTH_LONG);
            }
        });
        if (!mBoundCameraHandler || !mBoundUploadHandler) {
            enableProgressBar(true);
        }
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

        if (!mBoundUploadHandler) {
            Intent service = new Intent(getApplicationContext(), UploadHandlerService.class);
            bindService(service, mUploadHandlerConnection, BIND_AUTO_CREATE);
        }
        String userType = appPrefs.getStringPreference(PreferenceTypes.K_USER_TYPE);
        if (!appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN).equals("") && (userType.equals("") || userType.equals(PreferenceTypes.V_USER_TYPE_DRIVER))) {
            if (userType.equals(PreferenceTypes.V_USER_TYPE_DRIVER)) {
                EventBus.postSticky(new LoginChangedEvent(true, appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME), appPrefs.getStringPreference(PreferenceTypes
                        .K_DISPLAY_NAME), appPrefs.getStringPreference(PreferenceTypes
                        .K_USER_PHOTO_URL), true));
            } else {
                mUploadManager.getProfileDetails(new RequestResponseListener() {
                    @Override
                    public void requestFinished(int status, String result) {
                        Log.d(TAG, "getProfileDetails: " + " status - > " + status + " result - > " + result);
                        if (result != null && !result.isEmpty() && status == RequestResponseListener.STATUS_SUCCESS_PROFILE_DETAILS) {
                            final String userName, name, obdDistance, totalDistance, totalPhotos, totalTracks;
                            int rank = 0, score = 0, level = 0, xpProgress = 0, xpTarget = 0;
                            boolean isDriver = false;
                            try {
                                JSONObject obj = new JSONObject(result);
                                JSONObject osv = obj.getJSONObject("osv");
                                userName = osv.getString("username");
                                name = osv.getString("full_name");
                                String userType = osv.getString("type");
                                appPrefs.saveStringPreference(PreferenceTypes.K_USER_TYPE, userType);
                                if (userType.equals("driver")) {
                                    isDriver = true;
                                }
                                obdDistance = osv.getString("obdDistance");
                                totalDistance = osv.getString("totalDistance");
                                totalPhotos = Utils.formatNumber(osv.getDouble("totalPhotos"));
                                totalTracks = Utils.formatNumber(osv.getDouble("totalTracks"));
                                try {
                                    JSONObject gamification = osv.getJSONObject("gamification");

                                    score = gamification.getInt("total_user_points");
                                    level = gamification.getInt("level");
                                    xpProgress = gamification.getInt("level_progress");
                                    try {
                                        xpTarget = gamification.getInt("level_target");
                                    } catch (Exception e) {
                                        Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                                    }
                                    rank = gamification.getInt("rank");

                                } catch (Exception e) {
                                    Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                                }

                                SharedPreferences prefs = getSharedPreferences(ProfileFragment.PREFS_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                prefsEditor.putInt(ProfileFragment.K_RANK, rank);
                                prefsEditor.putInt(ProfileFragment.K_SCORE, score);
                                prefsEditor.putInt(ProfileFragment.K_LEVEL, level);
                                prefsEditor.putInt(ProfileFragment.K_XP_PROGRESS, xpProgress);
                                prefsEditor.putInt(ProfileFragment.K_XP_TARGET, xpTarget);
                                prefsEditor.putString(ProfileFragment.K_TOTAL_PHOTOS, totalPhotos);
                                prefsEditor.putString(ProfileFragment.K_TOTAL_TRACKS, totalTracks);
                                prefsEditor.putString(ProfileFragment.K_TOTAL_DISTANCE, totalDistance);
                                prefsEditor.putString(ProfileFragment.K_OBD_DISTANCE, obdDistance);
                                prefsEditor.apply();
                                EventBus.postSticky(new LoginChangedEvent(true, userName, name, appPrefs.getStringPreference(PreferenceTypes.K_USER_PHOTO_URL), isDriver));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void requestFinished(int status) {

                    }
                });
            }
        }
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
        if (mBoundUploadHandler) {
            mUploadHandlerService.stopSelf();
            unbindService(mUploadHandlerConnection);
            mBoundUploadHandler = false;
        }
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
        super.onStop();
    }

    private void tryToConnectOBD() {
        EventBus.postSticky(new ObdCommand(true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        Utils.checkGooglePlaySevices(this);
        mUploadManager = getApp().getUploadManager();
        enableProgressBar(false);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//set status bar color
//            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(getResources().getColor(R.color.md_grey_300));
//        }

        if (getIntent().getBooleanExtra(K_OPEN_CAMERA, false)) {//go directly to camera view, removing any fragments over the pager
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openScreen(ScreenComposer.SCREEN_RECORDING);
                }
            });
        }
//        int orientation = getResources().getConfiguration().orientation;
//        if (cameraPreviewFragment != null) {
//            cameraPreviewFragment.onOrientationChanged(orientation == Configuration.ORIENTATION_PORTRAIT);
//        }
//        int runCounter = appPrefs.getIntPreference(PreferenceTypes.K_RUN_COUNTER);
//        if (runCounter <= 4) {
//            if (runCounter == 4) {
//                Instabug.showIntroMessage();
//                runCounter++;
//            }
//            runCounter++;
//            appPrefs.saveIntPreference(PreferenceTypes.K_RUN_COUNTER, runCounter);
//        }

//        sFragmentOverlayTag = "";

        if (getIntent() != null && getIntent().getBooleanExtra(SplashActivity.RESTART_FLAG, false)) {
            getIntent().removeExtra(SplashActivity.RESTART_FLAG);
            showSnackBar(R.string.app_restarted, Snackbar.LENGTH_LONG);
        }
        if (appPrefs.getBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false)) {
            if (mSafeModeDialog == null) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                mSafeModeDialog = builder.setMessage(R.string.warning_problems_recording)
                        .setTitle(R.string.jpeg_recording_label)
                        .setCancelable(false).setNegativeButton(R.string.disable_label,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SAFE_MODE_ENABLED, false);
                                        appPrefs.saveBooleanPreference(PreferenceTypes.K_SHOW_SAFE_MODE_MESSAGE, false);
                                    }
                                }).setPositiveButton(R.string.enable_label,
                                new DialogInterface.OnClickListener() {
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
                mClearRecentsDialog = builder.setMessage(R.string.warning_swipe_recents_text).setTitle(R.string.warning_label)
                        .setCancelable(false).setPositiveButton("ok",
                                new DialogInterface.OnClickListener() {
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

//        mUploadManager.version(new RequestResponseListener() {
//            @Override
//            public void requestFinished(int status, String result) {
//                Log.d(TAG, "version: " + result);
//
//                PackageInfo pInfo;
//                int version = 1000;
//                try {
//                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//                    version = pInfo.versionCode;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//                try {
//                    JSONObject obj = new JSONObject(result);
//                    String ver = obj.getString("version");
//                    if (Double.parseDouble(ver) > version) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                showDialogUpdateVersion();
//                            }
//                        });
//
//                    }
//                } catch (Exception e) {
//                    Log.d(TAG, "requestFinished: " + Log.getStackTraceString(e));
//                }
//            }
//
//            @Override
//            public void requestFinished(int status) {
//                Log.d(TAG, "version: status " + status);
//            }
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBoundCameraHandler) {
            mCameraHandlerService.stopSelf();
            unbindService(mCameraHandlerConnection);
            mBoundCameraHandler = false;
        }
        if (mBoundUploadHandler) {
            mUploadHandlerService.stopSelf();
            unbindService(mUploadHandlerConnection);
            mBoundUploadHandler = false;
        }
    }

    @Override
    public void onBackPressed() {
        cancelNearby();
        mScreenComposer.onBackPressed();
    }

    public void cancelNearby() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                enableProgressBar(false);
                mUploadManager.cancelNearby();
            }
        });
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoginChanged(LoginChangedEvent event) {
        if (event.logged) {
            showSnackBar("Logged in as " + appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME), Snackbar.LENGTH_SHORT);
            if (event.driver) {
                boolean mapWasDisabled = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false);
                boolean miniMap = appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, true);
                boolean gamification = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
                boolean signDetection = appPrefs.getBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED, false);

                boolean needed = !mapWasDisabled || miniMap || gamification || signDetection;
                if (mDriverModeDialog == null && needed) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
                    mDriverModeDialog = builder.setMessage(R.string.warning_optimization_message)
                            .setTitle(R.string.warning_optimization_label)
                            .setCancelable(false).setPositiveButton(R.string.ok_label,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            boolean mapWasDisabled = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED, false);
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_DISABLED, true);
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, false);
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_GAMIFICATION, false);
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED, false);
                                            if (!mapWasDisabled) {
                                                EventBus.postSticky(new SdkEnabledEvent(false));
                                                Intent mStartActivity = new Intent(MainActivity.this, SplashActivity.class);
                                                int mPendingIntentId = 123456;
                                                PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId, mStartActivity, PendingIntent
                                                        .FLAG_CANCEL_CURRENT);
                                                AlarmManager mgr = (AlarmManager) MainActivity.this.getSystemService(Context.ALARM_SERVICE);
                                                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                                                appPrefs.saveBooleanPreference(PreferenceTypes.K_CRASHED, false);
                                                android.os.Process.killProcess(android.os.Process.myPid());
                                                System.exit(0);
                                            }
                                        }
                                    }).create();
                }
                if (needed && !mDriverModeDialog.isShowing()) {
                    mDriverModeDialog.show();
                    if (Fabric.isInitialized()) {
                        Answers.getInstance().logCustom(new CustomEvent("Drivermode dialog displayed").putCustomAttribute(PreferenceTypes.K_USER_NAME, appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME)));
                    }
                }
            }
        }
//        else {
//            showSnackBar(R.string.logged_out_confirmation, Snackbar.LENGTH_SHORT);
//        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mScreenComposer.onWindowFocusChanged(hasFocus);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRecordingStatusChanged(final RecordingEvent event) {
        final boolean started = event.started;
        if (scoreText != null) {
            scoreText.setActive(event.started);
            scoreText.setObdConnected(ObdManager.isConnected());
        }
        if (!started) {
            if (event.sequence != null && event.sequence.score >= 10) {
                openScreen(ScreenComposer.SCREEN_SUMMARY, event.sequence);
                event.sequence = null;
            }
        }
    }

    @Override
    public void showSnackBar(final CharSequence text, final int duration, final CharSequence button, final Runnable onClick) {
        mScreenComposer.showSnackBar(text, duration, button, onClick);
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


    @Subscribe(sticky = true)
    public void onResolutionNeeded(LocationPermissionEvent event) {
        this.locationResolution = event.status;
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
                locationResolution.startResolutionForResult(
                        this, code);
            } catch (IntentSender.SendIntentException e) {
                // Ignore the error.
            }
        }
    }

    @Subscribe(sticky = true)
    public void onCameraReady(final CameraInitEvent event) {
        if (event.type == CameraInitEvent.TYPE_READY) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    EventBus.post(new OrientationChangedEvent());
                }
            });
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
        float accuracy = event.accuracy;
        Log.d(TAG, "onAccuracyChanged: " + getAccuracyStatus(accuracy));
        if (mGPSIcon != null) {
            if (accuracy <= Recorder.ACCURACY_GOOD) {
                mGPSIcon.setImageDrawable(getResources().getDrawable(R.drawable.vector_gps_good));
            } else if (accuracy <= Recorder.ACCURACY_MEDIUM) {
                mGPSIcon.setImageDrawable(getResources().getDrawable(R.drawable.vector_gps_medium));
            } else {
                mGPSIcon.setImageDrawable(getResources().getDrawable(R.drawable.vector_gps_low));
            }
        }
    }

    private String getAccuracyStatus(float accuracy) {
        String textAccuracy;
        if (accuracy <= Recorder.ACCURACY_GOOD) {
            textAccuracy = getString(R.string.gps_ok_label);
        } else if (accuracy <= Recorder.ACCURACY_MEDIUM) {
            textAccuracy = getString(R.string.gps_medium_label);
        } else {
            textAccuracy = getString(R.string.gps_bad_label);
        }
        return textAccuracy;
    }

    @Subscribe(sticky = true)
    public void onSpeedCategoryChanged(SpeedCategoryEvent event) {
        Log.d(TAG, "onSpeedCategoryChanged: ");
        if (Utils.isDebugEnabled(this) && appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST) && distanceDebugText != null && speedDebugText != null) {
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

    @Subscribe(sticky = true)
    public void onObdStatusEvent(ObdStatusEvent event) {
        switch (event.type) {
            case ObdStatusEvent.TYPE_CONNECTED:
                if (scoreText != null) {
                    scoreText.setObdConnected(true);
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
                if (scoreText != null) {
                    scoreText.setObdConnected(false);
                }
                if (mOBDIconHolder != null) {
                    mOBDIconHolder.setVisibility(View.GONE);
                }
                break;
        }
    }

    @Subscribe
    public void onObdSpeed(ObdSpeedEvent event) {
        if (mOBDIcon != null && event.data.getSpeed() != -1) {
            mOBDIcon.setText(Utils.formatSpeedFromKmph(this, event.data.getSpeed())[0]);
        }
    }

    public void goToLeaderboard() {
        openScreen(ScreenComposer.SCREEN_LEADERBOARD);
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
                EventBus.post(new PositionerEvent());
            }
        } else {
            Log.d(TAG, "onActivityResult: result is error for " + requestCode);
            //DO NOTHING
            showSnackBar("Please enable your GPS, select \"High accuracy\" in the location settings.", Snackbar.LENGTH_LONG, "Enable", new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
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
                mHandler.postDelayed(new Runnable() {
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
                            mHandler.postDelayed(new Runnable() {
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
                            mHandler.postDelayed(new Runnable() {
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
                            mHandler.postDelayed(new Runnable() {
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


//    @Override
//    public void onSignDetected(SignType.enSignType type) {
//        Glide.with(activity).load("file:///android_asset/" + type.getFile()).into(signDetectionHolder);
//
//        Animation signAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
//        signDetectionHolder.startAnimation(signAnimation);
//        signDetectionHolder.setVisibility(View.VISIBLE);
//        mHandler.postDelayed(new Runnable() {
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
