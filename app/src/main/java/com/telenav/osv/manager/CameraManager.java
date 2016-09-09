package com.telenav.osv.manager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Application;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.listener.CameraReadyListener;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

//import com.skobbler.sensorlib.SensorLib;
//import com.skobbler.sensorlib.SensorLibSettings;
//import com.skobbler.sensorlib.listener.SignDetectedListener;

/**
 * This class is responsible for interacting with the Camera hardware.
 * It provides easy open/close and other interactions with camera
 * Created by Kalman on 10/7/2015.
 */
public class CameraManager {
    private final static String TAG = "CameraManager";

    private final Object syncObject = new Object();

//    private SensorLib mSensorLib;

    public int previewWidth;

    public int previewHeight;

    public CameraPreview mPreview;

    private SurfaceTexture mSurfaceTexture;

    private static Camera mCamera;

    private AutoFocusMoveCallback mAutoFocusMoveCallback;

    private Camera.Parameters mParameters;

    private int mOrientation = -1;

    private CameraReadyListener mCameraReadyListener;

    private Handler mHandler;

    private Application mContext;

    private CameraRenderer mRenderer;

    private boolean mIsPreviewStarted;

    private int mTriedApplyingCam;

    private String mSceneMode;

    private String mWhiteBalanceMode;

    private HandlerThread mCameraThread;

    private Handler mCameraHandler;

    private ApplicationPreferences appPrefs;

    private List<Camera.Size> supportedPicturesSizes;

    private HandlerThread mOpenThread;

    private Handler mOpenHandler;

    private Camera.Size sixteen = null;
    private Camera.Size twelve = null;
    private Camera.Size eight = null;
    private Camera.Size five = null;

//    private SignDetectedListener mSignDetectedListener;

    private boolean mRunDetection = false;

    private int mOrientationType = 0;

    public static CameraManager instance;

    public CameraManager(Application context) {
        if (instance != null) {
            return;
        }
        instance = this;
        mPreview = new CameraPreview();
        mHandler = new Handler();
        mContext = context;
        mRenderer = new CameraRenderer();
        mIsPreviewStarted = false;
        // Try to open the camera

        appPrefs = ((OSVApplication) mContext).getAppPrefs();
        mOpenThread = new HandlerThread("OpenCamera", Process.THREAD_PRIORITY_FOREGROUND);
        // Try to open the camera
        mOpenThread.start();
        mOpenHandler = new Handler(mOpenThread.getLooper());
        initSensorLib();
    }

    /**
     * Opens the camera and show its preview in the preview
     *
     * @return true if the operation succeeded, false otherwise
     */
    public boolean open() {
        mCameraThread = new HandlerThread("Camera", Process.THREAD_PRIORITY_FOREGROUND);
        // Try to open the camera
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        Log.d(TAG, "open: Camera");
        if (mCamera != null) {

            // Close the previous camera
            releaseCamera();
        }
        mOpenHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (syncObject) {
                    try {
                        if (mCamera != null) {
                            forceCloseCamera();
                            Log.e(TAG, "Previous camera not closed! Not opening");
//                            return;
                        }
                        createTexture();
                        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                        Log.v(TAG, "Camera is open");
                        try {
                            if (Build.VERSION.SDK_INT >= 17) {
                                mCamera.enableShutterSound(false);
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "open: enable shuttersound failed.");
                        }
                        mParameters = mCamera.getParameters();
                        mParameters.setPictureFormat(ImageFormat.JPEG);
                        mParameters.setJpegQuality(100);

                        supportedPicturesSizes = getOptimalPictureSize(mParameters.getSupportedPictureSizes());
                        setSupportedPicturesSizesPreferences();
                        Log.d(TAG, " resolutionWidth: " + appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) + " resolutionHeight : " + appPrefs.getIntPreference
                                (PreferenceTypes.K_RESOLUTION_HEIGHT));
                        if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
                            mParameters.setPictureSize(supportedPicturesSizes.get(0).width, supportedPicturesSizes.get(0).height);
                            Log.d(TAG, " Resolution of the image is in fast recording is: " + supportedPicturesSizes.get(0).width + " x " + supportedPicturesSizes.get(0).height);
                        } else {
                            if (appPrefs != null) {
                                mParameters.setPictureSize(appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH), appPrefs.getIntPreference(PreferenceTypes
                                        .K_RESOLUTION_HEIGHT));
                            }
                            Log.d(TAG, " Resolution of the image is: " + mParameters.getPictureSize().width + " x " + mParameters.getPictureSize().height);
                        }
                        Camera.Size maxPreviewSize = getOptimalPreviewSize(mParameters.getSupportedPreviewSizes());
                        if (maxPreviewSize != null) {
                            mParameters.setPreviewSize(maxPreviewSize.width, maxPreviewSize.height);
                        }
                        previewWidth = mParameters.getPreviewSize().width;
                        previewHeight = mParameters.getPreviewSize().height;
                        if (mParameters.getSupportedSceneModes() != null) {
                            if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_HDR) && Utils.DEBUG && false) {//todo hardcoded removal of hdr
                                mSceneMode = Camera.Parameters.SCENE_MODE_HDR;
                                Log.d(TAG, "enableHDR: true");
                            } else if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                                mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
                                ((OSVApplication) mContext.getApplicationContext())
                                        .getAppPrefs().saveBooleanPreference(PreferenceTypes.K_DEBUG_HDR, false);
                                Log.d(TAG, "enableHDR: false");
                            }
                            mParameters.setSceneMode(mSceneMode);

                        }
                        if (mParameters.getFlashMode() != null) {
                            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }

                        int min = mParameters.getMinExposureCompensation();
                        int max = mParameters.getMaxExposureCompensation();
                        float step = mParameters.getExposureCompensationStep();
                        //need to setExposureCompensation??
//                        mParameters.setExposureCompensation(max);
                        if (mParameters.getSupportedWhiteBalance() != null && mParameters.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                            mWhiteBalanceMode = Camera.Parameters.WHITE_BALANCE_AUTO;
                            mParameters.setWhiteBalance(mWhiteBalanceMode);
                        }
                        mPreview.notifyPreviewSize(mParameters.getPreviewSize().width, mParameters.getPreviewSize().height);
                        mCamera.setParameters(mParameters);
                        mCamera.setPreviewCallback(mPreview);
                        if (mAutoFocusMoveCallback != null) {
                            setAutoFocusMoveCallback(mAutoFocusMoveCallback);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error while opening camera: " + e.getMessage(), e);

                        if (mCameraReadyListener != null) {
                            mCameraReadyListener.onCameraFailed();
                        }

                        return;
                    }

                    if (mCameraReadyListener != null) {
                        mCameraReadyListener.onCameraReady();
                    }
                    mPreview.notifyCameraChanged(true);
                }
                if (mOrientation != -1) {
                    int temp = mOrientation;
                    mOrientation = -1;
                    setOrientation(temp);
                }
            }
        });

        return true;
    }

    public List<Camera.Size> getSupportedPicturesSizes() {
        return supportedPicturesSizes;
    }

    /**
     * This method chooses the best resolution for the taken picture below 16mp
     *
     * @param supportedPictureSizes
     * @return
     */
    private List<Camera.Size> getOptimalPictureSize(List<Camera.Size> supportedPictureSizes) {
        List<Camera.Size> relevantSizesList = new ArrayList<>();

        int fiveMpLimit = (1948 * 2596) + 20;
        int eightMpLimit = (3840 * 2160) + 20;
        int twelveMpLimit = (3024 * 4032) + 20;
        int sixteenMpLimit = (2988 * 5312) + 20;

        if (((OSVApplication) mContext).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
            Collections.sort(supportedPictureSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return (lhs.height * lhs.width) - (rhs.height * rhs.width);
                }
            });
            int limit = 1_000_000;
            for (Camera.Size sz : supportedPictureSizes) {
                if (sz.height * sz.width > limit) {
                    Log.d(TAG, "getOptimalPictureSize: using resolution for developer mode: " + sz.width + " x " + sz.height);
                    relevantSizesList.add(sz);
                    return relevantSizesList;
                }
            }
        } else {
            Collections.sort(supportedPictureSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return (rhs.height * rhs.width) - (lhs.height * lhs.width);
                }
            });
            for (Camera.Size sz : supportedPictureSizes) {
                if ((sz.height * sz.width <= sixteenMpLimit) && (sz.height * sz.width > twelveMpLimit)) {
                    if (sixteen == null) {
                        sixteen = sz;
                    }
                } else if ((sz.height * sz.width <= twelveMpLimit) && (sz.height * sz.width > eightMpLimit)) {
                    if (twelve == null) {
                        twelve = sz;
                    }
                } else if ((sz.height * sz.width <= eightMpLimit) && (sz.height * sz.width > fiveMpLimit)) {
                    if (eight == null) {
                        eight = sz;
                    }
                } else if (sz.height * sz.width <= fiveMpLimit) {
                    if (five == null) {
                        five = sz;
                    }
                }
            }
            if (sixteen != null) {
                relevantSizesList.add(sixteen);
            }
            if (twelve != null) {
                relevantSizesList.add(twelve);
            }
            if (eight != null) {
                relevantSizesList.add(eight);
            }
            if (five != null) {
                relevantSizesList.add(five);
            }
            return relevantSizesList;
        }
        return relevantSizesList;
    }

    /**
     * Set preferences based on the available picture sizes (8MP is the priority)
     */
    private void setSupportedPicturesSizesPreferences() {
        if ((appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT) == 0) || (appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) == 0)) {
            Camera.Size sizesPreferences = null;
            if (eight != null) {
                sizesPreferences = eight;
            } else if (twelve != null) {
                sizesPreferences = twelve;
            } else if (sixteen != null) {
                sizesPreferences = sixteen;
            } else if (five != null) {
                sizesPreferences = five;
            }
            if (sizesPreferences != null) {
                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, sizesPreferences.width);
                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, sizesPreferences.height);
            }
        }
    }

    /**
     * This method chooses the best resolution for the taken picture below 8mp
     *
     * @param supportedSizes
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> supportedSizes) {
        Collections.sort(supportedSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return (rhs.height * rhs.width) - (lhs.height * lhs.width);
            }
        });
        for (Camera.Size sz : supportedSizes) {
            float ratio = (float) sz.width / (float) sz.height;
            if (sz.height * sz.width <= (1100 * 1930) + 20 && ratio > 1.3f && ratio < 1.4f) {
                Log.d(TAG, "getOptimalPreviewSize: using resolution: " + sz.width + " x " + sz.height);
                return sz;
            }
        }
        return supportedSizes.get(0);
    }


    public void setCameraReadyListener(CameraReadyListener listener) {
        mCameraReadyListener = listener;
    }

    public void startDetection() {
        mRunDetection = true;
    }

    public void stopDetection() {
        mRunDetection = false;
    }

//    public void setSignDetectedListener(SignDetectedListener listener) {
////        if (mSensorLib != null) {
////            mSignDetectedListener = listener;
////            mSensorLib.setOnSignDetectedListener(mSignDetectedListener);
////        }
//    }

    /**
     * @return The GLES20-compatible renderer for the camera preview
     */
    public CameraRenderer getRenderer() {
        return mRenderer;
    }

    /**
     * Returns the parameters structure of the current running camera
     *
     * @return Camera.Parameters
     */
    public Camera.Parameters getParameters() {
        if (mCamera == null) {
            Log.w(TAG, "getParameters when camera is null");
            return null;
        }

        int tries = 0;
        while (mParameters == null) {
            try {
                mParameters = mCamera.getParameters();
                break;
            } catch (RuntimeException e) {
                Log.e(TAG, "Error while getting parameters: ", e);
                if (tries < 3) {
                    tries++;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Failed to get parameters after 3 tries");
                    break;
                }
            }
        }

        return mParameters;
    }

    public void destroySensorLib() {
//        if (mSensorLib != null) {
//            mSensorLib.destroy();
//            mSensorLib = null;
//        }
    }

    public void initSensorLib() {
        mOpenHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final boolean signDetectionActivated = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_SIGN_DETECTION);

                    if (signDetectionActivated) {
//                        mSensorLib = new SensorLib(mContext);
//                        mSensorLib.setSensorLibSettings(new SensorLibSettings(10, 10));
//                        if (mSignDetectedListener != null) {
//                            mSensorLib.setOnSignDetectedListener(mSignDetectedListener);
//                        }
                    }

                } catch (ExceptionInInitializerError e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Used by OSVApplication safeguard to release the camera when the app crashes.
     */
    public void forceCloseCamera() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
                mCamera = null;
                mParameters = null;
                if (mCameraThread != null) {
                    mCameraThread.quit();
                }
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    private void releaseCamera() {
        try {
            if (mCamera != null) {
                Log.v(TAG, "Releasing camera");
                safeStopPreview();
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
                mCamera = null;
                mParameters = null;
            }
            if (mCameraThread != null) {
                mCameraThread.quit();
            }
        } catch (Exception e) {
            Log.d(TAG, "releaseCamera: " + Log.getStackTraceString(e));
        }
    }

    private void safeStartPreview() {
        try {
            if (!mIsPreviewStarted && mCamera != null) {
                Log.d(TAG, "safeStartPreview");
                mCamera.startPreview();
                mIsPreviewStarted = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "safeStartPreview: " + Log.getStackTraceString(e));
        }
    }

    private void safeStopPreview() {
        try {
            if (mIsPreviewStarted && mCamera != null) {
                Log.d(TAG, "safeStopPreview");
                mCamera.stopPreview();
                mIsPreviewStarted = false;
            }
        } catch (Exception e) {
            Log.w(TAG, "safeStopPreview: " + Log.getStackTraceString(e));
        }
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Takes a snapshot
     */
    public boolean takeSnapshot(final Camera.ShutterCallback shutterCallback,
                                final Camera.PictureCallback raw, final Camera.PictureCallback jpeg) {

        Log.v(TAG, "takePicture: entered");
        if (mCamera != null) {
            mCameraHandler.post(new Runnable() {
                public void run() {
                    synchronized (syncObject) {
                        try {
                            Log.d(TAG, "takePicture: trying to take a picture...");
                            mCamera.takePicture(shutterCallback, raw, jpeg);
                            Log.d(TAG, "takePicture: success");
                        } catch (RuntimeException e) {
                            Log.e(TAG, "takePicture: Unable to take picture", e);
                            if (e.getLocalizedMessage().contains("error=-38")) {
                                Log.e(TAG, "takePicture: Unable to take picture during debug", e);
                            } else {
                                forceCloseCamera();
                                open();
                                restartPreviewIfNeeded();
                            }
                        }
                    }
                }
            });
            return true;
        } else {
            Log.w(TAG, "takePicture: camera is null");
            forceCloseCamera();
            open();
        }
        return false;
    }

    /**
     * @return The orientation of the device
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Sets the current orientation of the device
     *
     * @param orientation The orientation, in degrees
     */
    public void setOrientation(int orientation) {
//        orientation += 90;
        try {
            if (mOrientation == orientation) return;

            // Rotate the pictures accordingly (display is kept at 90 degrees)
            Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
//        orientation = (360 - orientation + 45) / 90 * 90;
//        Log.d(TAG, "setOrientation: orientation after stuff: " + orientation);
            int rotation = 0;
//        Log.d(TAG, "setOrientation: camerainfo orientation: " + info.orientation);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + orientation) % 360;
            }
            //for video we do need to set the rotation
            if (mCamera != null) {
                synchronized (syncObject) {
                    mOrientation = orientation;
                    try {
                        mParameters.setRotation(rotation);
                        mCamera.setParameters(mParameters);
                        Log.d(TAG, "setOrientation: camera rotation set : info.orientation(" + info.orientation + ") + orientation(" + orientation + ") %360 = " + rotation);
                    } catch (Exception e) {
                        mOrientation = -1;
                        Log.d(TAG, "setOrientation: rotation is" + rotation + "  ,error: " + Log.getStackTraceString(e));
                    }
                }
            } else {
                Log.w(TAG, "setOrientation: camera is null");
            }
        } catch (RuntimeException e) {
            Log.d(TAG, "setOrientation: exception: " + e.getLocalizedMessage());
        }
        updateDisplayOrientation();
    }

    public void restartPreviewIfNeeded() {
        mCameraHandler.post(new Runnable() {
            public void run() {
                try {
                    // Normally, we should use safeStartPreview everywhere. However, some
                    // cameras implicitly stops preview, and we don't know. So we just force
                    // it here.
                    if (mCamera != null) {
                        mCamera.startPreview();
                    } else {
                        Log.w(TAG, "restartPreviewIfNeeded: camera is null");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "restartPreviewIfNeeded: " + Log.getStackTraceString(e));
                }

                mIsPreviewStarted = true;
            }
        });
    }

    /**
     * Updates the orientation of the display
     */
    public void updateDisplayOrientation() {
        try {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
            int degrees = Utils.getDisplayRotation(mContext);

            int result = (info.orientation - degrees + 360) % 360;
            if (mCamera != null) {
                switch (result) {
                    case 90:
                        mOrientationType = 1;
                        break;
                    case 180:
                        mOrientationType = 3;
                        break;
                    case 270:
                        mOrientationType = 2;
                        break;
                    case 0:
                        mOrientationType = 0;
                        break;
                }
                mCamera.setDisplayOrientation(result);
                Log.d(TAG, "updateDisplayOrientation: setting " + result + " to preview");
            } else {
                Log.w(TAG, "updateDisplayOrientation: camera is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "updateDisplayOrientation: " + e.getLocalizedMessage());
        }
    }

    /**
     * Trigger the autofocus function of the device
     *
     * @param cb The AF callback
     * @return true if we could start the AF, false otherwise
     */
    public boolean doAutofocus(final AutoFocusCallback cb) {
        if (mCamera != null) {
            try {
                // Trigger af
                mCamera.cancelAutoFocus();

                mHandler.post(new Runnable() {
                    public void run() {
                        try {
                            mCamera.autoFocus(cb);
                        } catch (Exception e) {
                            // Do nothing here
                        }
                    }
                });

            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
        mAutoFocusMoveCallback = cb;

        List<String> focusModes = mParameters.getSupportedFocusModes();
        if (mCamera != null && focusModes != null
                && (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)
                || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))) {
            try {
                mCamera.setAutoFocusMoveCallback(cb);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to set AutoFocusMoveCallback", e);
            }
        }
    }

    /**
     * returns whether it is enabled or not
     *
     * @param enable
     * @return
     */
    public boolean enableHDR(boolean enable) {//todo hardcoded hdr disabled
        if (enable) {
            if (mParameters.getSceneMode().equals(Camera.Parameters.SCENE_MODE_HDR)) {
                return true;
            }
            synchronized (syncObject) {
                if (mParameters.getSupportedSceneModes() != null) {
                    if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_HDR) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        mSceneMode = Camera.Parameters.SCENE_MODE_HDR;
                    } else {
                        return false;
                    }
                    mParameters.setSceneMode(mSceneMode);
                    mCamera.setParameters(mParameters);
                    Log.d(TAG, "enableHDR: true");
                    return true;
                }
            }
        } else {
            if (!mParameters.getSceneMode().equals(Camera.Parameters.SCENE_MODE_HDR)) {
                return false;
            }
            synchronized (syncObject) {
                if (mParameters.getSupportedSceneModes() != null) {
                    if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                        mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
                    }
                    mParameters.setSceneMode(mSceneMode);
                    mCamera.setParameters(mParameters);
                    Log.d(TAG, "enableHDR: false");
                }
            }
        }
        return false;
    }

    private void createTexture() {
        int[] texture = new int[1];
        texture[0] = 26;
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);

        mSurfaceTexture = new SurfaceTexture(texture[0]);
    }

    public void cancelAutoFocus() {
        if (mCamera != null) {
            try {
                // cancel af
                mCamera.cancelAutoFocus();
            } catch (Exception e) {
                Log.w(TAG, "cancelAutoFocus: " + Log.getStackTraceString(e));
            }
        }
    }

    public void setParameters(Camera.Parameters params) {
        if (mCamera != null) {
            synchronized (syncObject) {
                try {
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    Log.w(TAG, "setParameters: " + Log.getStackTraceString(e));
                }
            }
        }
    }


    /**
     * The CameraPreview class handles the Camera preview feed
     * and setting the surface holder.
     */
    public class CameraPreview implements Camera.PreviewCallback {
        private final static String TAG = "CameraPreview";

        private byte[] mLastFrameBytes;

        private byte[] mPreviousFrameBytes;

        public CameraPreview() {

        }

        public void notifyPreviewSize(int width, int height) {
            if ((mLastFrameBytes != null && mLastFrameBytes.length == (height + height / 2) * width) && (mPreviousFrameBytes != null && mPreviousFrameBytes.length == (height +
                    height / 2) * width)) {
                return;
            }
            mLastFrameBytes = new byte[(height + height / 2) * width];
            mPreviousFrameBytes = new byte[(height + height / 2) * width];
            // Update preview aspect ratio
        }

        public void notifyCameraChanged(final boolean startPreview) {
            synchronized (syncObject) {
                if (mCamera != null) {
                    if (startPreview) {
                        safeStopPreview();
                    }

                    try {
                        mCamera.setParameters(mParameters);

                        postCallbackBuffer();
                    } catch (Exception e) {
                        Log.e(TAG, "Could not set device specifics");

                    }

                    try {
                        if (mSurfaceTexture == null) {
                            createTexture();
                            Log.d(TAG, "Texture is null, temporary texture will not be visible");
                        } else {
                            Log.d(TAG, "Using existing texture");
                        }
                        mCamera.setPreviewTexture(mSurfaceTexture);

                        if (startPreview) {
                            updateDisplayOrientation();
                            safeStartPreview();
                            postCallbackBuffer();
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Cannot set preview texture", e);
                    } catch (IOException e) {
                        Log.e(TAG, "Error setting camera preview", e);
                        forceCloseCamera();
                        open();
                        restartPreviewIfNeeded();
                    }
                } else {
                    mTriedApplyingCam++;
                    if (mTriedApplyingCam < 8)
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Trying to apply texture but camera is null... ");
                                notifyCameraChanged(true);
                            }
                        }, 200);
                }
            }
        }

        public void postCallbackBuffer() {
            try {
                if (mCamera != null) {
                    mCamera.addCallbackBuffer(mLastFrameBytes);
                    mCamera.addCallbackBuffer(mPreviousFrameBytes);
                    mCamera.setPreviewCallbackWithBuffer(null);//todo sensorlib
                }
            } catch (Exception e) {
                Log.d(TAG, "postCallbackBuffer: " + e.getLocalizedMessage());
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mCamera != null) {
//                if (mRunDetection && mSensorLib != null) {
//                    mSensorLib.processFrame(data, previewWidth, previewHeight, mOrientationType);
//                    Log.d(TAG, "onPreviewFrame: processed  w " + previewFrameWidth + " h " + previewFrameHeight);
//                }
                mCamera.addCallbackBuffer(data);
            }
        }
    }

    public class CameraRenderer implements GLSurfaceView.Renderer {
        // Number of coordinates per vertex in this array
        static final int COORDS_PER_VERTEX = 2;

        private final static String VERTEX_SHADER =
                "attribute vec4 vPosition;\n" +
                        "attribute vec2 a_texCoord;\n" +
                        "varying vec2 v_texCoord;\n" +
                        "uniform mat4 u_xform;\n" +
                        "void main() {\n" +
                        "  gl_Position = vPosition;\n" +
                        "  v_texCoord = vec2(u_xform * vec4(a_texCoord, 1.0, 1.0));\n" +
                        "}\n";

        private final static String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "uniform samplerExternalOES s_texture;\n" +
                        "varying vec2 v_texCoord;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(s_texture, v_texCoord);\n" +
                        "}\n";

        private final float[] mTransformMatrix;

        private final float mTextureVertices[] =
                {1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};

        int mTexture;

        private FloatBuffer mVertexBuffer, mTextureVerticesBuffer;

        private int mProgram;

        private int mPositionHandle;

        private int mTextureCoordHandle;

        private int mTransformHandle;

        private float mSquareVertices[];


        public CameraRenderer() {
            mTransformMatrix = new float[16];
        }

        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            createTexture();
            mPreview.notifyCameraChanged(true);
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

            Log.d(TAG, "onSurfaceCreated: CameraPreview");

            ByteBuffer bb2 = ByteBuffer.allocateDirect(mTextureVertices.length * 4);
            bb2.order(ByteOrder.nativeOrder());
            mTextureVerticesBuffer = bb2.asFloatBuffer();
            mTextureVerticesBuffer.put(mTextureVertices);
            mTextureVerticesBuffer.position(0);

            // Load shaders
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

            mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
            GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
            GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
            GLES20.glLinkProgram(mProgram);

            // Since we only use one program/texture/vertex array, we bind them once here
            // and then we only draw what we need in onDrawFrame
            GLES20.glUseProgram(mProgram);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);

            // Setup vertex buffer. Use a default 4:3 ratio, this will get updated once we have
            // a preview aspect ratio.
            // Regenerate vertexes
            mSquareVertices = new float[]{1.0f * 1.0f,
                    1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f,
                    1.0f * 1.0f, -1.0f};

            ByteBuffer bb = ByteBuffer.allocateDirect(mSquareVertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            mVertexBuffer = bb.asFloatBuffer();
            mVertexBuffer.put(mSquareVertices);
            mVertexBuffer.position(0);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                    false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(mPositionHandle);

            mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
            GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                    false, 0, mTextureVerticesBuffer);
            GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

            mTransformHandle = GLES20.glGetUniformLocation(mProgram, "u_xform");
            if (mCamera == null) {
                forceCloseCamera();
                open();
                restartPreviewIfNeeded();
            }
        }

        public void onDrawFrame(GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            try {
                if (mSurfaceTexture != null) {
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTexture.getTransformMatrix(mTransformMatrix);
                    GLES20.glUniformMatrix4fv(mTransformHandle, 1, false, mTransformMatrix, 0);
                }

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
            } catch (Exception e) {
                Log.d(TAG, "onDrawFrame: skipping, frame not drawn");
            }
        }

        public void onSurfaceChanged(GL10 unused, int width, int height) {
            updateDisplayOrientation();
            Point point = new Point();
            point.y = height;
            point.x = width;
            float ratio = (float) point.x / (float) previewHeight;
            int widthDiff = (int) (point.x - previewHeight * ratio);
            if (widthDiff < 0 || Math.max(point.y, point.x) / Math.min(point.y, point.x) > 1.5) {
                widthDiff = 0;
            }
            if (width > height) {
                ratio = (float) point.x / (float) previewWidth;
                GLES20.glViewport(0, 0, (int) (previewWidth * ratio), (int) (previewHeight * ratio));
                Log.d(TAG, "onSurfaceChanged, screen: " + point.x + "x" + point.y + ", previewWidth: " + previewWidth + "x" + previewHeight + ", Ratio=" + ratio);
            } else {
                GLES20.glViewport(widthDiff / 2, 0, (int) (previewHeight * ratio), (int) (previewWidth * ratio));
                Log.d(TAG, "onSurfaceChanged, screen: " + point.x + "x" + point.y + ", previewWidth: " + previewWidth + "x" + previewHeight + ", Ratio=" + ratio);
            }
        }


        public int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            return shader;
        }
    }
}
