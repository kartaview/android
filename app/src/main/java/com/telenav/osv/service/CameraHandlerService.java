package com.telenav.osv.service;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.view.OrientationEventListener;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.external.view.WifiCamSurfaceView;
import com.telenav.osv.listener.CameraReadyListener;
import com.telenav.osv.listener.RecordingStateChangeListener;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.CameraManager;
import com.telenav.osv.manager.FocusManager;
import com.telenav.osv.manager.LocationManager;
import com.telenav.osv.manager.ObdManager;
import com.telenav.osv.manager.ShutterManager;
import com.telenav.osv.obd.BLEConnection;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 10/20/2015.
 */
public class CameraHandlerService extends Service implements CameraReadyListener, RecordingStateChangeListener {

    public static final String FLAG_BLUETOOTH = "eventBluetooth";

    private static final String TAG = "CameraHandlerService";

    private static final int NOTIFICATION_ID = 114;

    private final CameraHandlerBinder mBinder = new CameraHandlerBinder();

    public ShutterManager mShutterManager;

    public FocusManager mFocusManager;

    public LocationManager mLocationManager;

    public boolean mCameraReady = false;

    public boolean mCameraFailed = false;

    public boolean mSphereModeActive = false;

    public int mOrientation = -1;

    private GLSurfaceView mGLSurfaceView;

    private CameraOrientationEventListener mOrientationListener;

    private Handler mHandler;

    private UploadProgressListener mUploadProgressListener;

    private CameraReadyListener mCameraReadyListener;

    private RecordingStateChangeListener mRecordingListener;

    private boolean mCameraPermissionNeeded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());

        mLocationManager = ((OSVApplication) getApplication()).getLocationManager();
        mLocationManager.connect();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mShutterManager = ((OSVApplication) getApplication()).getShutterManager();
        setupCamera();
        // Create orientation listener. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener = new CameraOrientationEventListener(getApplication());
        mOrientationListener.enable();
        return mBinder;
    }

    protected void setupCamera() {
        // Setup the Camera hardware and preview
        CameraManager.instance.forceCloseCamera();
//        mWifiCamManager = new WifiCamManager(application);
        mShutterManager.setRecordingStateChangeListener(this);

        CameraManager.instance.setCameraReadyListener(this);
        mCameraFailed = false;
        mCameraReady = false;
        mCameraPermissionNeeded = false;
        int cameraPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            mCameraPermissionNeeded = true;
        } else {
            CameraManager.instance.open();
        }
    }

    public void setCameraSurfaceView(GLSurfaceView surface) {
        Log.d(TAG, "setCameraSurfaceView: ");
        mGLSurfaceView = surface;
        mGLSurfaceView.setRenderer(CameraManager.instance.getRenderer());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        OSVApplication app = ((OSVApplication) getApplication());
        if (intent != null) {
            if (!intent.getBooleanExtra(FLAG_BLUETOOTH, false)) {
                return 0;
            }
            ObdManager obdManager = app.getOBDManager();
            if (!obdManager.isBluetooth()) {
                return 0;
            }
            BluetoothAdapter adapter;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                BluetoothManager bm = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
                adapter = bm.getAdapter();
            } else {
                adapter = BluetoothAdapter.getDefaultAdapter();
            }
            if (adapter.isEnabled()) {
                if (obdManager.isBle()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        return 0;
                    }

                    // Use this check to determine whether BLE is supported on the device. Then
                    // you can selectively disable BLE-related features.
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                        return 0;
                    }

                    BLEConnection.getInstance().initConnection(this);

                    SharedPreferences preferences = getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
                    if (preferences.getBoolean(Constants.BLE_SERVICE_STARTED, false) && preferences.getString(Constants.EXTRAS_BLE_DEVICE_ADDRESS, null) != null) {
                        obdManager.connect();
                        return START_STICKY;
                    } else {
                        return 0;
                    }
                } else {
                    // Use this check to determine whether BT is supported on the device. Then
                    // you can selectively disable BT-related features.
                    if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                        return 0;
                    }

                    SharedPreferences preferences = getSharedPreferences(Constants.PREF, Activity.MODE_PRIVATE);
                    if (preferences.getBoolean(Constants.BT_SERVICE_STARTED, false) && preferences.getString(Constants.EXTRAS_BT_DEVICE_ADDRESS, null) != null) {
                        obdManager.connect();
                        return START_STICKY;
                    }
                }
            }
        }
        return 0;
    }


    public void recordingStarted() {
        CameraManager.instance.startDetection();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRecordingListener != null) {
                    mRecordingListener.onRecordingStatusChanged(true);
                }
            }
        });
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.K_OPEN_CAMERA, true);
        PendingIntent pnextIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_short_name))
                .setContentText(getString(R.string.notification_sequence_recording_label))
                .setSmallIcon(R.drawable.ic_recording_pin)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(pnextIntent).build();
        startForeground(NOTIFICATION_ID, notification);
    }


    public void recordingStopped() {
        CameraManager.instance.stopDetection();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRecordingListener != null) {
                    mRecordingListener.onRecordingStatusChanged(false);
                }
            }
        });
        stopForeground(true);
    }

    public void setRecordingListener(RecordingStateChangeListener mRecordingListener) {
        this.mRecordingListener = mRecordingListener;
    }

    @Override
    public void onDestroy() {
        if (mOrientationListener != null) {
            mOrientationListener.disable();
        }
        removeCameraSurfaceView();

        CameraManager.instance.forceCloseCamera();

        if (mLocationManager != null) {
            mLocationManager.disconnect();
        }
        super.onDestroy();
    }

    public void removeCameraSurfaceView() {
        Log.d(TAG, "removeCameraSurfaceView: ");
        if (mGLSurfaceView != null) {
            mGLSurfaceView.onPause();
            mGLSurfaceView = null;
        }
    }

    @Override
    public void onCameraReady() {
        if (!mCameraReady && !mCameraFailed) {
            mCameraReady = true;
            mLocationManager.setShutterManager(mShutterManager);

            CameraManager.instance.updateDisplayOrientation();

            if (mFocusManager == null) {
                mFocusManager = new FocusManager();
            }
            if (mShutterManager != null) {
                mShutterManager.setFocusManager(mFocusManager);
            }
            if (mCameraReadyListener != null) {
                mCameraReadyListener.onCameraReady();
            }
        }
    }

    @Override
    public void onCameraFailed() {
        mCameraFailed = true;
        if (mCameraReadyListener != null) {
            mCameraReadyListener.onCameraFailed();
        }
    }

    @Override
    public void onPermissionNeeded() {

    }

    public void setCameraReadyListener(CameraReadyListener mCameraReadyListener) {
        this.mCameraReadyListener = mCameraReadyListener;
        if (mCameraReadyListener != null) {
            if (mCameraReady) {
                mCameraReadyListener.onCameraReady();
            } else if (mCameraFailed) {
                mCameraReadyListener.onCameraFailed();
            } else if (mCameraPermissionNeeded) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                    mCameraReadyListener.onPermissionNeeded();
                } else {
                    mCameraPermissionNeeded = false;
                }
            }
        }
    }

    public void removeRecordingListener() {
        this.mRecordingListener = null;
    }

    public void removeCameraReadyListener() {
        this.mCameraReadyListener = null;
    }

    public void startSphereCamera(WifiCamSurfaceView surfaceView, final CameraReadyListener listener) {
        if (mSphereModeActive) {
            return;
        }

        mSphereModeActive = true;
//        mWifiCamManager.startPreview(surfaceView, new CameraReadyListener() {
//            @Override
//            public void onCameraReady() {
//                if (mCamManager != null) {
//                    mCamManager.pause();
//                }
//                mSphereModeActive = true;
//                listener.onCameraReady();
//            }
//
//            @Override
//            public void onCameraFailed() {
////                startNormalCamera();
//                listener.onCameraFailed();
//            }
//        });
    }

    public void startNormalCamera() {
//        if (!mSphereModeActive) {
//            return;
//        }
//        mSphereModeActive = false;
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
////                if (mWifiCamManager != null) {
////                    mWifiCamManager.disconnectCamera();
////                }
//                if (mCamManager != null) {
//                    mCamManager.resume();
//                }
//            }
//        });
    }

    @Override
    public void onRecordingStatusChanged(boolean started) {
        if (started) {
            recordingStarted();
        } else {
            recordingStopped();
        }
    }

//    public void setSignDetectedListener(SignDetectedListener signDetectedListener) {
//        if (mCamManager != null) {
//            mCamManager.setSignDetectedListener(signDetectedListener);
//        }
//    }

    public class CameraHandlerBinder extends Binder {
        public CameraHandlerService getService() {
            return CameraHandlerService.this;
        }
    }

    /**
     * Handles the orientation changes without turning the actual activity
     */
    private class CameraOrientationEventListener extends OrientationEventListener {
        public CameraOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            int value = Utils.roundOrientation(orientation, mOrientation);

            if (value == mOrientation) {
                return;
            }
            mOrientation = value;
            // Notify camera of the raw orientation
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    CameraManager.instance.setOrientation(mOrientation);
                }
            }, 1000);

        }
    }

}
