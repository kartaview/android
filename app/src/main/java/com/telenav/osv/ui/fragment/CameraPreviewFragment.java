package com.telenav.osv.ui.fragment;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.listener.AccuracyListener;
import com.telenav.osv.listener.CameraReadyListener;
import com.telenav.osv.listener.FocusListener;
import com.telenav.osv.listener.RecordingStateChangeListener;
import com.telenav.osv.listener.SpeedChangedListener;
import com.telenav.osv.manager.CameraManager;
import com.telenav.osv.manager.FocusManager;
import com.telenav.osv.manager.LocationManager;
import com.telenav.osv.manager.ObdManager;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

//import com.skobbler.sensorlib.listener.SignDetectedListener;
//import com.skobbler.sensorlib.sign.SignType;

/**
 * Recording ui
 * Created by Kalman on 10/7/2015.
 */
public class CameraPreviewFragment extends Fragment implements CameraReadyListener, AccuracyListener, SpeedChangedListener,
        RecordingStateChangeListener, ObdManager.ConnectionListener/*, SignDetectedListener*/, Camera.ShutterCallback {
    public final static String TAG = "CameraPreviewFragment";

    private GestureDetector mGestureDetector;

    private boolean mPaused = false;

    private View.OnTouchListener mPreviewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            if (mGestureDetector != null) {
                mGestureDetector.onTouchEvent(ev);
            }
            return true;
        }
    };

    private View view;

    private MainActivity activity;

    private GLSurfaceView mGLSurfaceView;

    private FocusManager mFocusManager;

    private AnimationSet shutterAnimation;

    private CameraHandlerService mCameraHandlerService;

    private Handler mHandler = new Handler();

    private ApplicationPreferences appPrefs;

    private TextView distanceDebugText;

    private TextView speedDebugText;

    private FrameLayout mCameraSurfaceViewContainer;

    private TextView mOBDIcon;

    private FrameLayout mOBDIconHolder;

    private TextView mOBDUnit;

    private View mDeveloperRecording;

    private View mCameraPreview;

    private ImageView signDetectionHolder;

    private ImageView mGPSIcon;

    private boolean mIsSmall = false;

    private FrameLayout mRecordingFeedbackLayout;

    private ImageView mFocusIndicator;

    private FrameLayout mFocusLockedIndicator;

    private LocationManager mLocationManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_camera, null);
        activity = (MainActivity) getActivity();
        mLocationManager = activity.getApp().getLocationManager();
        // Setup HUDs
//        mBaseHolder = (RelativeLayout) view.findViewById(R.id.base_holder);
        mCameraPreview = view.findViewById(R.id.camera_preview_layout);

        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;

//        initLayouts(inflater, portrait);
//        hintPagerAdapter.populate(portrait);

        // Setup shutter button
        mCameraSurfaceViewContainer = (FrameLayout) mCameraPreview.findViewById(R.id.gl_renderer_container);
        mCameraSurfaceViewContainer.setOnTouchListener(mPreviewTouchListener);

        appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();

        mRecordingFeedbackLayout = (FrameLayout) mCameraPreview.findViewById(R.id.recording_feedback_layout);
        if (mIsSmall) {
            mRecordingFeedbackLayout.setVisibility(View.INVISIBLE);
        } else {
            mRecordingFeedbackLayout.setVisibility(View.VISIBLE);
        }
        mOBDIcon = (TextView) mCameraPreview.findViewById(R.id.obd_icon);
        mGPSIcon = (ImageView) mCameraPreview.findViewById(R.id.gps_icon);
        mFocusIndicator = (ImageView) mRecordingFeedbackLayout.findViewById(R.id.focus_indicator);
        mFocusLockedIndicator = (FrameLayout) mRecordingFeedbackLayout.findViewById(R.id.focus_locked_indicator);
        mOBDIconHolder = (FrameLayout) mCameraPreview.findViewById(R.id.obd_icon_holder);
        mOBDUnit = (TextView) mCameraPreview.findViewById(R.id.obd_icon_unit);
        signDetectionHolder = (ImageView) mCameraPreview.findViewById(R.id.sign_detection_container);
        mDeveloperRecording = mCameraPreview.findViewById(R.id.developer_recording_indicator);
        if (appPrefs != null && appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
            mDeveloperRecording.setVisibility(View.VISIBLE);
        } else {
            mDeveloperRecording.setVisibility(View.GONE);
        }
        distanceDebugText = (TextView) mCameraPreview.findViewById(R.id.debug_distance_text);
        speedDebugText = (TextView) mCameraPreview.findViewById(R.id.debug_speed_text);


        if (appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST, false)) {
            distanceDebugText.setVisibility(View.VISIBLE);
            speedDebugText.setVisibility(View.VISIBLE);
        }
        Animation fadeIn = new AlphaAnimation(0, 0.60f);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(100);

        Animation fadeOut = new AlphaAnimation(0.60f, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(100);
        fadeOut.setDuration(300);

        shutterAnimation = new AnimationSet(false); //change to false
        shutterAnimation.addAnimation(fadeIn);
        shutterAnimation.addAnimation(fadeOut);
        mPaused = false;
        setupPreview();

        mPaused = false;
        activity.getApp().getOBDManager().addConnectionListener(this);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mCameraHandlerService != null) {
//            mCameraHandlerService.mShutterManager.setShutterCallback(mShutterCallback);
            onCameraServiceConnected(mCameraHandlerService);
        }
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocationManager != null) {
            mLocationManager.setAccuracyListener(this);
            mLocationManager.setSpeedChangedListener(this);
        }
        if (activity != null){
            if (activity.needsCameraPermission()){
                activity.setNeedsCameraPermission(false);
                activity.checkPermissionsForCamera();
            }
        }
    }

    @Override
    public void onPause() {
        if (mLocationManager != null) {
            mLocationManager.setAccuracyListener(null);
            mLocationManager.setSpeedChangedListener(null);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        // Pause the camera preview
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.getApp().getOBDManager().removeConnectionListener(this);
        mPaused = true;
        if (mCameraHandlerService != null) {
//            mCameraHandlerService.mShutterManager.setShutterCallback(null);
            removeCameraSurfaceView();
        }
        super.onDestroyView();
    }

    public void setupPreview() {
        if (mCameraHandlerService != null && mCameraHandlerService.mShutterManager != null) {
            mGestureDetector = new GestureDetector(activity, new GestureListener());
            int orientation = activity.getResources().getConfiguration().orientation;
            boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
//            if (mCameraHandlerService != null) {
////            mCameraHandlerService.mShutterManager.setShutterCallback(mShutterCallback);
//                addCameraSurfaceView();
//            }
            resizePreview(portrait, CameraManager.instance.previewWidth, CameraManager.instance.previewHeight);
            if (mCameraHandlerService.mShutterManager == null) {
                return;
            }
//            mCameraHandlerService.mShutterManager.setShutterCallback(mShutterCallback);
//            mCameraHandlerService.mShutterManager.setImageSavedListener(new Camera.ShutterCallback() {
//                @Override
//                public void onShutter() {
//                    refreshDetails();
//                }
//            });
        } else {
            Log.d(TAG, "setupPreview: service or shutter manager is null");
        }
    }

    @Override
    public void onShutter() {
//        mHandler.post(new Runnable() { todo removed shutter animation
//            @Override
//            public void run() {
//                final View fadingView = mCameraPreview.findViewById(R.id.save_fading_view);
//                shutterAnimation.setAnimationListener(new Animation.AnimationListener() {
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//                        fadingView.setVisibility(View.VISIBLE);
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        fadingView.setVisibility(View.GONE);
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//
//                    }
//                });
//                fadingView.clearAnimation();
//                fadingView.startAnimation(shutterAnimation);
//            }
//        });
    }

    public void addCameraSurfaceView() {
        Log.d(TAG, "addCameraSurfaceView: ");
        if (mCameraPreview != null) {
            // Delete the previous GL Surface View (if any)
            if (mGLSurfaceView != null) {
                mCameraSurfaceViewContainer.removeView(mGLSurfaceView);
                mGLSurfaceView = null;
            }

            // Make a new GL view using the provided renderer
            mGLSurfaceView = new GLSurfaceView(activity);
            mGLSurfaceView.setEGLContextClientVersion(2);
            mGLSurfaceView.setZOrderMediaOverlay(false);
            mCameraSurfaceViewContainer.addView(mGLSurfaceView);
            if (mCameraHandlerService != null) {
                mCameraHandlerService.setCameraSurfaceView(mGLSurfaceView);
            }
        }
    }

    public void removeCameraSurfaceView() {
        if (mCameraPreview != null) {
            final ViewGroup container = ((ViewGroup) mCameraPreview.findViewById(R.id.gl_renderer_container));
            // Delete the previous GL Surface View (if any)
            if (mGLSurfaceView != null) {
                container.removeView(mGLSurfaceView);
                mGLSurfaceView = null;
                if (mCameraHandlerService != null) {
                    mCameraHandlerService.removeCameraSurfaceView();
                }
            }
        }
    }

    /**
     * @param portrait
     * @param previewWidthF  scaled to screen size
     * @param previewHeightF scaled to screen size
     */
    public void resizePreview(final boolean portrait, final int previewWidthF, final int previewHeightF) {
        Point point = new Point();
        activity.getWindowSize(point);
        Log.d(TAG, "resizePreview: contentHeight = " + point.y + " contentWidth = " + point.x);
        float ratio;
        int previewHeight;
        int previewWidth;
        if (previewHeightF == 0) {
            previewHeight = CameraManager.instance.previewHeight;
        } else {
            previewHeight = previewHeightF;
        }
        if (previewWidthF == 0) {
            previewWidth = CameraManager.instance.previewWidth;
        } else {
            previewWidth = previewWidthF;
        }
        if (!portrait) {
            ratio = (float) point.x / (float) previewHeight;
        } else {
            ratio = (float) point.x / (float) previewHeight;
        }

        RelativeLayout.LayoutParams previewLayoutParams;
        if (portrait) {
            int value = (int) ((float) previewWidth * ratio);
            Log.d(TAG, "resizePreview: surface height = " + value);
            previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, value);
            previewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else {
            int value = (int) ((float) previewWidth * ratio);
            Log.d(TAG, "resizePreview: surface width = " + value);
            previewLayoutParams = new RelativeLayout.LayoutParams(value, RelativeLayout.LayoutParams.MATCH_PARENT);
            previewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        mCameraPreview.setLayoutParams(previewLayoutParams);
//        addCameraSurfaceView();
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        onOrientationChanged(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
//    }

//    public void onOrientationChanged(boolean portrait) {
//        if (mCameraHandlerService != null) {
//            resizePreview(portrait, mCameraHandlerService.mCamManager.previewWidth, mCameraHandlerService.mCamManager.previewHeight);
//        }
//    }

    public void onCameraServiceConnected(CameraHandlerService cameraHandlerService) {
        this.mCameraHandlerService = cameraHandlerService;
        addCameraSurfaceView();
    }

    @Override
    public void onCameraReady() {
        Log.d(TAG, "onCameraReady: ");
        if (mCameraHandlerService.mFocusManager != null) {
            mFocusManager = mCameraHandlerService.mFocusManager;
            mFocusManager.setListener(new MainFocusListener());
        } else {
            Log.e(TAG, "onCameraReady: mFocusManager is null");
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDeveloperRecording != null) {
                    if (appPrefs != null && appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                        mDeveloperRecording.setVisibility(View.VISIBLE);
                    } else {
                        mDeveloperRecording.setVisibility(View.GONE);
                    }
                    int orientation = activity.getResources().getConfiguration().orientation;
                    boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
                    resizePreview(portrait, CameraManager.instance.previewWidth, CameraManager.instance.previewHeight);
                }
            }
        });
    }

    @Override
    public void onCameraFailed() {
    }

    @Override
    public void onPermissionNeeded() {
    }


    @Override
    public void onAccuracyChanged(float accuracy) {
        if (activity != null && mGPSIcon != null) {
            if (accuracy <= LocationManager.ACCURACY_GOOD) {
                mGPSIcon.setImageDrawable(activity.getResources().getDrawable(R.drawable.g_p_s_good));
            } else if (accuracy <= LocationManager.ACCURACY_MEDIUM) {
                mGPSIcon.setImageDrawable(activity.getResources().getDrawable(R.drawable.g_p_s_medium));
            } else {
                mGPSIcon.setImageDrawable(activity.getResources().getDrawable(R.drawable.g_p_s_low));
            }
        }
    }

    @Override
    public void onSpeedChanged(float mSpeed, SpeedCategory category) {
        if (Utils.isDebugEnabled(activity) && appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SPEED_DIST) && distanceDebugText != null && speedDebugText != null) {
            distanceDebugText.setVisibility(View.VISIBLE);
            speedDebugText.setVisibility(View.VISIBLE);
            distanceDebugText.setText(category.toString());
            speedDebugText.setText(Utils.formatDistanceFromKiloMeters(activity, mSpeed)[0]);
        } else {
            if (distanceDebugText != null && speedDebugText != null) {
                distanceDebugText.setVisibility(View.GONE);
                speedDebugText.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onRecordingStatusChanged(boolean started) {

    }

    @Override
    public void onConnected() {
        if (mOBDIcon != null) {
            mOBDIconHolder.setVisibility(View.VISIBLE);
            mOBDIcon.setText("0");
            mOBDUnit.setText(Utils.formatSpeedFromKmph(activity, 0)[1]);
            mOBDIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            activity.getApp().getOBDManager().setAuto();
                            activity.getApp().getOBDManager().reset();
                        }
                    }).start();
                }
            });
        }
    }

    @Override
    public void onDisconnected() {
        if (mOBDIcon != null) {
            mOBDIconHolder.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSpeedObtained(ObdManager.SpeedData speedData) {
        if (mOBDIcon != null && speedData.getSpeed() != -1) {
            mOBDIcon.setText(Utils.formatSpeedFromKmph(activity, speedData.getSpeed())[0]);
        }
    }

    @Override
    public void onConnecting() {
        if (mOBDIcon != null) {
            mOBDIconHolder.setVisibility(View.VISIBLE);
            mOBDIcon.setText("-");
            mOBDUnit.setText(null);
            mOBDIcon.setOnClickListener(null);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
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

    public void setPreviewSmall(boolean mIsSmall) {
        this.mIsSmall = mIsSmall;
        if (mIsSmall) {
            mRecordingFeedbackLayout.setVisibility(View.INVISIBLE);
        } else {
            mRecordingFeedbackLayout.setVisibility(View.VISIBLE);
        }

    }

    public void bringToFront() {
        if (mGLSurfaceView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mGLSurfaceView.setZ(5);
                mGLSurfaceView.setTranslationZ(5);
            }
            mGLSurfaceView.bringToFront();
            mGLSurfaceView.setZOrderMediaOverlay(true);
        }
    }

    public void sendToBack() {
        if (mGLSurfaceView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mGLSurfaceView.setZ(0);
                mGLSurfaceView.setTranslationZ(0);
            }
            mGLSurfaceView.setZOrderMediaOverlay(false);
        }
    }


    /**
     * Focus listener to animate the focus HUD ring from FocusManager events
     */
    private class MainFocusListener implements FocusListener {
        @Override
        public void onFocusStart(final boolean smallAdjust) {
        }

        @Override
        public void onFocusReturns(final boolean smallAdjust, final boolean success) {
        }
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                if (mPaused || mCameraHandlerService == null) return false;
                // A single tap equals to touch-to-focus
                if (mFocusManager != null) {
                    float indicatorSize = getResources().getDimension(R.dimen.tap_to_focus_view);
                    setFocusViewOnScreen(mFocusIndicator, e, indicatorSize);
                    ObjectAnimator circleFade = ObjectAnimator.ofFloat(mFocusIndicator, View.ALPHA, 1f, 0f);
                    circleFade.setDuration(1000);
                    circleFade.start();
                    if (mCameraHandlerService.mShutterManager.isRecording() && mIsSmall) {
                        activity.switchPreviews();
                    } else {
                        mFocusManager.focusOnTouch(e, mGLSurfaceView.getWidth(), mGLSurfaceView.getHeight(), indicatorSize / 2, false);
                    }
                }
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mCameraHandlerService.mShutterManager.isRecording()) {
                LocationManager lm = mCameraHandlerService.mLocationManager;
                if (mCameraHandlerService.mLocationManager.hasPosition()) {
                    double dist = ComputingDistance.distanceBetween(lm.getActualLocation().getLongitude(), lm.getActualLocation().getLatitude(), lm.getPreviousLocation()
                            .getLongitude(), lm.getPreviousLocation().getLatitude());
                    mCameraHandlerService.mShutterManager.takeSnapshot(lm.getActualLocation(), lm.getAccuracy(), dist);
                }
            }
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            float indicatorSize = getResources().getDimension(R.dimen.tap_to_focus_view);
            setFocusViewOnScreen(mFocusLockedIndicator, e, indicatorSize);
            ObjectAnimator circleFade = ObjectAnimator.ofFloat(mFocusLockedIndicator, View.ALPHA, 1f, 0f);
            circleFade.setDuration(1000);
            circleFade.start();
            if (mFocusManager != null) {
                mFocusManager.focusOnTouch(e, mGLSurfaceView.getWidth(), mGLSurfaceView.getHeight(), indicatorSize / 2f, true);
            }
        }
    }

    private void setFocusViewOnScreen(View v, MotionEvent e, float indicatorSize) {
        float x = e.getRawX();
        float y = e.getRawY();
        float r = indicatorSize / 2f;
        float left, top;

        if (x - r < 0) {
            left = 0;
        } else if (x + r > mGLSurfaceView.getWidth()) {
            left = mGLSurfaceView.getWidth() - 2f * r;
        } else {
            left = x - r;
        }
        if (y - r < 0) {
            top = 0;
        } else if (y + r > mGLSurfaceView.getHeight()) {
            top = mGLSurfaceView.getHeight() - 2f * r;
        } else {
            top = y - r;
        }
        v.setVisibility(View.VISIBLE);
        v.setX(left);
        v.setY(top);
    }
}
