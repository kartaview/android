package com.telenav.osv.manager.capture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.OrientationEventListener;
import android.view.Surface;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.CameraResetCommand;
import com.telenav.osv.command.SignDetectInitCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.event.ui.OrientationChangedEvent;
import com.telenav.osv.utils.CameraParamParser;
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
@SuppressWarnings("deprecation")
public abstract class CameraManager extends FocusManager implements Camera.ErrorCallback {
    private final static String TAG = "CameraManager";

    private static final int TEXTURE_ID = 26;

    private final Object syncObject = new Object();

//    private SensorLib mSensorLib;

    private final OrientationEventListener mOrientationListener;

    protected ApplicationPreferences appPrefs;

    boolean mRunDetection = false;

    private int previewWidth;

    private int previewHeight;

    private int mOrientation = -1;

    private Context mContext;

    private boolean mIsPreviewStarted;

    private String mSceneMode;

    private String mWhiteBalanceMode;

    private HandlerThread mCameraThread;

    private Handler mCameraHandler;

    private List<Camera.Size> supportedPicturesSizes;

    private Handler mOpenHandler;

    private Camera.Size sixteen = null;

    private Camera.Size twelve = null;

//    private SignDetectedListener mSignDetectedListener;

    private Camera.Size eight = null;

    private Camera.Size five = null;

    private boolean mOpening = false;

    private SurfaceTexture mTexture;

    CameraManager(Context context) {
        super();
        Log.d(TAG, "CameraManager: Created camera manager");
        mContext = context;
        mOrientationListener = new OrientationListener(context);
        mIsPreviewStarted = false;
        // Try to open the camera

        appPrefs = new ApplicationPreferences(mContext);
        HandlerThread mOpenThread = new HandlerThread("OpenCamera", Process.THREAD_PRIORITY_FOREGROUND);
        // Try to open the camera
        mOpenThread.start();
        mOpenHandler = new Handler(mOpenThread.getLooper());
        mCameraThread = new HandlerThread("Camera", Process.THREAD_PRIORITY_FOREGROUND);
        // Try to open the camera
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        initSensorLib();
    }

    /**
     * Opens the camera and show its preview in the preview
     */
    protected void open() {
        if (mOpening) {
            return;
        }
        mOpening = true;
        Log.d(TAG, "open: Camera");
        if (mCamera != null) {

            // Close the previous camera
            releaseCamera();
        }
        mOrientationListener.enable();
        sendOrientation();
        Log.d(TAG, "open: orientationListener enabled");
        mOpenHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (syncObject) {
                    try {
                        if (mCamera != null) {
                            forceCloseCamera();
                            Log.e(TAG, "Previous camera not closed! Not opening");
                        }
                        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                        mCamera.setErrorCallback(CameraManager.this);
                        Log.v(TAG, "Camera is open");
                        try {
                            if (Build.VERSION.SDK_INT >= 17) {
                                mCamera.enableShutterSound(false);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "open: enable shuttersound failed.");
                        }
                        mParameters = mCamera.getParameters();
                        mParameters.setPictureFormat(ImageFormat.JPEG);
                        mParameters.setJpegQuality(100);
                        List<int[]> list = mParameters.getSupportedPreviewFpsRange();
                        int[] fpsRange = list.get(list.size() - 1);
                        mParameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
                        supportedPicturesSizes = getOptimalPictureSize(mParameters.getSupportedPictureSizes());
                        setSupportedPicturesSizesPreferences();
                        Log.d(TAG, " resolutionWidth: " + appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) + " resolutionHeight : " + appPrefs.getIntPreference
                                (PreferenceTypes.K_RESOLUTION_HEIGHT));
                        if (appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
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
//                            if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_HDR) && Utils.DEBUG && false) {//todo hardcoded removal of hdr
//                                mSceneMode = Camera.Parameters.SCENE_MODE_HDR;
//                                Log.d(TAG, "enableHDR: true");
//                            } else
                            if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                                mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_DEBUG_HDR, false);
                                Log.d(TAG, "enableHDR: false");
                            }
                            mParameters.setSceneMode(mSceneMode);

                        }
                        if (mParameters.getFlashMode() != null) {
                            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }

//                        int min = mParameters.getMinExposureCompensation();
//                        int max = mParameters.getMaxExposureCompensation();
//                        float step = mParameters.getExposureCompensationStep();
//                        //need to setExposureCompensation??
//                        mParameters.setExposureCompensation(max);
                        if (mParameters.getSupportedWhiteBalance() != null && mParameters.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                            mWhiteBalanceMode = Camera.Parameters.WHITE_BALANCE_AUTO;
                            mParameters.setWhiteBalance(mWhiteBalanceMode);
                        }
                        setAutoFocusMoveCallback(mParameters);
                        setFocusMode();
                        mCamera.setParameters(mParameters);
                        CameraParamParser.parse(mParameters.flatten());

                    } catch (Exception e) {
                        Log.e(TAG, "Error while opening camera: " + e.getMessage(), e);
                        EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_FAILED));
                        mOrientationListener.disable();
                        mOpening = false;
                        return;
                    }
                    EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_READY, previewWidth, previewHeight));
                    if (mTexture != null) {
                        setCameraPreviewSurface(mTexture);
                    }
                }
                if (mOrientation != -1) {
                    int temp = mOrientation;
                    mOrientation = -1;
                    setOrientation(temp);
                }
                mOpening = false;
            }
        });
    }

    private void sendOrientation() {
        int rotation = Utils.getScreenOrientation(mContext);
        switch (rotation) {
            case Surface.ROTATION_0:
                mOrientationListener.onOrientationChanged(0);
                break;
            case Surface.ROTATION_90:
                mOrientationListener.onOrientationChanged(90);
                break;
            case Surface.ROTATION_180:
                mOrientationListener.onOrientationChanged(180);
                break;
            case Surface.ROTATION_270:
                mOrientationListener.onOrientationChanged(270);
                break;
            default:
                mOrientationListener.onOrientationChanged(0);
                break;
        }
    }

    public List<Camera.Size> getSupportedPicturesSizes() {
        if (mParameters != null) {
            supportedPicturesSizes = getOptimalPictureSize(mParameters.getSupportedPictureSizes());
        }
        return supportedPicturesSizes;
    }

    /**
     * This method chooses the best resolution for the taken picture below 16mp
     * @param supportedPictureSizes sizes from api
     * @return the selected size
     */
    private List<Camera.Size> getOptimalPictureSize(List<Camera.Size> supportedPictureSizes) {
        List<Camera.Size> relevantSizesList = new ArrayList<>();

//        for (Camera.Size size: supportedPictureSizes){
//            Log.d(TAG, "resolution: " + size.width + " x " + size.height);
//        }
        int fiveMpLimit = (1948 * 2596) + 20;
        int eightMpLimit = (3840 * 2160) + 20;
        int twelveMpLimit = (3024 * 4032) + 20;
        int sixteenMpLimit = (2988 * 5312) + 20;

        if (appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_AUTO_SHUTTER)) {
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
     * @param supportedSizes the sizes from the api
     * @return proper size
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

//    public void startDetection() {
//        mRunDetection = true;
//    }

//    public void setSignDetectedListener(SignDetectedListener listener) {
////        if (mSensorLib != null) {
////            mSignDetectedListener = listener;
////            mSensorLib.setOnSignDetectedListener(mSignDetectedListener);
////        }
//    }

//    public void stopDetection() {
//        mRunDetection = false;
//    }

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

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onDetectionInit(SignDetectInitCommand command) {
        if (command.initialization) {
            initSensorLib();
        } else {
            destroySensorLib();
        }
    }

    private void destroySensorLib() {
//        mOpenHandler.post(new Runnable() {
//            @Override
//            public void run() {
//        if (mSensorLib != null) {
//            mSensorLib.destroy();
//            mSensorLib = null;
//        }
//            }
//        });
    }

    private void initSensorLib() {
//        mOpenHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    final boolean signDetectionActivated = appPrefs.getBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_ENABLED);
//
//                    if (signDetectionActivated) {
//                        mSensorLib = new SensorLib(mContext);
//                        mSensorLib.setSensorLibSettings(new SensorLibSettings(10, 10));
//                        if (mSignDetectedListener != null) {
//                            mSensorLib.setOnSignDetectedListener(mSignDetectedListener);
//                        }
//                    }
//
//                } catch (ExceptionInInitializerError e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    /**
     * Used by OSVApplication safeguard to release the camera when the app crashes.
     */
    public void forceCloseCamera() {
        if (mCamera != null) {
            try {
                mOrientationListener.disable();
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
                mCamera = null;
                mParameters = null;
            } catch (Exception e) {
                // Do nothing
            }
        }
    }

    protected void releaseCamera() {
        try {
            if (mCamera != null) {
                mOrientationListener.disable();
                Log.v(TAG, "Releasing camera");
                safeStopPreview();
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
                mCamera = null;
                mParameters = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "releaseCamera: " + Log.getStackTraceString(e));
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onResetNeeded(CameraResetCommand command) {
        forceCloseCamera();
        open();
        restartPreviewIfNeeded();
    }

    private void safeStartPreview() {
        try {
            if (!mIsPreviewStarted && mCamera != null) {
                Log.d(TAG, "safeStartPreview");
                mCamera.startPreview();
                mIsPreviewStarted = true;
            }
        } catch (Exception e) {
            Log.w(TAG, "safeStartPreview: " + e.getLocalizedMessage());
        }
    }

    private void safeStopPreview() {
        try {
            if (mIsPreviewStarted && mCamera != null) {
                Log.d(TAG, "safeStopPreview");
                mCamera.stopPreview();
                mCamera.setPreviewTexture(null);
                Log.d(TAG, "safeStopPreview: kali destroyed texture");
                mIsPreviewStarted = false;
            }
        } catch (Exception e) {
            Log.w(TAG, "safeStopPreview: " + e.getLocalizedMessage());
        }
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Takes a snapshot
     */
    boolean takeSnapshot(final Camera.ShutterCallback shutterCallback,
                         final Camera.PictureCallback jpeg) {

        Log.v(TAG, "takePicture: entered");
        if (mCamera != null) {
            if (mCameraHandler == null || !mCameraHandler.getLooper().getThread().isAlive() || mCameraHandler.getLooper().getThread().isInterrupted()) {
                mCameraThread = new HandlerThread("Camera", Process.THREAD_PRIORITY_FOREGROUND);
                // Try to open the camera
                mCameraThread.start();
                Log.d(TAG, "takeSnapshot: starting new thread for background operation");
                mCameraHandler = new Handler(mCameraThread.getLooper());
            }
            mCameraHandler.post(new Runnable() {
                public void run() {
                    Log.d(TAG, "takePicture: before synchronize");
                    synchronized (syncObject) {
                        try {
                            Log.d(TAG, "takePicture: trying to take a picture...");
                            mCamera.takePicture(shutterCallback, null, new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] bytes, Camera camera) {
                                    Log.d(TAG, "onPictureTaken: callback called");
                                    checkFocusManual();
                                    if (jpeg != null) {
                                        jpeg.onPictureTaken(bytes, camera);
                                    }
                                }
                            });
                            Log.d(TAG, "takePicture: success");
                        } catch (RuntimeException e) {
                            Log.e(TAG, "takePicture: Unable to take picture", e);
                            if (e.getLocalizedMessage().contains("error=-38")) {
                                Log.e(TAG, "takePicture: Unable to take picture during debug", e);
                            } else {
                                if (!mOpening) {
                                    forceCloseCamera();
                                    open();
                                    restartPreviewIfNeeded();
                                }
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
     * @param orientation The orientation, in degrees
     */
    private void setOrientation(int orientation) {
//        orientation += 90;
        try {
            if (mOrientation == orientation) return;

            // Rotate the pictures accordingly (display is kept at 90 degrees)
            Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
//        orientation = (360 - orientation + 45) / 90 * 90;
//        Log.d(TAG, "setOrientation: orientation after stuff: " + orientation);
            int rotation;
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
                        mCamera.setDisplayOrientation(rotation);
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
            Log.w(TAG, "setOrientation: exception: " + Log.getStackTraceString(e));
        }
    }

//    /**
//     * returns whether it is enabled or not
//     * @param enable
//     * @return
//     */
//    public boolean enableHDR(boolean enable) {//todo hardcoded hdr disabled
//        if (enable) {
//            if (mParameters.getSceneMode().equals(Camera.Parameters.SCENE_MODE_HDR)) {
//                return true;
//            }
//            synchronized (syncObject) {
//                if (mParameters.getSupportedSceneModes() != null) {
//                    if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_HDR) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                        mSceneMode = Camera.Parameters.SCENE_MODE_HDR;
//                    } else {
//                        return false;
//                    }
//                    mParameters.setSceneMode(mSceneMode);
//                    mCamera.setParameters(mParameters);
//                    Log.d(TAG, "enableHDR: true");
//                    return true;
//                }
//            }
//        } else {
//            if (!mParameters.getSceneMode().equals(Camera.Parameters.SCENE_MODE_HDR)) {
//                return false;
//            }
//            synchronized (syncObject) {
//                if (mParameters.getSupportedSceneModes() != null) {
//                    if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
//                        mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
//                    }
//                    mParameters.setSceneMode(mSceneMode);
//                    mCamera.setParameters(mParameters);
//                    Log.d(TAG, "enableHDR: false");
//                }
//            }
//        }
//        return false;
//    }

    void restartPreviewIfNeeded() {
        mCameraHandler.post(new Runnable() {
            public void run() {
                try {
                    // Normally, we should use safeStartPreview everywhere. However, some
                    // cameras implicitly stops preview, and we don't know. So we just force
                    // it here.
                    if (mCamera != null) {
                        Log.d(TAG, "restartPreviewIfNeeded: ");
                        mCamera.startPreview();
                    } else {
                        Log.w(TAG, "restartPreviewIfNeeded: camera is null");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "restartPreviewIfNeeded: " + e.getLocalizedMessage());
//                    restartPreviewIfNeeded();
                    return;
                }

                mIsPreviewStarted = true;
            }
        });
    }

    public boolean setCameraPreviewSurface(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            surfaceTexture = createTexture();
        }
        synchronized (syncObject) {
            try {
                if (mCamera != null) {
                    try {
                        safeStopPreview();
                        mCamera.setPreviewTexture(surfaceTexture);
                        mTexture = surfaceTexture;
                    } catch (IOException e) {
                        Log.w(TAG, "setCameraPreviewSurface: " + e.getLocalizedMessage());
                        return false;
                    }
                }
                safeStartPreview();
            } catch (Exception e) {
                Log.e(TAG, "setCameraPreviewSurface: second catch " + Log.getStackTraceString(e));
            }
        }
        return true;
    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "onError: " + error + " on camera " + camera);
        switch (error) {
            case 100:
            default:
                synchronized (syncObject) {
                    releaseCamera();
                    open();
                }
                break;
        }
    }

    private SurfaceTexture createTexture() {
        int[] texture = new int[1];
        texture[0] = TEXTURE_ID;
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, TEXTURE_ID);

        return new SurfaceTexture(TEXTURE_ID);

    }

    private class OrientationListener extends OrientationEventListener {

        private FixedQueue queue;

        OrientationListener(Context context) {
            super(context);
            EventBus.register(this);
        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onConfigChanged(OrientationChangedEvent event) {
            if (queue != null) {
                int orientation = queue.get(queue.size() - 1);
                Log.d(TAG, "onConfigChanged: setting orientation " + orientation);
                setOrientation(orientation);
            }
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            final int value = Utils.roundOrientation(orientation, mOrientation);

            if (queue == null) {
                queue = new FixedQueue(new Integer[]{value, value, value, value, value, value});
                setOrientation(value);
            }
            queue.offer(value);
            if (value == mOrientation) {
                return;
            }
            for (int i = 0; i < queue.size(); i++) {
                if (value != queue.get(i)) {
                    Log.d(TAG, "onOrientationChanged: not valid");
                    return;
                }
            }
            Log.d(TAG, "onOrientationChanged: valid orientation: " + value);
            setOrientation(value);
        }
    }

    private class FixedQueue {

        protected int index;

        Integer[] ring;

        /**
         * @param initialValues contains the ring's initial values.
         * The "oldest" value in the queue is expected to reside in
         * position 0, the newest one in position length-1.
         */
        FixedQueue(Integer[] initialValues) {
            // This is a little ugly, but there are no
            // generic arrays in Java
            ring = new Integer[initialValues.length];

            // We don't want to work on the original data
            System.arraycopy(initialValues, 0, ring, 0, initialValues.length);

            // The next time we add something to the queue,
            // the oldest element should be replaced
            index = 0;
        }

        public boolean add(Integer newest) {
            return offer(newest);
        }

        boolean offer(Integer newest) {
            ring[index] = newest;
            incrIndex();
            return true;
        }

        public int size() {
            return ring.length;
        }

        public Integer get(int absIndex) throws IndexOutOfBoundsException {
            if (absIndex >= ring.length) {
                throw new IndexOutOfBoundsException("Invalid index " + absIndex);
            }
            int i = index + absIndex;
            if (i >= ring.length) {
                i -= ring.length;
            }
            return ring[i];
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = index, n = 0; n < ring.length; i = nextIndex(i), n++) {
                sb.append(ring[i]);
                if (n + 1 < ring.length) { sb.append(", "); }
            }
            return sb.append("]").toString();
        }

        void incrIndex() {
            index = nextIndex(index);
        }

        int nextIndex(int current) {
            if (current + 1 >= ring.length) { return 0; } else return current + 1;
        }
    }
}
