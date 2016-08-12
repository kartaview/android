package com.telenav.osv.service;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.view.OrientationEventListener;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.external.WifiCamManager;
import com.telenav.osv.external.view.WifiCamSurfaceView;
import com.telenav.osv.listener.CameraReadyListener;
import com.telenav.osv.listener.RecordingStateChangeListener;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.manager.CameraManager;
import com.telenav.osv.manager.FocusManager;
import com.telenav.osv.manager.LocationManager;
import com.telenav.osv.manager.ShutterManager;
import com.telenav.osv.ui.fragment.CameraPreviewFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 10/20/2015.
 */
public class CameraHandlerService extends Service implements CameraReadyListener, RecordingStateChangeListener {

    private static final String TAG = "CameraHandlerService";

    private static final int NOTIFICATION_ID = 114;

    private final CameraHandlerBinder mBinder = new CameraHandlerBinder();

    public ShutterManager mShutterManager;

    public FocusManager mFocusManager;

    public LocationManager mLocationManager;

    public boolean mCameraReady = false;

    public boolean mCameraFailed = false;

    public boolean mSphereModeActive = false;

    private GLSurfaceView mGLSurfaceView;

    private CameraOrientationEventListener mOrientationListener;

    private Handler mHandler;

    public int mOrientation = 0;

    private UploadProgressListener mUploadProgressListener;

    private CameraPreviewFragment cameraReadyListener;

    private RecordingStateChangeListener mRecordingListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());

        mLocationManager = ((OSVApplication) getApplication()).getLocationManager();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mShutterManager = ((OSVApplication) getApplication()).getShutterManager();
        setupCamera(getApplication());
        // Create orientation listener. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener = new CameraOrientationEventListener(getApplication());
        mOrientationListener.enable();
        return mBinder;
    }

    protected void setupCamera(Application application) {
        // Setup the Camera hardware and preview
        CameraManager.instance.forceCloseCamera();
//        mWifiCamManager = new WifiCamManager(application);
        mShutterManager.setRecordingStateChangeListener(this);

        CameraManager.instance.setCameraReadyListener(this);

        CameraManager.instance.open();
    }

    public void setCameraSurfaceView(GLSurfaceView surface) {
        Log.d(TAG, "setCameraSurfaceView: ");
        mGLSurfaceView = surface;
        mGLSurfaceView.setRenderer(CameraManager.instance.getRenderer());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
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
                .setContentTitle("OSV")
                .setContentText("Sequence recording...")
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
            mLocationManager.stopLocationUpdates();
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
            if (cameraReadyListener != null) {
                cameraReadyListener.onCameraReady();
            }
        }
    }

    @Override
    public void onCameraFailed() {
        mCameraFailed = true;
        if (cameraReadyListener != null) {
            cameraReadyListener.onCameraFailed();
        }
    }

    public void setCameraReadyListener(CameraPreviewFragment cameraReadyListener) {
        this.cameraReadyListener = cameraReadyListener;
        if (cameraReadyListener != null) {
            if (mCameraReady) {
                cameraReadyListener.onCameraReady();
            } else if (mCameraFailed) {
                cameraReadyListener.onCameraFailed();
            }
        }
    }

    public void removeRecordingListener() {
        this.mRecordingListener = null;
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


    public interface OrientationChangedListener {
        void onOrientationChanged();
    }

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
