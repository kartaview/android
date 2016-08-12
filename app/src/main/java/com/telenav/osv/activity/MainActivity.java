package com.telenav.osv.activity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.TimerTask;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.Target;
import com.facebook.network.connectionclass.ConnectionQuality;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.api.Status;
import com.skobbler.ngx.SKMaps;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.http.RequestListener;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.RecordingStateChangeListener;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.CameraManager;
import com.telenav.osv.manager.LocalPlaybackManager;
import com.telenav.osv.manager.LocationManager;
import com.telenav.osv.manager.OnlinePlaybackManager;
import com.telenav.osv.manager.PlaybackManager;
import com.telenav.osv.manager.ShutterManager;
import com.telenav.osv.manager.UploadManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.service.UploadHandlerService;
import com.telenav.osv.ui.fragment.CameraControlsFragment;
import com.telenav.osv.ui.fragment.CameraPreviewFragment;
import com.telenav.osv.ui.fragment.HintsFragment;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.NearbyFragment;
import com.telenav.osv.ui.fragment.OAuthDialogFragment;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.ui.fragment.SettingsFragment;
import com.telenav.osv.ui.fragment.TrackPreviewFragment;
import com.telenav.osv.ui.fragment.UploadProgressFragment;
import com.telenav.osv.ui.fragment.WaitingFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;

/**
 * Activity displaying the map - camera view - profile screen
 */

public class MainActivity extends AppCompatActivity implements RecordingStateChangeListener, UploadProgressListener, FragmentManager
        .OnBackStackChangedListener, View.OnClickListener, Camera.ShutterCallback {

    /**
     * Intent extra used when opening app from recording notification
     * to go directly to camera view
     */
    public static final String K_OPEN_CAMERA = "open_camera";

    public static final int SCREEN_MAP = 0;

    public static final int SCREEN_RECORDING = 1;

    public static final int SCREEN_MY_PROFILE = 2;

    public static final int SCREEN_SETTINGS = 3;

    public static final int SCREEN_PREVIEW = 4;

    public static final int SCREEN_UPLOAD_PROGRESS = 5;

    public static final int SCREEN_WAITING = 6;

    public static final int SCREEN_TRACK_DETAIL = 7;

    public static final int SCREEN_RECORDING_HINTS = 8;

    public static final int SCREEN_NEARBY = 9;

    private static final String TAG = "MainActivity";

    public static final com.bumptech.glide.request.RequestListener<String, GlideDrawable> mGlideRequestListener = new com.bumptech.glide.request.RequestListener<String,
            GlideDrawable>() {
        @Override
        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
            if (e != null) {
                Log.d(TAG, "Glide: " + e.getLocalizedMessage());
            } else {
                Log.d(TAG, "Glide: exception during image load, no details");
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

//    public static int sCurrentScreen = -1;
//
//    public static String sFragmentOverlayTag = "";
//
//    public static int sLastSequence = -1;
//
//    public static int sLastSequenceIndex = 0;

    /**
     * the map fragment object
     */
    public MapFragment mapFragment;

    public CameraHandlerService mCameraHandlerService;

    public UploadHandlerService mUploadHandlerService;

    public SettingsFragment settingsFragment;

    public ProfileFragment profileFragment;

    public UploadProgressFragment uploadProgressFragment;

    public View.OnClickListener actionUploadAllListener;

    public View.OnClickListener actionCancelListener;

    public View.OnClickListener detailsOnClickListener;

    public View.OnClickListener positionMeOnClickListener;

    public View.OnClickListener resumeOnClickListener;

    public View.OnClickListener pauseOnClickListener;

    public View.OnClickListener uploadSequenceOnClickListener;


    public ApplicationPreferences appPrefs;

    /**
     * The fragments used in the pager
     */
//    public List<Fragment> fragments = new ArrayList<>();

    public TrackPreviewFragment trackPreviewFragment;

    public WaitingFragment waitingFragment;

    public boolean mNeedsToExit = false;

    ActionBarDrawerToggle actionBarDrawerToggle;

    private CameraPreviewFragment cameraPreviewFragment;

    private boolean mBoundCameraHandler;

    private boolean mBoundUploadHandler;

    private TextView usernameTextView;

    private TextView signatureActionBarText;

    private ImageView logOutImage;

    private ProgressBar progressBar;

    private AlertDialog alertDialog;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mUploadHandlerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            UploadHandlerService.UploadHandlerBinder binder = (UploadHandlerService.UploadHandlerBinder) service;
            mUploadHandlerService = binder.getService();
            mUploadHandlerService.addUploadProgressListener(MainActivity.this);
            mBoundUploadHandler = true;

            if (settingsFragment != null) {
                settingsFragment.onUploadServiceConnected(mUploadHandlerService);
            }
            if (waitingFragment != null) {
                waitingFragment.onUploadServiceConnected(mUploadHandlerService);
            }
            if (uploadProgressFragment != null) {
                uploadProgressFragment.onUploadServiceConnected(mUploadHandlerService);
            }
            if (mBoundCameraHandler) {
                enableProgressBar(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mUploadHandlerService.removeUploadProgressListener(MainActivity.this);
            mUploadHandlerService = null;
            mBoundUploadHandler = false;

        }
    };

    private TimerTask animateDownTimerTask;

    private UploadManager mUploadManager;

    private Snackbar mSnackbar;

    private AppBarLayout mAppBar;

//    private Fragment mCurrentFragment;

    private Toolbar toolbar;

    private DrawerLayout drawerLayout;

    private boolean mScreenModeParalell = false;

    private FloatingActionButton recordButton;

    private ActionBar mActionBar;

    private boolean mMenuOpen = false;

    private NavigationView navigationView;

    private LinearLayout mFragmentsContainer;

    private FrameLayout mUpperFragmentHolder;

    private FrameLayout mLowerFragmentHolder;

    private FrameLayout mSmallFragmentHolder;

    private HintsFragment hintsFragment;

    private NearbyFragment nearbyFragment;

    private boolean mBackButtonShown = false;

    private CameraControlsFragment cameraControlsFragment;

    private Fragment smallFragment;

    private Fragment largeFragment;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Status locationResolution;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mCameraHandlerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            CameraHandlerService.CameraHandlerBinder binder = (CameraHandlerService.CameraHandlerBinder) service;
            mCameraHandlerService = binder.getService();
            mBoundCameraHandler = true;

            mCameraHandlerService.setRecordingListener(MainActivity.this);
            mCameraHandlerService.mShutterManager.setImageSavedListener(MainActivity.this);
            if (cameraPreviewFragment == null) {
                cameraPreviewFragment = new CameraPreviewFragment();
            }
            if (cameraControlsFragment != null) {
                cameraControlsFragment.onCameraServiceConnected(mCameraHandlerService);
            }
            cameraPreviewFragment.onCameraServiceConnected(mCameraHandlerService);
            if (mCameraHandlerService.mShutterManager.isRecording()) {
                openScreen(SCREEN_RECORDING);
            }
            if (mBoundUploadHandler) {
                enableProgressBar(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCameraHandlerService.removeRecordingListener();
            mCameraHandlerService = null;
            mBoundCameraHandler = false;
        }
    };

    private AlertDialog mExitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.initializeLibrary(this);
//        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OSVApplication.sUiThreadId = Thread.currentThread().getId();
        mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
        appPrefs = ((OSVApplication) getApplication()).getAppPrefs();
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.accent_material_dark_1), PorterDuff.Mode.SRC_IN);
        if (!mBoundCameraHandler || !mBoundUploadHandler) {
            enableProgressBar(true);
        }
        mAppBar = (AppBarLayout) findViewById(R.id.app_bar);
        toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        mFragmentsContainer = (LinearLayout) findViewById(R.id.fragments_container);
        mUpperFragmentHolder = (FrameLayout) findViewById(R.id.content_frame_upper);
        mLowerFragmentHolder = (FrameLayout) findViewById(R.id.content_frame_lower);
        signatureActionBarText = (TextView) toolbar.findViewById(R.id.signature_action_bar_text);
        mSmallFragmentHolder = (FrameLayout) findViewById(R.id.content_frame_small);
        setSupportActionBar(toolbar);
        initNavigationDrawer();
        initFragments();
        toolbar.setNavigationOnClickListener(this);
        mActionBar = getSupportActionBar();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (mapFragment != null && cameraPreviewFragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.addToBackStack(MapFragment.TAG);
            ft.add(R.id.content_frame_upper, mapFragment, MapFragment.TAG).commit();
        }
        positionSmallFragment(isPortrait());

        //FAB
        actionUploadAllListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);

                if (userName.equals("")) {
                    showSnackBar(R.string.login_to_upload_warning, Snackbar.LENGTH_LONG, "Login", new Runnable() {
                        @Override
                        public void run() {
                            showLogInScreen();
                        }
                    });

                    return;
                }
                if (!NetworkUtils.isInternetAvailable(MainActivity.this)) {
                    showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);

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
                        if (mUploadHandlerService != null) {
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
                            openScreen(SCREEN_UPLOAD_PROGRESS);
                        }
                    }
                }).create().show();
            }
        };
        actionCancelListener = new View.OnClickListener() {
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

        detailsOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeUpperFragments();
                openScreen(SCREEN_MY_PROFILE);
            }
        };

        resumeOnClickListener = new View.OnClickListener() {
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

        pauseOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUploadManager.pauseUpload();
            }
        };

        positionMeOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapFragment.onPositionerClicked();
            }
        };
        uploadSequenceOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapFragment.mCurrentSequence != null) {
                    String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);


                    if (userName.equals("")) {
                        showSnackBar(R.string.please_login_label, Snackbar.LENGTH_SHORT);

                        return;
                    }
                    if (!NetworkUtils.isInternetAvailable(MainActivity.this)) {
                        showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);

                        return;
                    }
                    if (!NetworkUtils.isWifiInternetAvailable(MainActivity.this) && !appPrefs.getBooleanPreference(PreferenceTypes
                            .K_UPLOAD_DATA_ENABLED)) {
                        showSnackBar(R.string.no_wifi_label, Snackbar.LENGTH_SHORT);
                        return;
                    }

                    final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
                    builder.setMessage(getString(R.string.upload_sequence_warning)).setTitle(getString(R.string.upload_sequence_warning_title)).setNegativeButton(R.string
                                    .cancel_label,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).setPositiveButton(R.string.upload_sequence_label, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mUploadHandlerService != null) {
                                ArrayList<Sequence> list = new ArrayList<Sequence>();
                                list.add(mapFragment.mCurrentSequence);
                                mUploadManager.uploadCache(new RequestListener() {
                                    @Override
                                    public void requestFinished(int status) {
                                        if (status == STATUS_FAILED) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                }
                                            });
                                        }
                                    }
                                }, list);
                                openScreen(SCREEN_MY_PROFILE);
                                removeUpperFragments();
                                showSnackBar(R.string.sequence_will_be_uploaded, Snackbar.LENGTH_SHORT);
                            }
                        }
                    }).create().show();
                }
            }
        };
        if (!isPortrait()) {
            startImmersiveMode();
        }
    }

    public void initFragments() {
//        fragments = new ArrayList<>(9);
        if (mapFragment == null) {
            mapFragment = new MapFragment();
        }
        smallFragment = mapFragment;
        if (cameraPreviewFragment == null) {
            cameraPreviewFragment = new CameraPreviewFragment();
        }
        largeFragment = cameraPreviewFragment;
        if (cameraControlsFragment == null) {
            cameraControlsFragment = new CameraControlsFragment();
        }
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
        }
        if (profileFragment == null) {
            profileFragment = new ProfileFragment();
        }
        if (uploadProgressFragment == null) {
            uploadProgressFragment = new UploadProgressFragment();
        }
        if (trackPreviewFragment == null) {
            trackPreviewFragment = new TrackPreviewFragment();
        }

        if (waitingFragment == null) {
            waitingFragment = new WaitingFragment();
        }
        if (hintsFragment == null) {
            hintsFragment = new HintsFragment();
        }
        if (nearbyFragment == null) {
            nearbyFragment = new NearbyFragment();
        }

//        fragments.add(mapFragment);
//        fragments.add(cameraPreviewFragment);
//        fragments.add(profileFragment);
//        fragments.add(settingsFragment);
//        fragments.add(uploadProgressFragment);
//        fragments.add(trackPreviewFragment);
//        fragments.add(waitingFragment);
//        fragments.add(hintsFragment);
//        fragments.add(nearbyFragment);
    }


    public void openScreen(int screen) {
        openScreen(screen, null);
    }

    public void openScreen(final int screen, final Object extra) {
        if (mUploadHandlerService == null || mCameraHandlerService == null) {
            showSnackBar("Loading components, just a minute...", Snackbar.LENGTH_SHORT);
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                enableProgressBar(false);
                Fragment fragment = null;
                String tag = "";

                if (mSnackbar != null && mSnackbar.isShown()) {
                    mSnackbar.dismiss();
                }
                boolean shouldPop = false;
                if (getCurrentFragment().equals(CameraControlsFragment.TAG) || getCurrentFragment().equals(HintsFragment.TAG)) {
                    removeFragment(mapFragment);
                    removeFragment(cameraPreviewFragment);
                }
                switch (screen) {
                    case SCREEN_MAP:
                        mapFragment.enterMode(MapFragment.MODE_IDLE);
                        if (mapFragment.isAdded()) {
                            if (!mCameraHandlerService.mShutterManager.isRecording()) {
                                removeUpperFragments();
                            }
                        } else {
                            removeUpperFragments();
                            try {
                                getSupportFragmentManager().popBackStackImmediate();
                            } catch (IllegalStateException e) {
                                Log.d(TAG, "error: popBackStackImmediate failed");
                            }
                            displayFragment(mapFragment, MapFragment.TAG);
                        }
                        return;
                    case SCREEN_RECORDING:
                        removeUpperFragments();
                        try {
                            getSupportFragmentManager().popBackStackImmediate();
                        } catch (IllegalStateException e) {
                            Log.d(TAG, "error: popBackStackImmediate failed");
                        }
                        Point point = new Point();
                        getWindowSize(point);
                        Log.d(TAG, "resizePreview: contentHeight = " + point.y + " contentWidth = " + point.x);
                        float ratio;
                        int previewHeight;
                        int previewWidth;
                        previewHeight = CameraManager.instance.previewHeight;
                        previewWidth = CameraManager.instance.previewWidth;
                        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
                        float large = Math.max(previewHeight, point.x);
                        float small = Math.min(previewHeight, point.x);
                        float finalRatio = small / large;

                        float length = finalRatio * previewHeight;
                        float ffratio = Math.min(length, point.y) / Math.max(length, point.y);

                        displayFragmentsParallel(1.0f - Math.max(0.75f, ffratio), null, cameraControlsFragment, CameraControlsFragment.TAG, CameraPreviewFragment.TAG);
                        addUpperFragment(cameraPreviewFragment, CameraPreviewFragment.TAG);
                        if (mCameraHandlerService.mShutterManager.isRecording()) {
                            addSmallFragment(mapFragment, MapFragment.TAG);
                            if (smallFragment == cameraPreviewFragment) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        cameraPreviewFragment.addCameraSurfaceView();
                                    }
                                }, 300);
                            }
                        }
                        return;
                    case SCREEN_MY_PROFILE:
                        tag = ProfileFragment.TAG;
                        fragment = profileFragment;
                        shouldPop = true;
                        break;
                    case SCREEN_SETTINGS:
                        tag = SettingsFragment.TAG;
                        fragment = settingsFragment;
                        shouldPop = true;
                        break;
                    case SCREEN_PREVIEW:
                        boolean fromMap = false;
                        if (getCurrentFragment().equals(NearbyFragment.TAG)) {
                            fromMap = true;
                        }
                        mapFragment.enterMode(MapFragment.MODE_PREVIEW);
                        removeUpperFragments();
                        try {
                            getSupportFragmentManager().popBackStackImmediate();
                        } catch (IllegalStateException e) {
                            Log.d(TAG, "error: popBackStackImmediate failed");
                        }
                        PlaybackManager player;
                        if (((Sequence) extra).online) {
                            player = new OnlinePlaybackManager(MainActivity.this, (Sequence) extra);
                        } else {
                            player = new LocalPlaybackManager((Sequence) extra);
                        }
                        mapFragment.setSource(player);
                        trackPreviewFragment.setSource(player);
                        trackPreviewFragment.hideDelete(fromMap);
                        displayFragmentsParallel(0.6f, mapFragment, trackPreviewFragment, TrackPreviewFragment.TAG, MapFragment.TAG);
                        return;
                    case SCREEN_WAITING:
                        tag = WaitingFragment.TAG;
                        fragment = waitingFragment;
                        shouldPop = true;
                        break;
                    case SCREEN_UPLOAD_PROGRESS:
                        tag = UploadProgressFragment.TAG;
                        fragment = uploadProgressFragment;
                        break;
                    case SCREEN_RECORDING_HINTS:
                        tag = HintsFragment.TAG;
                        fragment = hintsFragment;
                        break;
                    case SCREEN_NEARBY:
                        tag = NearbyFragment.TAG;
                        fragment = nearbyFragment;
                        nearbyFragment.handleNearbyResult((String) extra);
                        break;
                }
                if (getCurrentFragment().equals(tag)) {
                    return;
                }
                if (shouldPop) {
                    removeUpperFragments();
                }
                if (fragment != null) {
                    displayFragment(fragment, tag);
                }
            }
        });
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
        mScreenModeParalell = true;
    }

    private void displayFragment(Fragment fragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment.isAdded()) {
            ft.remove(fragment);
        }
        ft.addToBackStack(tag);
        ft.add(R.id.content_frame_upper, fragment, tag);
        resizeHolders(0, true);
        ft.commitAllowingStateLoss();
        mScreenModeParalell = false;
    }


    private void resizeHolders(float ratio, final boolean portrait) {
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
        boolean reorient = mFragmentsContainer.getOrientation() != intendedOrientation;
        if (reorient) {
//            mFragmentsContainer.removeAllViews();
            mFragmentsContainer.setOrientation(intendedOrientation);
//            mFragmentsContainer.addView(portrait ? mUpperFragmentHolder : mLowerFragmentHolder);
//            mFragmentsContainer.addView(portrait ? mLowerFragmentHolder : mUpperFragmentHolder);
        }
        mUpperFragmentHolder.setLayoutParams(lpu);
        mLowerFragmentHolder.setLayoutParams(lpl);
        mFragmentsContainer.invalidate();
//        if (reorient) {
        positionSmallFragment(portrait);
//        }
    }

    private void positionSmallFragment(final boolean portrait) {

        final FrameLayout.LayoutParams smallLp = new FrameLayout.LayoutParams(portrait ? (int) Utils.dpToPx(this, 110) : (int) Utils.dpToPx(this, 150)
                , portrait ? (int) Utils.dpToPx(this, 150) : (int) Utils.dpToPx(this, 110));
        mLowerFragmentHolder.post(new Runnable() {
            @Override
            public void run() {
                if (portrait) {
                    smallLp.bottomMargin = mLowerFragmentHolder.getHeight() + (int) Utils.dpToPx(MainActivity.this, 15);
                    smallLp.leftMargin = (int) Utils.dpToPx(MainActivity.this, 10);
                    smallLp.gravity = Gravity.BOTTOM | Gravity.START;
                } else {
                    smallLp.bottomMargin = (int) Utils.dpToPx(MainActivity.this, 10);
                    smallLp.rightMargin = mLowerFragmentHolder.getWidth() + (int) Utils.dpToPx(MainActivity.this, 15);
                    smallLp.gravity = Gravity.BOTTOM | Gravity.END;
                }
                mSmallFragmentHolder.setLayoutParams(smallLp);
                mSmallFragmentHolder.invalidate();
            }
        });
    }

    public void resizeHolderStatic(float ratio, final boolean portrait) {
        Log.d(TAG, "resizeHolders: ratio = " + ratio);
        LinearLayout.LayoutParams lpu = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
        lpu.weight = 1.0f - ratio;
        LinearLayout.LayoutParams lpl = new LinearLayout.LayoutParams(portrait ? LinearLayout.LayoutParams.MATCH_PARENT : 0, portrait ? 0 : LinearLayout.LayoutParams.MATCH_PARENT);
        lpl.weight = ratio;
        mUpperFragmentHolder.setLayoutParams(lpu);
        mLowerFragmentHolder.setLayoutParams(lpl);
        mFragmentsContainer.invalidate();
    }

    private void initNavigationDrawer() {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        toolbar.setNavigationOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                int id = menuItem.getItemId();
                boolean addMap = false;

                if (getCurrentFragment().equals(CameraControlsFragment.TAG)) {
                    addMap = true;
                }
                switch (id) {
                    case R.id.menu_settings:
                        if (addMap) {
                            openScreen(SCREEN_MAP);
                        }
                        openScreen(SCREEN_SETTINGS);
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.menu_upload:
                        if (Sequence.getLocalSequencesSize() <= 0) {
                            drawerLayout.closeDrawers();
                            showSnackBar("You have no local recordings.", Snackbar.LENGTH_LONG);
                            return true;
                        }
                        if (addMap) {
                            openScreen(SCREEN_MAP);
                        }
                        openScreen(SCREEN_WAITING);
                        drawerLayout.closeDrawers();
                        break;
                    case R.id.menu_profile:
                        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
                        if (userName.equals("")) {
                            showSnackBar(R.string.login_to_see_online_warning, Snackbar.LENGTH_LONG, "Login", new Runnable() {
                                @Override
                                public void run() {
                                    showLogInScreen();
                                }
                            });
                        } else {
                            if (addMap) {
                                openScreen(SCREEN_MAP);
                            }
                            openScreen(SCREEN_MY_PROFILE);
                        }
                        drawerLayout.closeDrawers();
                        break;
                }
                return true;
            }
        });

        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);

        View header = navigationView.getHeaderView(0);
        usernameTextView = (TextView) header.findViewById(R.id.username_label);
        usernameTextView.setText(userName);

        logOutImage = (ImageView) header.findViewById(R.id.log_out_image_button);
        logOutImage.setOnClickListener(this);

        if ((!appPrefs.getStringPreference(PreferenceTypes.K_USER_ID).equals("")) && (!appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME).equals(""))) {
            logOutImage.setVisibility(View.VISIBLE);
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
            }

            @Override
            public void onDrawerOpened(View v) {
                super.onDrawerOpened(v);
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBoundCameraHandler) {
            Intent service = new Intent(getApplicationContext(), CameraHandlerService.class);
            bindService(service, mCameraHandlerConnection, BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "Service not killed, connecting to camera fragment");
            if (cameraPreviewFragment != null && mCameraHandlerService != null) {
                cameraPreviewFragment.onCameraServiceConnected(mCameraHandlerService);
            } else {
                Log.d(TAG, "connection unsuccessful, service reference null ");
            }
        }

        if (!mBoundUploadHandler) {
            Intent service = new Intent(getApplicationContext(), UploadHandlerService.class);
            bindService(service, mUploadHandlerConnection, BIND_AUTO_CREATE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocationManager.checkGooglePlaySevices(this);
        mUploadManager = ((OSVApplication) getApplication()).getUploadManager();
        enableProgressBar(false);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//set status bar color
//            Window window = getWindow();
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(getResources().getColor(R.color.md_grey_300));
//        }

        if (getIntent().getBooleanExtra(K_OPEN_CAMERA, false)) {//go directly to camera view, removing any fragments over the pager
            removeUpperFragments();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openScreen(SCREEN_RECORDING);
                }
            });
        }
        int orientation = getResources().getConfiguration().orientation;
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
        if (OSVApplication.sOSVBackupExt != null || OSVApplication.sOSVBackup != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
            builder.setMessage("The application database was updated,you can upload any old recordings with the desktop OSV upload script."
                    + System.getProperty("line.separator") +
                    "You can find your old recordings at:"
                    + System.getProperty("line.separator") +
                    (OSVApplication.sOSVBackupExt != null ? OSVApplication.sOSVBackupExt
                            + System.getProperty("line.separator") : "")
                    + (OSVApplication.sOSVBackup != null ? OSVApplication.sOSVBackup : "")).setTitle("Recordings backup").setNeutralButton(R.string.ok_label,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OSVApplication.sOSVBackupExt = null;
                            OSVApplication.sOSVBackup = null;
                        }
                    }).create().show();
        }

        mUploadManager.version(new RequestResponseListener() {
            @Override
            public void requestFinished(int status, String result) {
                Log.d(TAG, "version: " + result);

                PackageInfo pInfo;
                int version = 1000;
                try {
                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    version = pInfo.versionCode;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    JSONObject obj = new JSONObject(result);
                    String ver = obj.getString("version");
                    if (Double.parseDouble(ver) > version) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showDialogUpdateVersion();
                            }
                        });

                    }
                } catch (Exception e) {
                    Log.d(TAG, "requestFinished: " + Log.getStackTraceString(e));
                }
            }

            @Override
            public void requestFinished(int status) {
                Log.d(TAG, "version: status " + status);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (resultCode == -1) {
            //SUCCESS
            if (requestCode == REQUEST_CODE_GPS_RECORD) {
                if (mCameraHandlerService != null && getCurrentFragment().equals(CameraControlsFragment.TAG) && getApp().getLocationManager().hasPosition()) {
                    mCameraHandlerService.mShutterManager.startSequence();
                }
            } else if (requestCode == REQUEST_CODE_GPS) {
                mapFragment.onPositionerClicked();
            }
        } else {
            //DO NOTHING
        }


    }

    public void continueAfterCrash() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                openScreen(sCurrentScreen);
//                if (sLastSequence != -1 && Sequence.getLocalSequence(sLastSequence) != null) {
//                    mapFragment.displaySequence(Sequence.getLocalSequence(sLastSequence), true, true, sLastSequenceIndex, new RequestListener() {
//                        @Override
//                        public void requestFinished(int status) {
//                            if (status != STATUS_FAILED) {
//                                switch (sFragmentOverlayTag) {
//                                    case FullscreenPreviewFragment.TAG:
//                                        mapFragment.showFullScreen(true, sLastSequenceIndex);
//                                        break;
//                                    case ImageGridFragment.TAG:
//                                    case ImageGridOnlineFragment.TAG:
//                                        mapFragment.editCurrentlyDisplayedSequence();
//                                        break;
//                                }
//                            }
//                        }
//                    });
//
//                }
//            }
//        });
    }

    @Override
    protected void onStop() {
        if (mBoundCameraHandler && !(mCameraHandlerService != null && mCameraHandlerService.mShutterManager.isRecording())) {
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
        if (mBoundUploadHandler) {
            mUploadHandlerService.stopSelf();
            unbindService(mUploadHandlerConnection);
            mBoundUploadHandler = false;
        }
    }

//    public void onClick(View v) {
//        if (v.getId() == R.id.back_button) {
//            onBackPressed();
//            return;
//        }
//        removeUpperFragments();
//    }

    public void removeUpperFragments() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            while (fm.getBackStackEntryCount() > 1) {
                try {
                    fm.popBackStackImmediate();
                } catch (IllegalStateException e) {
                    Log.d(TAG, "removeUpperFragments: popBackStackImmediate failed");
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mActionBar != null && drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawers();
                    return;
                }
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    String tag = getCurrentFragment();
                    Log.d(TAG, "onBackPressed: " + tag);
                    if (tag.equals(MapFragment.TAG)) {
                        showExitDialog();
                        return;
                    } else if (getCurrentFragment().equals(CameraControlsFragment.TAG)) {
                        if (mCameraHandlerService.mShutterManager.isRecording()) {
                            showRecordingDialog();
                        } else {
                            openScreen(SCREEN_MAP);
                        }
                        return;
                    } else if (getCurrentFragment().equals(HintsFragment.TAG)) {
//                if (mCameraHandlerService.mShutterManager.isRecording()) {
                        if (getSupportFragmentManager().getBackStackEntryCount() <= 2) {
                            openScreen(SCREEN_RECORDING);
                        } else {
                            try {
                                getSupportFragmentManager().popBackStackImmediate();
                            } catch (IllegalStateException e) {
                                Log.d(TAG, "error: popBackStackImmediate failed");
                            }
                        }

//                } else {
//                    openScreen(SCREEN_MAP);
//                }
                        return;
                    } else if (getCurrentFragment().equals(TrackPreviewFragment.TAG)) {
                        boolean online = trackPreviewFragment.isOnline();
                        removeUpperFragments();
                        try {
                            getSupportFragmentManager().popBackStackImmediate();
                        } catch (IllegalStateException e) {
                            Log.d(TAG, "error: popBackStackImmediate failed");
                        }
                        openScreen(SCREEN_MAP);
                        openScreen(online ? SCREEN_MY_PROFILE : SCREEN_WAITING);
                        return;
                    }
                    if (mScreenModeParalell) {
                        mScreenModeParalell = false;
                        int orientation = getResources().getConfiguration().orientation;
                        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                        resizeHolders(0, true);
                    }
                    try {
                        getSupportFragmentManager().popBackStackImmediate();
                    } catch (IllegalStateException e) {
                        Log.d(TAG, "error: popBackStackImmediate failed");
                    }
                }
            }
        });
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        mMenuOpen = true;
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        mMenuOpen = false;
        super.onOptionsMenuClosed(menu);
    }

    private void showExitDialog() {
        if (mExitDialog == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
            mExitDialog = builder.setMessage("Are you sure you want to exit the app?").setTitle("Open Street View").setPositiveButton("Exit",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            }).create();
        }
        mExitDialog.show();
    }

    public void showLogInScreen() {
        if (Utils.isInternetAvailable(this)) {
            enableProgressBar(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableLandscape(false);
                }
            }, 10000);
            mUploadManager.logIn(this, new RequestResponseListener() {
                @Override
                public void requestFinished(int status, String xml) {
                    try {
                        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(is);
                        doc.getDocumentElement().normalize();

                        NodeList nodeList = doc.getElementsByTagName("user");
                        String name = "";
                        for (int i = 0; i < nodeList.getLength(); i++) {

                            Node node = nodeList.item(i);
                            Element fstElmnt = (Element) node;
                            String id = fstElmnt.getAttribute("id");
                            name = fstElmnt.getAttribute("display_name");
                            appPrefs.saveStringPreference(PreferenceTypes.K_USER_NAME, name);
                            appPrefs.saveStringPreference(PreferenceTypes.K_USER_ID, id);
                            final String finalName = name;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    usernameTextView.setText(finalName);

                                }
                            });

                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                settingsFragment.onLoginChanged(true);
                                logOutImage.setVisibility(View.VISIBLE);
                                enableProgressBar(false);
                                mUploadManager.getProfileDetails(new RequestResponseListener() {
                                    @Override
                                    public void requestFinished(int status, String result) {
                                        Log.d(TAG, "getProfileDetails: " + " status - > " + status + " result - > " + result);
                                        if (result != null && !result.isEmpty() && status == RequestResponseListener.STATUS_SUCCESS_PROFILE_DETAILS) {
                                            final String obdDistance, totalDistance, totalPhotos, overallRank, totalTracks, weeklyRank;
                                            try {
                                                JSONObject obj = new JSONObject(result);
                                                JSONObject osv = obj.getJSONObject("osv");
                                                obdDistance = osv.getString("obdDistance");
                                                totalDistance = osv.getString("totalDistance");
                                                totalPhotos = osv.getString("totalPhotos");
                                                overallRank = osv.getString("overallRank");
                                                totalTracks = osv.getString("totalTracks");
                                                weeklyRank = osv.getString("weeklyRank");

                                                SharedPreferences prefs = getSharedPreferences(ProfileFragment.PREFS_NAME, MODE_PRIVATE);
                                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                                prefsEditor.putString(ProfileFragment.K_OVERALL_RANK, overallRank);
                                                prefsEditor.putString(ProfileFragment.K_WEEKLY_RANK, weeklyRank);
                                                prefsEditor.putString(ProfileFragment.K_TOTAL_PHOTOS, totalPhotos);
                                                prefsEditor.putString(ProfileFragment.K_TOTAL_TRACKS, totalTracks);
                                                prefsEditor.putString(ProfileFragment.K_TOTAL_DISTANCE, totalDistance);
                                                prefsEditor.putString(ProfileFragment.K_OBD_DISTANCE, obdDistance);
                                                prefsEditor.apply();
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
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        enableProgressBar(false);
                    }
                }


                @Override
                public void requestFinished(final int status) {
                    enableProgressBar(false);
                }
            }, new OAuthDialogFragment.OnDetachListener() {
                @Override
                public void onDetach() {
                    enableLandscape(false);
                    enableProgressBar(false);
                }
            });
        } else {
            showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);
        }
    }

    private void showRecordingDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setMessage(R.string.stop_recording_warning_message).setTitle(R.string.stop_recording_warning_title).setNegativeButton(R.string.stop_value_string,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mCameraHandlerService != null) {
                            mNeedsToExit = true;
                            mCameraHandlerService.mShutterManager.stopSequence();
                        }
                    }
                }).setPositiveButton(R.string.keep_value_string, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, R.string.recording_in_background_toast_message, Toast.LENGTH_SHORT).show();
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(homeIntent);
            }
        }).create().show();
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getCurrentFragment().equals(CameraControlsFragment.TAG)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startImmersiveMode();

                    int orientation = newConfig.orientation;
                    boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
//                    openScreen(SCREEN_RECORDING);
                    resizeHolders(-1, portrait);
                }
            });
        }
    }

    public void addSmallFragment(Fragment fragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment.isAdded()) {
            ft.remove(fragment);
        }
        ft.add(R.id.content_frame_small, fragment, tag);
        smallFragment = fragment;
        ft.commitAllowingStateLoss();
        positionSmallFragment(isPortrait());

        if (smallFragment == mapFragment) {
            mapFragment.setMapSmall(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    cameraPreviewFragment.addCameraSurfaceView();
                }
            }, 300);
        } else {
            cameraPreviewFragment.setPreviewSmall(true);
        }
    }

    public void removeFragment(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment.isAdded()) {
            ft.remove(fragment);
        }
        ft.commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
    }

    public void addUpperFragment(Fragment fragment, String tag) {
        largeFragment = fragment;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (fragment != null) {
            if (fragment.isAdded()) {
                ft.remove(fragment);
            }
            ft.add(R.id.content_frame_upper, fragment, tag);
        }
        ft.commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
        if (largeFragment == cameraPreviewFragment) {
            cameraPreviewFragment.setPreviewSmall(false);
        } else if (largeFragment == mapFragment) {
            mapFragment.setMapSmall(false);
        }
    }

    public void switchPreviews() {
        if (mCameraHandlerService != null && mCameraHandlerService.mShutterManager.isRecording()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if (mapFragment.isAdded()) {
                ft.remove(mapFragment);
            }
            if (cameraPreviewFragment.isAdded()) {
                ft.remove(cameraPreviewFragment);
            }
            ft.commitAllowingStateLoss();
            getSupportFragmentManager().executePendingTransactions();
            ft = getSupportFragmentManager().beginTransaction();
            if (smallFragment == mapFragment) {
                cameraPreviewFragment.setPreviewSmall(true);
                mapFragment.setMapSmall(false);
//                ft.addToBackStack(CameraPreviewFragment.TAG + "&");
                ft.add(R.id.content_frame_small, cameraPreviewFragment, CameraPreviewFragment.TAG);
                ft.add(R.id.content_frame_upper, mapFragment, MapFragment.TAG);
                smallFragment = cameraPreviewFragment;
                largeFragment = mapFragment;
            } else {
                cameraPreviewFragment.setPreviewSmall(false);
                mapFragment.setMapSmall(true);
//                ft.addToBackStack(MapFragment.TAG + "&");
                ft.add(R.id.content_frame_upper, cameraPreviewFragment, CameraPreviewFragment.TAG);
                ft.add(R.id.content_frame_small, mapFragment, MapFragment.TAG);
                smallFragment = mapFragment;
                largeFragment = cameraPreviewFragment;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraPreviewFragment.addCameraSurfaceView();
                    }
                }, 300);
            }
            ft.commitAllowingStateLoss();
        }
    }


    public void logout() {
        if (!Utils.DEBUG || !appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH)) {
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.clear();
            editor.commit();
            CookieSyncManager.createInstance(this);
            CookieManager.getInstance().removeAllCookie();
        }
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_ID, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_NAME, "");
        usernameTextView.setText(" ");
        logOutImage.setVisibility(View.GONE);
        settingsFragment.onLoginChanged(false);
        SharedPreferences prefs = getSharedPreferences(ProfileFragment.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (getCurrentFragment().equals(CameraControlsFragment.TAG)) {
            startImmersiveMode();
        }
    }

    @Override
    public void onRecordingStatusChanged(final boolean started) {
        if (cameraPreviewFragment != null) {
            cameraPreviewFragment.onRecordingStatusChanged(started);
        }
        if (cameraControlsFragment != null) {
            cameraControlsFragment.onRecordingStatusChanged(started);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (started) {
                    mapFragment.enterMode(MapFragment.MODE_RECORDING);
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, navigationView);
                    addSmallFragment(mapFragment, MapFragment.TAG);

                } else {
                    mapFragment.enterMode(MapFragment.MODE_IDLE);
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, navigationView);
                    removeFragment(mapFragment);
                    removeFragment(cameraPreviewFragment);
                    addUpperFragment(cameraPreviewFragment, CameraPreviewFragment.TAG);
                }
            }
        });
        if (mNeedsToExit) {
            mNeedsToExit = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onBackPressed();
                }
            });
        }
    }

    @Override
    public void onUploadStarted(long mTotalSize) {

    }

    @Override
    public void onUploadingMetadata() {

    }

    @Override
    public void onPreparing(int nrOfFrames) {

    }

    @Override
    public void onIndexingFinished() {

    }

    @Override
    public void onUploadCancelled(int total, int remaining) {
        showSnackBar(R.string.upload_cancelled, Snackbar.LENGTH_SHORT);
    }

    @Override
    public void onUploadFinished(int successful, int unsuccessful) {
        if (successful + unsuccessful != 0) {
            showSnackBar("Upload finished: " + successful + " successful, " + unsuccessful + " failed.", Snackbar.LENGTH_LONG);
            mUploadManager.resetUploadStats();
        }
    }

    @Override
    public void onProgressChanged(final long total, long remaining) {

    }

    @Override
    public void onImageUploaded(Sequence sequence, boolean success) {

    }

    @Override
    public void onSequenceUploaded(Sequence sequence) {

    }

    @Override
    public void onIndexingSequence(Sequence sequence, int remainingUploads) {

    }

    @Override
    public void onUploadPaused() {
        showSnackBar(R.string.upload_paused, Snackbar.LENGTH_SHORT);
    }

//    public void reanimate() {
//        if (appPrefs == null || floatingActionButton == null || mUploadHandlerService == null) {
//            return;
//        }
//        if (appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME).equals("") || Utils.generateOSVFolder(this).listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String filename) {
//                return !filename.contains("-");
//            }
//        }).length <= 0) {
//            hideFAB();
//        } else {
//            floatingActionButton.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    setupFab();
//                    if (mUploadHandlerService != null && mUploadManager.sUploadStatus != UploadManager.STATUS_IDLE) {
//                        floatingActionButton.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.ic_clear_white_24dp));
//                        floatingActionButton.setOnClickListener(detailsOnClickListener);
//                    }
//                }
//            }, 500);
//        }
//    }

    @Override
    public void onUploadResumed() {
        showSnackBar(R.string.resuming_upload, Snackbar.LENGTH_SHORT);
    }

    public void showSnackBar(final int resId, final int duration) {
        showSnackBar(resId, duration, null, null);
    }

    public void showSnackBar(final CharSequence text, final int duration) {
        showSnackBar(text, duration, null, null);
    }

    public void showSnackBar(final int resId, final int duration, final String button, final Runnable onClick) {
        showSnackBar(getText(resId), duration, button, onClick);
    }

    public void showSnackBar(final CharSequence text, final int duration, final String button, final Runnable onClick) {
        runOnUiThread(new Runnable() {

            public boolean shouldGoUp;

            @Override
            public void run() {
                mSnackbar = Snackbar.make(drawerLayout, text, duration);
                if (button != null && onClick != null) {
                    mSnackbar.setAction(button, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onClick.run();
                        }
                    });
                }
                    shouldGoUp = true;
                    if (animateDownTimerTask != null) {
                        shouldGoUp = !animateDownTimerTask.cancel();
                    }
                    mSnackbar.getView().post(new Runnable() {
                        @Override
                        public void run() {
                            if (shouldGoUp) {
                                int height = mSnackbar.getView().getHeight();
                                if (height < 20) {
                                    height = (int) Math.max(height, Utils.dpToPx(MainActivity.this, 58));
                                }
//                                Log.d(TAG, "showSnackbar: goUp -" + height);
//                                animateFabToPosition(-height);
                            }
                        }
                    });
                mSnackbar.show();
            }
        });
    }

//    public void showActionBar(String title) {
//        showActionBar(title, false);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    public void showActionBar(String title, boolean showBackButton) {
//        mAppBar.setVisibility(View.VISIBLE);
//        if (mActionBar == null) {
//            mActionBar = getSupportActionBar();
//        }
//        if (mActionBar == null) {
//            return;
//        }
//        if (showBackButton) {
//            mActionBar.setHomeButtonEnabled(true);
//            mActionBar.setDisplayHomeAsUpEnabled(true);
//            mActionBar.setHomeButtonEnabled(true);
//        } else {
//            mActionBar.setDisplayHomeAsUpEnabled(false);
//            mActionBar.setHomeButtonEnabled(true);
//        }
//        if (title != null) {
//            mActionBar.setTitle(title);
//        }
//    }

    public void refreshSignatureValue(final String signatureText) {
        if (signatureActionBarText != null) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "refreshSignatureValue: signature " + signatureText);
                    signatureActionBarText.setText(signatureText);
                    if (getCurrentFragment().equals(UploadProgressFragment.TAG) || getCurrentFragment().equals(WaitingFragment.TAG)) {
                        signatureActionBarText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    public void showActionBar(String title, boolean showBackButton, int backgroundColor, int textColor, boolean light, int signatureColor) {
        showActionBar(title, showBackButton, backgroundColor, textColor, light);
        if (mActionBar == null) {
            mActionBar = getSupportActionBar();
        }
        if (mActionBar == null) {
            return;
        }
        if (signatureColor != -1 && mUploadManager != null && mUploadManager.isUploading()) {
            signatureActionBarText.setTextColor(ResourcesCompat.getColor(getResources(), signatureColor, null));
            signatureActionBarText.setVisibility(View.VISIBLE);
            signatureActionBarText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getCurrentFragment().equals(UploadProgressFragment.TAG)) {
                        if (mUploadManager != null && mUploadManager.isUploading()) {
                            onBackPressed();
                        }
                    }
                    }
            });
        } else {
            signatureActionBarText.setVisibility(View.GONE);
        }
    }

    // Todo Kali please review this
    public void showActionBar(String title, boolean showBackButton, int backgroundColor, int textColor, boolean light) {
        mAppBar.setVisibility(View.VISIBLE);
        // if background color for the action bar is white set darker_grey color for the back icon


        if (mActionBar == null) {
            mActionBar = getSupportActionBar();
        }
        if (mActionBar == null) {
            return;
        }
        if (showBackButton) {
            Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.back, null);
            if (light) {
                upArrow.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.darker_grey, null), PorterDuff.Mode.SRC_ATOP);
            } else {
                upArrow.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.white, null), PorterDuff.Mode.SRC_ATOP);
            }
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(upArrow);
            mBackButtonShown = true;
        } else {
            Drawable menuButton = ResourcesCompat.getDrawable(getResources(), R.drawable.menu, null);
            if (light) {
                menuButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.darker_grey, null), PorterDuff.Mode.SRC_ATOP);
            } else {
                menuButton.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.white, null), PorterDuff.Mode.SRC_ATOP);
            }
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(menuButton);
            mBackButtonShown = false;
        }

        if (title != null) {
            mActionBar.setTitle(title);
            toolbar.setTitleTextColor(ResourcesCompat.getColor(getResources(), textColor, null));
        }
        mActionBar.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(), backgroundColor, null));
    }

    public void hideActionBar() {
        mAppBar.setVisibility(View.GONE);
    }

    @Override
    public void onBandwidthStateChange(ConnectionQuality bandwidthState, double bandwidth) {
    }

    public OSVApplication getApp() {
        return (OSVApplication) getApplication();
    }

    public void getWindowSize(Point point) {
        WindowManager window = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        Point temp = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(temp);
        } else {
            display.getSize(temp);
        }
//        try {
//            temp.x = findViewById(R.id.main).getWidth();
//            temp.y = findViewById(R.id.main).getHeight();
//        } catch (Exception e) {
//            Log.d(TAG, "getWindowSize: " + Log.getStackTraceString(e));
//        }

        point.y = Math.max(temp.y, temp.x);
        point.x = Math.min(temp.y, temp.x);
    }

    public void startImmersiveMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        } catch (Exception e) {

        }
    }

    public void stopImmersiveMode() {
        int orientation = getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        if (portrait) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
            } catch (Exception e) {

            }
        }
    }

    public String getCurrentFragment() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count > 0) {
            String name = getSupportFragmentManager().getBackStackEntryAt(count - 1).getName();
//            Log.d(TAG, "getCurrentFragment: " + name);
            return name;

        }
        return "";
    }

    private void createDialogUpdateVersion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setMessage("There is a new version available for OpenStreetView.").setTitle("Update").setNeutralButton("Update now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.telenav.streetview")));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + "com.telenav.streetview")));
                        }
                    }
                });

            }
        }).setCancelable(false);
        alertDialog = builder.create();
    }

    private void showDialogUpdateVersion() {
        if (alertDialog == null){
            createDialogUpdateVersion();
        }
        if (!alertDialog.isShowing()){
            alertDialog.show();
        }
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fm = getSupportFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            String title = "";
            boolean showBack = false;
            // By default action bar background is dark grey and text color is white
            int backgroundColor = R.color.darker_grey, textColor = R.color.white;
            boolean isLight = false;
            int colorSignature = -1;
            String tag = fm.getBackStackEntryAt(count - 1).getName();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(fm.getBackStackEntryAt(i).getName()).append(" ");
            }
            Log.d(TAG, "onBackStackChanged: " + sb.toString());
//            Log.d(TAG, "onBackStackChanged: " + tag);
            switch (tag) {
                case MapFragment.TAG:
                    title = "";
                    backgroundColor = R.color.white;
                    isLight = true;
                    break;
                case SettingsFragment.TAG:
                    showBack = true;
                    title = "Settings";
                    isLight = true;
                    backgroundColor = R.color.white;
                    textColor = R.color.dark_grey_action_bar;
                    break;
                case ProfileFragment.TAG:
                    isLight = false;
                    showBack = true;
                    title = "Hey " + appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME) + "!";
                    backgroundColor = R.color.action_bar_blue;
                    textColor = R.color.white;
                    break;
                case CameraPreviewFragment.TAG + "&":
                case MapFragment.TAG + "&":
                case CameraPreviewFragment.TAG:
                case CameraControlsFragment.TAG:
                    enableLandscape(true);
                    startImmersiveMode();
                    hideActionBar();
                    return;
                case UploadProgressFragment.TAG:
                    backgroundColor = R.color.dark_grey_action_bar;
                    showBack = true;
                    isLight = false;
                    textColor = R.color.gray_text_color;
                    colorSignature = R.color.white;
                    title = "Uploading Track";
                    break;
                case WaitingFragment.TAG:
                    isLight = true;
                    showBack = true;
                    backgroundColor = R.color.white;
                    textColor = R.color.dark_grey_action_bar;
                    title = "Waiting upload";
                    colorSignature = R.color.signature_waiting_upload;
                    waitingFragment.setupUploadButton();
                    break;
                case HintsFragment.TAG:
                    hideActionBar();
                    return;
                case TrackPreviewFragment.TAG:
                    hideActionBar();
                    return;
                case NearbyFragment.TAG:
                    isLight = false;
                    showBack = true;
                    title = "Nearby";
                    backgroundColor = R.color.dark_grey_action_bar;
                    textColor = R.color.white;
                    break;
            }
            enableLandscape(false);
            stopImmersiveMode();
            showActionBar(title, showBack, backgroundColor, textColor, isLight, colorSignature);
        } else {
//            if (mapFragment != null) {
//                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//                ft.addToBackStack(MapFragment.TAG);
//                ft.add(R.id.content_frame_upper, mapFragment, MapFragment.TAG).commit();
//            }
        }
    }

    @Override
    public void onClick(View v) {
        if (isBackButtonEnabled()) {
            onBackPressed();
        } else {
            toggleMenu();
        }
        switch (v.getId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return;
        }
        if (v.getId() == R.id.log_out_image_button) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
            builder.setMessage(getString(R.string.logout_confirmation_message)).setTitle(getString(R.string.log_out)).setNegativeButton(R.string.cancel_label,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setPositiveButton(R.string.log_out, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    logout();
                }
            }).create().show();
        }

    }

    private void toggleMenu() {
        if (drawerLayout.isDrawerOpen(navigationView) || drawerLayout.isDrawerVisible(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            drawerLayout.openDrawer(navigationView);
        }
    }

    public boolean isBackButtonEnabled() {
        return mBackButtonShown;
    }

    public void enableLandscape(boolean enable) {
        if (enable) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onShutter() {
        if (cameraControlsFragment != null) {
            cameraControlsFragment.refreshDetails();
        }
        if (cameraPreviewFragment != null) {
            cameraPreviewFragment.onShutter();
        }
    }

    public void enableProgressBar(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressBar != null) {
                    progressBar.setVisibility(enable ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    public boolean isPortrait() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public void setLocationResolution(Status locationResolution) {
        this.locationResolution = locationResolution;
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
}
