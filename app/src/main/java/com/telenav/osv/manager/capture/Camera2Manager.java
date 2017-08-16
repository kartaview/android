package com.telenav.osv.manager.capture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.CameraConfigChangedCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.CameraPermissionEvent;
import com.telenav.osv.event.hardware.camera.CameraInfoEvent;
import com.telenav.osv.event.hardware.camera.CameraInitEvent;
import com.telenav.osv.listener.ImageReadyCallback;
import com.telenav.osv.listener.ShutterCallback;
import com.telenav.osv.utils.Log;

/**
 * This class is responsible for interacting with the Camera hardware.
 * It provides easy open/close and other interactions with camera
 * Created by Kalman on 10/7/2015.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Manager extends CameraManager {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2Manager";

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId = "0";

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mCameraHandler;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private Size sixteen;

    private Size twelve;

    private Size eight;

    private Size five;

    private SurfaceTexture mTexture;

    private CaptureRequest.Builder mSnapShotRequestBuilder;

    private ArrayList<Size> mPictureResolutions;

    private ApplicationPreferences appPrefs;

    private int mSensorOrientation;

    private Semaphore mCameraWorkLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened: ");
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            setupCameraDevice(mCameraDevice);
            EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_READY, mPreviewSize.getWidth(), mPreviewSize.getHeight()));
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "onDisconnected: ");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            mOrientationListener.disable();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            switch (error) {
                case ERROR_CAMERA_IN_USE:
                    Log.e(TAG, "onError: ERROR_CAMERA_IN_USE");
                    break;
                case ERROR_MAX_CAMERAS_IN_USE:
                    Log.e(TAG, "onError: ERROR_MAX_CAMERAS_IN_USE");
                    break;
                case ERROR_CAMERA_DISABLED:
                    Log.e(TAG, "onError: ERROR_CAMERA_DISABLED");
                    break;
                case ERROR_CAMERA_DEVICE:
                    Log.e(TAG, "onError: ERROR_CAMERA_DEVICE");
                    break;
                case ERROR_CAMERA_SERVICE:
                    Log.e(TAG, "onError: ERROR_CAMERA_SERVICE");
                    break;
                default:
                    Log.e(TAG, "onError: " + error);
            }
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_FAILED));
            mOrientationListener.disable();
        }

    };

    private Rect mFocusRect;

    private boolean mSupportsTapToFocus = false;

    Camera2Manager(Context context) {
        super(context);
        appPrefs = new ApplicationPreferences(mContext);
        startBackgroundThread();
        readCameraCharacteristics();
    }

    private void setupCameraDevice(final CameraDevice camera) {
        Log.d(TAG, "setupCameraDevice: camera is " + camera);
        try {
            getOptimalPictureSize(mPictureResolutions);
            setSupportedPicturesSizesPreferences();
            int width = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH);
            int height = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT);
            Log.d(TAG, " resolutionWidth: " + width + " resolutionHeight : " + height);

            int maxImages;
            if (width * height > (3840 * 2160) + 20) {
                maxImages = 1;
            } else {
                maxImages = 2;
            }
            mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, maxImages);

            // We set up a CaptureRequest.Builder with the output Surface.
            mSnapShotRequestBuilder = createSnapshotTemplate();
            mSnapShotRequestBuilder.addTarget(mImageReader.getSurface());

            createNoPreviewSession();
        } catch (CameraAccessException e) {
            Log.w(TAG, "setupCameraDevice: " + Log.getStackTraceString(e));
        } catch (Exception e) {
            Log.d(TAG, "setupCameraDevice: " + Log.getStackTraceString(e));
        }
    }

    private void createNoPreviewSession() {
        if (mCameraDevice != null) {
            try {
                Log.d(TAG, "createNoPreviewSession mCameraWorkLock: acquired");
                mCameraWorkLock.acquire();
                mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                Log.d(TAG, "setPreviewSurface: onConfigured: ");
                                try {
                                    Log.d(TAG, "createNoPreviewSession onConfigured mCameraWorkLock: acquired");
                                    mCameraWorkLock.acquire();
                                    mCaptureSession = cameraCaptureSession;
                                } catch (InterruptedException e) {
                                    Log.d(TAG, "onConfigured: " + Log.getStackTraceString(e));
                                } finally {
                                    Log.d(TAG, "createNoPreviewSession onConfigured mCameraWorkLock: released");
                                    mCameraWorkLock.release();
                                }
                            }

                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                Log.d(TAG, "onConfigureFailed: capture session config failed");
                            }
                        }, mCameraHandler);
            } catch (InterruptedException e) {
                Log.d(TAG, "createNoPreviewSession: " + Log.getStackTraceString(e));

            } catch (CameraAccessException e) {
                Log.d(TAG, "createNoPreviewSession: " + Log.getStackTraceString(e));
            } catch (SecurityException e) {
                Log.d(TAG, "createNoPreviewSession: " + Log.getStackTraceString(e));
            } finally {
                Log.d(TAG, "createNoPreviewSession mCameraWorkLock: released");
                mCameraWorkLock.release();
            }
        }
    }

    /**
     * Set preferences based on the available picture sizes (8MP is the priority)
     */
    private void setSupportedPicturesSizesPreferences() {
        if ((appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT) == 0) || (appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH) == 0)) {
            Size sizesPreferences = null;
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
                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH, sizesPreferences.getWidth());
                appPrefs.saveIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT, sizesPreferences.getHeight());
            }
        }
    }

    /**
     * This method chooses the best resolution for the preview
     * @param supportedSizes sad
     * @return sad
     */
    private Size getOptimalPreviewSize(List<Size> supportedSizes) {
        Collections.sort(supportedSizes, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                return (rhs.getHeight() * rhs.getWidth()) - (lhs.getHeight() * lhs.getWidth());
            }
        });
        for (Size sz : supportedSizes) {
            float ratio = (float) sz.getWidth() / (float) sz.getHeight();
            if (sz.getHeight() * sz.getWidth() <= (MAX_PREVIEW_HEIGHT * MAX_PREVIEW_WIDTH) + 20 && ratio > 1.3f && ratio < 1.4f) {
                Log.d(TAG, "getOptimalPreviewSize: using resolution: " + sz.getWidth() + " x " + sz.getHeight());
                return sz;
            }
        }
        return supportedSizes.get(0);
    }

    /**
     * This method chooses the best resolution for the taken picture below 16mp
     * @param supportedPictureSizes ad
     * @return ad
     */
    private List<Size> getOptimalPictureSize(List<Size> supportedPictureSizes) {
        List<Size> relevantSizesList = new ArrayList<>();

//        for (Camera.Size size: supportedPictureSizes){
//            Log.d(TAG, "resolution: " + size.getWidth() + " x " + size.getHeight());
//        }
        int fiveMpLimit = (1948 * 2596) + 20;
        int eightMpLimit = (3840 * 2160) + 20;
        int twelveMpLimit = (3024 * 4032) + 20;
        int sixteenMpLimit = (2988 * 5312) + 20;

        Collections.sort(supportedPictureSizes, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                return (rhs.getHeight() * rhs.getWidth()) - (lhs.getHeight() * lhs.getWidth());
            }
        });
        for (Size sz : supportedPictureSizes) {
            if ((sz.getHeight() * sz.getWidth() <= sixteenMpLimit) && (sz.getHeight() * sz.getWidth() > twelveMpLimit)) {
                if (sixteen == null) {
                    sixteen = sz;
                }
            } else if ((sz.getHeight() * sz.getWidth() <= twelveMpLimit) && (sz.getHeight() * sz.getWidth() > eightMpLimit)) {
                if (twelve == null) {
                    twelve = sz;
                }
            } else if ((sz.getHeight() * sz.getWidth() <= eightMpLimit) && (sz.getHeight() * sz.getWidth() > fiveMpLimit)) {
                if (eight == null) {
                    eight = sz;
                }
            } else if (sz.getHeight() * sz.getWidth() <= fiveMpLimit) {
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
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mOrientationListener.disable();
            mCameraOpenCloseLock.acquire();
            Log.d(TAG, "closeCamera mCameraWorkLock: acquired");
            mCameraWorkLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "closeCamera: " + Log.getStackTraceString(e));
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraWorkLock.release();
            mCameraOpenCloseLock.release();
            Log.d(TAG, "closeCamera mCameraWorkLock: released");
        }
        EventBus.clear(CameraInitEvent.class);
        EventBus.clear(CameraInfoEvent.class);
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        /*
      An additional thread for running tasks that shouldn't block the UI.
     */
        HandlerThread mCameraThread = new HandlerThread("CameraThread", Process.THREAD_PRIORITY_FOREGROUND);
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        HandlerThread mBackgroundThread = new HandlerThread("CameraBackgroundThread", Process.THREAD_PRIORITY_FOREGROUND);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    public void takeSnapshot(final ShutterCallback shutterCallback, final ImageReadyCallback imageReadyCallback,
                             final long timestamp, final int sequenceId, final String folderPath, final Location location) {
        Log.d(TAG, "takeSnapshot: ");
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "takeSnapshot mCameraWorkLock: acquired");
                    mCameraWorkLock.acquire();
                    if (null == mCameraDevice) {
                        open();
                    }
                    // This is the CaptureRequest.Builder that we use to take a picture.
                    mSnapShotRequestBuilder.addTarget(mImageReader.getSurface());

                    // Orientation
                    mSnapShotRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation());

                    mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            if (imageReadyCallback != null) {
                                try {
                                    Image mImage = reader.acquireNextImage();
                                    if (mImage.getFormat() == ImageFormat.JPEG) {
                                        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                                        byte[] bytes = new byte[buffer.remaining()];
                                        buffer.get(bytes);
                                        imageReadyCallback.onPictureTaken(bytes, timestamp, sequenceId, folderPath, location);
                                    }
                                    mImage.close();
                                } catch (IllegalStateException e) {
                                    Log.w(TAG, "onImageAvailable: capturing too fast: " + Log.getStackTraceString(e));
                                }
                            }
                        }
                    }, mBackgroundHandler);
                    mCaptureSession.capture(mSnapShotRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                            Log.d(TAG, "onCaptureStarted: ");
                        }

                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            Log.d(TAG, "onCaptureCompleted: ");
                            if (shutterCallback != null) {
                                shutterCallback.onShutter();
                            }
                        }

                        @Override
                        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                            Log.d(TAG, "onCaptureFailed: " + (failure.getReason() == CaptureFailure.REASON_ERROR ? "error" : "flushed"));
                        }

                        @Override
                        public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
                            Log.d(TAG, "onCaptureBufferLost: ");
                        }
                    }, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    Log.w(TAG, "takeSnapshot: " + Log.getStackTraceString(e));
                } catch (Exception e) {
                    Log.w(TAG, "takeSnapshot: " + Log.getStackTraceString(e));
                } finally {
                    Log.d(TAG, "takeSnapshot mCameraWorkLock: released");
                    mCameraWorkLock.release();
                }
            }
        });
    }

    @Override
    public void open() {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mOrientationListener.enable();
                readCameraCharacteristics();
                android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                try {
                    if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        EventBus.postSticky(new CameraInitEvent(CameraInitEvent.TYPE_FAILED));
                    }
                    manager.openCamera(mCameraId, mCameraStateCallback, mCameraHandler);
                } catch (SecurityException e) {
                    EventBus.postSticky(new CameraPermissionEvent());
                    e.printStackTrace();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
            }
        });
    }

    private void readCameraCharacteristics() {
        android.hardware.camera2.CameraManager manager = (android.hardware.camera2.CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                mFocusRect = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Integer afRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
                Integer aeRegions = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
                mSupportsTapToFocus = afRegions != null && aeRegions != null && (afRegions > 0) && (aeRegions > 0);

                Integer rotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                mSensorOrientation = rotation != null ? rotation : 0;

                mPictureResolutions = new ArrayList<>(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)));
                Collections.sort(mPictureResolutions, new CompareSizesByArea());
                mPreviewSize = getOptimalPreviewSize(Arrays.asList(map.getOutputSizes(SurfaceTexture.class)));

                mCameraId = cameraId;
                EventBus.postSticky(new CameraInfoEvent(mPreviewSize.getWidth(), mPreviewSize.getHeight()));
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    @Override
    public void release() {
        closeCamera();
    }

    @Override
    public void setPreviewSurface(SurfaceTexture texture) {
        if (texture != null) {
            addPreviewToCaptureSession(texture);
        } else {
            removePreviewFromCaptureSession();
        }
    }

    private void addPreviewToCaptureSession(final SurfaceTexture texture) {
        if (mCameraDevice != null) {
            try {
                Log.d(TAG, "addPreviewToCaptureSession mCameraWorkLock: acquired");
                mCameraWorkLock.acquire();
                if (mCaptureSession != null) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                Surface surface = new Surface(texture);
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mPreviewRequestBuilder
                        = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surface);
                mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface(), surface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                Log.d(TAG, "setPreviewSurface: onConfigured: ");
                                try {
                                    mCameraWorkLock.acquire();
                                    mCaptureSession = cameraCaptureSession;
                                    // Auto focus should be continuous for camera preview.
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
                                    mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f);
                                    // Flash is automatically enabled when necessary.
                                    // Finally, we start displaying the camera preview.
                                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                                            null, mCameraHandler);

                                } catch (CameraAccessException e) {
                                    Log.d(TAG, "addPreviewToCaptureSession: " + Log.getStackTraceString(e));
                                } catch (InterruptedException e) {
                                    Log.d(TAG, "onConfigured: " + Log.getStackTraceString(e));
                                } finally {
                                    mCameraWorkLock.release();
                                }
                            }

                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                Log.d(TAG, "onConfigureFailed: capture session config failed");
                            }
                        }, mCameraHandler);
                mTexture = texture;
            } catch (CameraAccessException e) {
                Log.d(TAG, "addPreviewToCaptureSession: " + Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                Log.d(TAG, "addPreviewToCaptureSession: " + Log.getStackTraceString(e));
            } finally {
                Log.d(TAG, "addPreviewToCaptureSession mCameraWorkLock: released");
                mCameraWorkLock.release();
            }
        } else {
            Log.d(TAG, "addPreviewToCaptureSession: camera is null");
        }
    }

    private void removePreviewFromCaptureSession() {
        if (mCaptureSession != null && mCameraDevice != null) {
            try {
                Log.d(TAG, "removePreviewFromCaptureSession mCameraWorkLock: acquired");
                mCameraWorkLock.acquire();
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
            } catch (CameraAccessException e) {
                Log.d(TAG, "removePreviewFromCaptureSession: " + Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                Log.d(TAG, "removePreviewFromCaptureSession: " + Log.getStackTraceString(e));
            } finally {
                Log.d(TAG, "removePreviewFromCaptureSession mCameraWorkLock: released");
                mCameraWorkLock.release();
            }
            mTexture = null;
            createNoPreviewSession();
        }
    }

    @Override
    public void forceCloseCamera() {
        closeCamera();
    }

    @Override
    public List<com.telenav.osv.utils.Size> getSupportedPictureSizes() {
        if (mPictureResolutions == null) {
            return null;
        }
        List<Size> supportedPicturesSizes = getOptimalPictureSize(mPictureResolutions);
        ArrayList<com.telenav.osv.utils.Size> list = new ArrayList<>(supportedPicturesSizes.size());
        for (Size size : supportedPicturesSizes) {
            list.add(new com.telenav.osv.utils.Size(size.getWidth(), size.getHeight()));
        }
        return list;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onResolutionChanged(CameraConfigChangedCommand command) {
        try {
            mCameraWorkLock.acquire();
            int width = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH);
            int height = appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT);
            int maxImages;
            if (width * height > (3840 * 2160) + 20) {
                maxImages = 1;
            } else {
                maxImages = 2;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
            mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, maxImages);

            if (mCameraDevice != null || mSnapShotRequestBuilder != null) {
                // We set up a CaptureRequest.Builder with the output Surface.
                if (mSnapShotRequestBuilder == null) {
                    mSnapShotRequestBuilder = createSnapshotTemplate();
                }
                mSnapShotRequestBuilder.addTarget(mImageReader.getSurface());
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "onCameraConfigChanged: " + Log.getStackTraceString(e));
        } catch (CameraAccessException e) {
            Log.d(TAG, "onCameraConfigChanged: " + Log.getStackTraceString(e));
        } finally {
            mCameraWorkLock.release();
        }
        if (mTexture != null) {
            addPreviewToCaptureSession(mTexture);
        }
    }

    private CaptureRequest.Builder createSnapshotTemplate() throws CameraAccessException {
        mSnapShotRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        // Auto focus should be continuous.
        mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        mSnapShotRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f);
        mSnapShotRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
        mSnapShotRequestBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, new Size(0, 0));
        return mSnapShotRequestBuilder;
    }

    private int getOrientation() {
        return (mSensorOrientation + mOrientation) % 360;
    }

    @Override
    protected void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    @Override
    public void unlockFocus() {

    }

    @Override
    public void focusOnTouch(int x, int y) {
        super.focusOnTouch(x, y);
    }

    @Override
    protected void focus(Rect focusRect) {
        if (mSupportsTapToFocus && mPreviewRequestBuilder != null && mCaptureSession != null && mCameraDevice != null) {

            Log.d(TAG, "focus: left=" + focusRect.left + " right=" + focusRect.right + " top=" + focusRect.top + " bottom=" + focusRect.bottom);
            focusRect.left += 1000;
            focusRect.right += 1000;
            focusRect.top += 1000;
            focusRect.bottom += 1000;
//            Log.d(TAG, "focus: left=" + focusRect.left + " right=" + focusRect.right + " top=" + focusRect.top + " bottom=" + focusRect.bottom);
            scaleRect(focusRect, mFocusRect);
//            Log.d(TAG, "focus: left=" + focusRect.left + " right=" + focusRect.right + " top=" + focusRect.top + " bottom=" + focusRect.bottom);
            MeteringRectangle meteringRectangle = new MeteringRectangle(focusRect, 1000);
            MeteringRectangle[] meteringRectangleArr = {meteringRectangle};
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangleArr);
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, meteringRectangleArr);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            //setting up snapshot request builder
//            mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
//            mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
//            mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangleArr);
//            mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
//                    mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//            mSnapShotRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, meteringRectangleArr);
            try {
                mCameraWorkLock.acquire();
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.d(TAG, "focusOnTouch: " + Log.getStackTraceString(e));
            } catch (InterruptedException e) {
                Log.d(TAG, "focusOnTouch: " + Log.getStackTraceString(e));
            } finally {
                mCameraWorkLock.release();
            }
        }
    }

    private void scaleRect(Rect source, Rect system) {
        // source is always 2000x2000

        float widthRatio = system.width() / 2000f;
        float heightRatio = system.height() / 2000f;

        source.left = (int) ((float) source.left * widthRatio);
        source.right = (int) ((float) source.right * widthRatio);
        source.top = (int) ((float) source.top * heightRatio);
        source.bottom = (int) ((float) source.bottom * heightRatio);

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}

