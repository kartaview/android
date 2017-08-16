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
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.Surface;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.CameraConfigChangedCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.CameraInfoEvent;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.listener.ImageReadyCallback;
import com.telenav.osv.listener.ShutterCallback;
import com.telenav.osv.utils.CameraParamParser;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;

/**
 * This class is responsible for interacting with the Camera hardware.
 * It provides easy open/close and other interactions with camera
 * Created by Kalman on 10/7/2015.
 */
public class CameraManagerOld extends CameraManager implements Camera.ErrorCallback, Camera.AutoFocusCallback, Camera.AutoFocusMoveCallback {
    private final static String TAG = "CameraManagerOld";

    private static final int TEXTURE_ID = 26;

    // Miliseconds during which we assume the focus is good
    private static final int FOCUS_KEEP_TIME = 30000;

    private static final int FOCUS_MODE_STATIC = 0;

    private static final int FOCUS_MODE_DYNAMIC = 1;

//    private SensorLib mSensorLib;

    private static boolean mIsFocusing = false;

    private final Object syncObject = new Object();

    protected ApplicationPreferences appPrefs;

    private int previewWidth;

    private int previewHeight;

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

    private android.hardware.Camera mCamera;

    private Camera.Parameters mParameters;

    private boolean mContinuousSupported = false;

    private long mLastFocusTimestamp = 0;

    private boolean notFocused = true;

    private int focusRetryCount;

    private int mFocusMode = FOCUS_MODE_DYNAMIC;

    CameraManagerOld(Context context) {
        super(context);
        Log.d(TAG, "CameraManager: Created camera manager");
        mContext = context;
        mIsFocusing = false;
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
        previewWidth = appPrefs.getIntPreference(PreferenceTypes.K_PREVIEW_WIDTH, 1600);
        previewHeight = appPrefs.getIntPreference(PreferenceTypes.K_PREVIEW_HEIGHT, 1200);
        EventBus.postSticky(new CameraInfoEvent(previewWidth, previewHeight));
    }

    /**
     * Opens the camera and show its preview in the preview
     */
    public void open() {
        if (mOpening) {
            return;
        }
        mOpening = true;
        Log.d(TAG, "open: Camera");
        if (mCamera != null) {

            // Close the previous camera
            release();
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
                        mCamera.setErrorCallback(CameraManagerOld.this);
                        Log.v(TAG, "Camera is open");
                        try {
                            if (Build.VERSION.SDK_INT >= 17) {
                                mCamera.enableShutterSound(false);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "open: enable shuttersound failed.");
                        }
                        Camera.CameraInfo info =
                                new android.hardware.Camera.CameraInfo();
                        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
                        mCamera.setDisplayOrientation(info.orientation);
                        mParameters = mCamera.getParameters();
                        mParameters.setPictureFormat(ImageFormat.JPEG);
                        mParameters.setJpegQuality(100);
                        List<int[]> list = mParameters.getSupportedPreviewFpsRange();
                        int[] fpsRange = list.get(list.size() - 1);
                        mParameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
                        supportedPicturesSizes = getOptimalPictureSize(mParameters.getSupportedPictureSizes());
                        setSupportedPicturesSizesPreferences();
                        Log.d(TAG, "saved resolutionWidth: " + appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) + " resolutionHeight : " + appPrefs.getIntPreference
                                (PreferenceTypes.K_RESOLUTION_HEIGHT));
                        mParameters.setPictureSize(appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH), appPrefs.getIntPreference(PreferenceTypes
                                .K_RESOLUTION_HEIGHT));
                        Log.d(TAG, " Resolution of the image is: " + mParameters.getPictureSize().width + " x " + mParameters.getPictureSize().height);
                        Camera.Size maxPreviewSize = getOptimalPreviewSize(mParameters.getSupportedPreviewSizes());
                        if (maxPreviewSize != null) {
                            mParameters.setPreviewSize(maxPreviewSize.width, maxPreviewSize.height);
                        }
                        previewWidth = mParameters.getPreviewSize().width;
                        previewHeight = mParameters.getPreviewSize().height;

                        appPrefs.saveIntPreference(PreferenceTypes.K_PREVIEW_WIDTH, previewWidth);
                        appPrefs.saveIntPreference(PreferenceTypes.K_PREVIEW_HEIGHT, previewHeight);
                        if (mParameters.getSupportedSceneModes() != null) {
                            if (mParameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                                mSceneMode = Camera.Parameters.SCENE_MODE_AUTO;
                                Log.d(TAG, "enableHDR: false");
                            }
                            mParameters.setSceneMode(mSceneMode);

                        }
                        if (mParameters.getFlashMode() != null) {
                            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }

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
                    EventBus.postSticky(new CameraInfoEvent(previewWidth, previewHeight));
                    EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_READY, previewWidth, previewHeight));
                    if (mTexture != null) {
                        setPreviewSurface(mTexture);
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


    @Override
    public List<Size> getSupportedPictureSizes() {
        if (mParameters != null) {
            supportedPicturesSizes = getOptimalPictureSize(mParameters.getSupportedPictureSizes());
        }
        ArrayList<Size> list = new ArrayList<>(supportedPicturesSizes.size());
        for (Camera.Size size : supportedPicturesSizes) {
            list.add(new Size(size.width, size.height));
        }
        return list;
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


    private void setParameters(Camera.Parameters params) {
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
        EventBus.clear(CameraInitEvent.class);
        EventBus.clear(CameraInfoEvent.class);
    }

    @Override
    public void release() {
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
        EventBus.clear(CameraInitEvent.class);
        EventBus.clear(CameraInfoEvent.class);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onCameraConfigChanged(CameraConfigChangedCommand command) {
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
            Log.w(TAG, "safeStartPreview: " + Log.getStackTraceString(e));
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
            Log.w(TAG, "safeStopPreview: " + Log.getStackTraceString(e));
        }
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Takes a snapshot
     */
    @Override
    public void takeSnapshot(final ShutterCallback shutterCallback, final ImageReadyCallback jpeg,
                             final long timestamp, final int sequenceId, final String folderPath, final Location location) {

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
                            mCamera.takePicture(new Camera.ShutterCallback() {
                                @Override
                                public void onShutter() {
                                    shutterCallback.onShutter();
                                }
                            }, null, new Camera.PictureCallback() {
                                @Override
                                public void onPictureTaken(byte[] bytes, Camera camera) {
                                    Log.d(TAG, "onPictureTaken: callback called");
                                    restartPreviewIfNeeded();
                                    if (jpeg != null) {
                                        jpeg.onPictureTaken(bytes, timestamp, sequenceId, folderPath, location);
                                    }
                                    checkFocusManual();
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
        } else {
            Log.w(TAG, "takePicture: camera is null");
            forceCloseCamera();
            open();
        }
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
    protected void setOrientation(int orientation) {
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
//                        mCamera.setDisplayOrientation(rotation);
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

    private void restartPreviewIfNeeded() {
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
                    Log.w(TAG, "restartPreviewIfNeeded: " + Log.getStackTraceString(e));
//                    restartPreviewIfNeeded();
                    return;
                }

                mIsPreviewStarted = true;
            }
        });
    }

    @Override
    public void setPreviewSurface(SurfaceTexture surfaceTexture) {
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
                    }
                }
                safeStartPreview();
            } catch (Exception e) {
                Log.e(TAG, "setCameraPreviewSurface: second catch " + Log.getStackTraceString(e));
            }
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "onError: " + error + " on camera " + camera);
        switch (error) {
            case 100:
            default:
                synchronized (syncObject) {
                    release();
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

    private void setFocusMode() {
        if (mParameters != null) {
            boolean supportsInfinity = mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY);
            if (!supportsInfinity) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC, false);
            }
            boolean useInfinityFocus = appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC);
            if (useInfinityFocus && supportsInfinity) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                mFocusMode = FOCUS_MODE_STATIC;
            } else {
                mContinuousSupported = mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// && !Build.MODEL.contains("SM-G900");
                if (mContinuousSupported) {
                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else {
                    if (mParameters.getSupportedFocusModes().contains("auto")) {
                        mParameters.setFocusMode("auto");
                    }

                    // Do a first focus after 1 second
                    mCameraHandler.postDelayed(new Runnable() {
                        public void run() {
                            checkFocusManual();
                        }
                    }, 1000);
                }
                mFocusMode = FOCUS_MODE_DYNAMIC;
            }
        }
    }


//    =========================================================================

    private void setAutoFocusMoveCallback(Camera.Parameters params) {
        if (mFocusMode == FOCUS_MODE_DYNAMIC) {
            List<String> focusModes = params.getSupportedFocusModes();
            if (mCamera != null && focusModes != null
                    && (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)
                    || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                    || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))) {
                try {
                    mCamera.setAutoFocusMoveCallback(this);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Unable to set AutoFocusMoveCallback", e);
                }
            }
        }
    }

    /**
     * Trigger the autofocus function of the device
     * @param cb The AF callback
     * @return true if we could start the AF, false otherwise
     */
    private boolean doAutofocus(final Camera.AutoFocusCallback cb) {
        if (mFocusMode == FOCUS_MODE_DYNAMIC) {
            if (mCamera != null) {
                try {
                    // Trigger af
                    mCamera.cancelAutoFocus();

                    mCameraHandler.post(new Runnable() {
                        public void run() {
                            try {
                                mCamera.autoFocus(cb);
                            } catch (Exception e) {
                                Log.d(TAG, "doAutofocus: " + Log.getStackTraceString(e));
                                // Do nothing here
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.d(TAG, "doAutofocus: " + Log.getStackTraceString(e));
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void checkFocusManual() {
        if (mFocusMode == FOCUS_MODE_DYNAMIC) {
            if (mContinuousSupported) {
                return;
            }
            long time = System.currentTimeMillis();
            if ((notFocused || time - mLastFocusTimestamp > FOCUS_KEEP_TIME) && !mIsFocusing) {
                if (doAutofocus(this)) {
                    mIsFocusing = true;
                }
            }
        }
    }

//    private void lockFocus(boolean lock) {
//        if (mFocusMode == FOCUS_MODE_DYNAMIC) {
//            Log.d(TAG, "lockFocus: " + lock);
//            focusRetryCount = 0;
////        if (mIsLocked != lock) {
//            mIsLocked = lock;
//            if (lock) {
//                if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
//                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                }
//                setParameters(mParameters);
//            } else {
//                if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
//                    mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//                }
//                setParameters(mParameters);
//                if (mContinuousSupported) {
//                    setContinuousAfterFocusOk = true;
//                }
//            }
////        }
//        }
//    }


    @Override
    public void unlockFocus() {
        setFocusMode();
        setParameters(mParameters);
    }

    @Override
    protected void focus(Rect focusRect) {
        if (mFocusMode == FOCUS_MODE_DYNAMIC) {
            cancelAutoFocus();
            List<Camera.Area> focusList = new ArrayList<>();
            Camera.Area focusArea = new Camera.Area(focusRect, 1000);
            focusList.add(focusArea);

            if (mParameters.getMaxNumFocusAreas() > 0) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                if (mParameters.getFocusAreas() != null) {
                    mParameters.getFocusAreas().clear();
                }
                mParameters.setFocusAreas(focusList);
            }
//        if (mParameters.getMaxNumMeteringAreas() > 0) {
//            mParameters.setMeteringAreas(focusList);
//        }
            try {
                setParameters(mParameters);
                doAutofocus(this);
            } catch (Exception e) {
                Log.w(TAG, "Unable to focus: " + Log.getStackTraceString(e));
            }
        }
    }

    private void cancelAutoFocus() {
        if (mFocusMode == FOCUS_MODE_DYNAMIC) {
            if (mCamera != null) {
                try {
                    // cancel af
                    mCamera.cancelAutoFocus();
                } catch (Exception e) {
                    Log.w(TAG, "cancelAutoFocus: " + Log.getStackTraceString(e));
                }
            }
        }
    }


    @Override
    public void onAutoFocus(boolean success, Camera cam) {
        Log.d(TAG, "onAutoFocus: " + success);
        mLastFocusTimestamp = System.currentTimeMillis();
        mIsFocusing = false;
        notFocused = !success;
        if (!success) {
            if (focusRetryCount < 3) {
                doAutofocus(this);
                focusRetryCount = focusRetryCount + 1;
                Log.d(TAG, "onAutoFocus: retry autofocus " + focusRetryCount);
            } else {
                // if the focus is not true after 3 tries cancel the focus and switch to continuous focus
                unlockFocus();
            }
        }
    }

    @Override
    public void onAutoFocusMoving(boolean start, Camera cam) {
        if (mIsFocusing && !start) {
            // We were focusing, but we stopped, notify time of last focus
            mLastFocusTimestamp = System.currentTimeMillis();
        }
        mIsFocusing = start;
        Log.d(TAG, "onAutoFocusMoving: moving = " + start);
    }

}
