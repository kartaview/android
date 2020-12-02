package com.telenav.osv.recorder.camera.init;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.google.common.base.Optional;
import com.telenav.osv.recorder.camera.model.CameraDeviceData;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.recorder.camera.util.CameraError;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.recorder.camera.util.SizeMap;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;

import org.apache.commons.math3.util.Precision;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * The implementation for the Camera V1 initialization.
 */
//TODO: This class contains the legacy code and should be refactored properly.
@SuppressWarnings("deprecation")
public class Camera1InitManager extends CameraInitManager implements Camera.ErrorCallback, Camera.AutoFocusMoveCallback, Camera.PreviewCallback {

    private static final String TAG = Camera1InitManager.class.getSimpleName();

    private static final int TEXTURE_ID = 26;

    private static final int FOCUS_MODE_STATIC = 0;

    private static final int FOCUS_MODE_DYNAMIC = 1;

    /**
     * The camera api v1 value for when the angle is not set
     */
    private static final int CAMERA_API_V1_ANGLE_UNKOWN = -1;

    private final Object syncObject = new Object();

    private Camera camera;

    private Camera.Parameters parameters;

    private int focusMode = FOCUS_MODE_DYNAMIC;

    private BehaviorSubject<Camera.Parameters> cameraParametersSubject = BehaviorSubject.create();

    private BehaviorSubject<Optional<Camera>> cameraSubject = BehaviorSubject.create();

    private boolean isCameraOpen;

    private Disposable disposable;

    public Camera1InitManager(Context context, Size pictureSize, Size containerSize, int imageFormat) {
        super(context, containerSize, imageFormat);
        this.pictureSize = pictureSize;
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.d(TAG, "Constructor. Status: inactive. Message: No camera on this devices.");
        } else {
            setupBackFacingCameras();
            int cameraId = Integer.parseInt(cameraDeviceDataList.get(0).getCameraId());
            if (cameraId < 0) {
                Log.d(TAG, "Constructor. Status: not found. Message: No back camera found.");
            }
            this.containerSize = containerSize;
            camera = Camera.open();
            parameters = camera.getParameters();
            pictureSizes = CameraHelper.getPictureSizes(parameters.getSupportedPictureSizes());
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Utils.checkResolutionsAvailabilityForEncoder(previewSizes);
            optimalPreviewSize = CameraHelper.chooseOptimalPreviewSize(previewSizes, containerSize);
            setPictureSize(pictureSize);
            camera.release();
            camera = null;
            parameters = null;
        }
    }

    @Override
    public Completable openCamera() {
        return Completable.create(emitter -> {
            if (camera != null) {
                Log.d(TAG, "openCamera. Status: success. Message: Camera already exists.");
                isCameraOpen = true;
                orientationListener.enable();
                emitter.onComplete();
            } else {
                synchronized (syncObject) {
                    if (cameraDeviceData == null) {
                        Log.d(TAG, "openCamera. Status: error. Message: Camera data not found.");
                        emitter.onError(new Throwable("Camera data not found."));
                        return;
                    }
                    try {
                        camera = Camera.open(Integer.parseInt(cameraDeviceData.getCameraId()));
                    } catch (Exception e) {
                        Log.d(TAG, "openCamera. Status: error. Message: Invalid camera id.");
                        emitter.onError(new Throwable("Camera not found."));
                        return;
                    }
                    isCameraOpen = true;
                    Log.d(TAG, "openCamera. Status: opening. Message: open.");
                    camera.setErrorCallback(Camera1InitManager.this);
                    Log.d(TAG, "openCamera. Status: opening. Message: set error callback.");
                    camera.enableShutterSound(false);
                    Log.d(TAG, "openCamera. Status: opening. Message: enable shutter sound.");
                    Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
                    Log.d(TAG, "openCamera. Status: opening. Message: info.");
                    Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
                    Log.d(TAG, "openCamera. Status: opening. Message: get camera info.");
                    camera.setDisplayOrientation(info.orientation);
                    Log.d(TAG, "openCamera. Status: opening. Message: set display orientation.");
                    initCameraParameters();
                    Log.d(TAG, "openCamera. Status: opening. Message: init params.");
                    if (surfaceTexture != null) {
                        startPreview();
                    }
                    Log.d(TAG, "openCamera. Status: success. Message: Camera opened successfully.");
                    orientationListener.enable();
                    emitter.onComplete();
                }
            }
        });
    }

    @Override
    public Completable openCamera(String cameraId) {
        cameraDeviceData = getCameraById(cameraId);
        return openCamera();
    }

    @Override
    public void closeCamera() {
        try {
            if (camera != null) {
                isCameraOpen = false;
                cameraSubject.onNext(Optional.fromNullable(null));
                Log.d(TAG, "closeCamera");
                stopPreview();
                camera.setPreviewCallbackWithBuffer(null);
                camera.release();
                camera = null;
                parameters = null;
                orientationListener.disable();
            }
            cameraDeviceData = null;
        } catch (RuntimeException e) {
            Log.d(TAG, "closeCamera. Status: error. Message: " + e.getMessage());
        }
    }

    @Override
    public Single<CameraFrame> captureStillPicture() {
        if (!isCameraOpen) {
            return Single.error(new CameraError(CameraError.ERROR_CAPTURE_FAILED));
        }
        return Single.create(emitter -> {
            if (camera != null) {
                if (!isImageFormatJpeg()) {
                    frameByteEmitter = emitter;
                    shouldTakeFrame = true;
                    return;
                }
                synchronized (syncObject) {
                    try {
                        camera.takePicture(
                                //onShutter callback
                                null, null,
                                //onPictureTaken callback
                                (data, camera) -> {
                                    Log.d(TAG, "captureStillPicture. Status: capture. Message: onPictureTaken callback.");
                                    restartPreviewIfNeeded();
                                    emitter.onSuccess(new CameraFrame(data, imageFormat));
                                });
                    } catch (RuntimeException e) {
                        Log.d(TAG, "captureStillPicture. Status: error. Message: RuntimeException while taking a picture." + e.getMessage());
                        closeCamera();
                        reopenCamera();
                    }
                }
            } else {
                Log.d(TAG, "captureStillPicture. Status: restart. Message: Camera is null and must be restarted.");
                closeCamera();
                reopenCamera();
            }
        });
    }

    @Override
    public void addCameraPreviewSession() {

    }

    @Override
    public void createNoCameraPreviewSession() {

    }

    @Override
    public void onAutoFocusMoving(boolean start, Camera camera) {

    }

    @Override
    public List<Size> getSupportedPictureResolutions(String cameraId) {
        Log.d(TAG, "getSupportedPictureResolutions: cameraId " + cameraId + " imageFormat " + imageFormat);
        if (isImageFormatJpeg()) {
            if (cameraId.isEmpty()) {
                if (cameraDeviceData != null) {
                    cameraId = cameraDeviceData.getCameraId();
                } else {
                    return new ArrayList<>();
                }
            }
            try {
                Camera camera = Camera.open(Integer.parseInt(cameraId));
                Camera.Parameters parameters = camera.getParameters();
                SizeMap pictureSizes = CameraHelper.getPictureSizes(parameters.getSupportedPictureSizes());
                SortedSet<Size> resolutions = CameraHelper.getPictureSize(pictureSizes, optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight());
                camera.release();
                return new ArrayList<>(resolutions);
            } catch (Exception ex) {
                Log.d(TAG, "getSupportedPictureResolutions: Could not retrieve camera picture sizes.");
                return new ArrayList<>();
            }
        } else {
            //When the preview frames are used instead of jpeg images, the only available size for frames is the preview size.
            //This is a limitation only for Camera1, where is not possible to set a different resolution for frames than the preview resolution.
            List<Size> resolutions = new ArrayList<>();
            resolutions.add(optimalPreviewSize);
            return resolutions;
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, String.format("onError. Status: error. Message: An error occurred during the camera usage: errorId %s; camera %s", error, camera.toString()));
        synchronized (syncObject) {
            closeCamera();
            reopenCamera();
        }
    }

    @Override
    public Subject<Camera.Parameters> getCameraParameter() {
        return cameraParametersSubject;
    }

    @Override
    public Subject<Optional<Camera>> getCameraService() {
        return cameraSubject;
    }

    @Override
    public void startPreview() {
        if (camera == null) {
            return;
        }
        if (surfaceTexture == null) {
            surfaceTexture = createTexture();
        }
        synchronized (syncObject) {
            stopPreview();
            try {
                camera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                Log.d(TAG, "startPreview. Status:error. Message:" + e.getMessage());
            }
            try {
                camera.startPreview();
                setPreviewCallback();
            } catch (RuntimeException e) {
                Log.d(TAG, "startPreview. Status:error. Message:" + e.getMessage());
            }
        }
    }

    @Override
    protected void setDeviceOrientation(int orientation) {
        Log.d(TAG, "setDeviceOrientation. Status: success. Message: orientation " + orientation);
        if (parameters == null || cameraDeviceData == null) {
            return;
        }
        int cameraId;
        try {
            cameraId = Integer.parseInt(cameraDeviceData.getCameraId());
        } catch (Exception e) {
            Log.d(TAG, "setDeviceOrientation. Invalid camera id.");
            return;
        }
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = CameraHelper.getOrientation(orientation, info.orientation);
        //On some device the rotation is 89 instead of 90.
        //For this particular case we need to round up to 90 in order to start a preview.
        Log.d(TAG, "setDeviceOrientation. Message: rotation without precision " + rotation);
        rotation = (int) Precision.round((double) rotation, -1);
        Log.d(TAG, "setDeviceOrientation. Message: rotation with precision " + rotation);
        synchronized (syncObject) {
            parameters.setRotation(rotation);
            camera.setParameters(parameters);
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (frameByteEmitter != null && !frameByteEmitter.isDisposed() && shouldTakeFrame) {
            frameByteEmitter.onSuccess(new CameraFrame(bytes, imageFormat));
            shouldTakeFrame = false;
        }
        camera.addCallbackBuffer(bytes);
    }

    @Override
    public List<CameraDeviceData> getCameraDevices() {
        return cameraDeviceDataList;
    }

    /**
     * Reopens the camera if an error occurred.
     */
    private void reopenCamera() {
        disposeObservable();
        disposable = openCamera().subscribe(
                () -> Log.d(TAG, "reopenCamera. Status: success. Message: Camera reopened."),
                throwable -> Log.d(TAG, "reopenCamera. Status: error. Message: Camera reopened failed."));
    }

    /**
     * Stops and releases the camera preview.
     */
    private void stopPreview() {
        camera.stopPreview();
        try {
            camera.setPreviewTexture(null);
        } catch (IOException e) {
            Log.d(TAG, "stopPreview. Status: error. Message: Failed to remove the preview.");
        }
    }

    /**
     * Restarts the camera preview.
     */
    private void restartPreviewIfNeeded() {
        if (backgroundHandler == null) {
            return;
        }
        backgroundHandler.post(() -> {
            try {
                // Some cameras implicitly stops preview, and we don't know why.
                // So we just force it here.
                if (camera != null) {
                    Log.d(TAG, "restartPreviewIfNeeded. Status: starting. Message: Starting the preview.");
                    camera.startPreview();
                    setPreviewCallback();
                } else {
                    Log.w(TAG, "restartPreviewIfNeeded. Status: warning. Message: Camera is null.");
                }
            } catch (Exception e) {
                Log.w(TAG, "restartPreviewIfNeeded. Status: error. Message: Failed to start the preview. " + e.getMessage());
            }
        });
    }

    /**
     * Sets the preview callback for continuously frame updates.
     */
    private void setPreviewCallback() {
        if (frameData == null) {
            initFrameBuffer(optimalPreviewSize);
        }
        camera.addCallbackBuffer(frameData);
        camera.setPreviewCallbackWithBuffer(this);
    }

    /**
     * Sets the auto focus mode.
     */
    private void setAutoFocusMoveCallback() {
        if (focusMode == FOCUS_MODE_DYNAMIC) {
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (camera != null && focusModes != null &&
                    (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO) || focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))) {
                camera.setAutoFocusMoveCallback(this);
            }
        }
    }

    /**
     * Initializes the camera parameters for a camera session.
     */
    private void initCameraParameters() {
        parameters = camera.getParameters();
        pictureSizes = CameraHelper.getPictureSizes(parameters.getSupportedPictureSizes());
        setPictureSize(pictureSize);
        if (optimalPreviewSize == null) {
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Utils.checkResolutionsAvailabilityForEncoder(previewSizes);
            optimalPreviewSize = CameraHelper.chooseOptimalPreviewSize(previewSizes, containerSize);
        }
        parameters.setPreviewSize(optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight());
        if (isImageFormatJpeg()) {
            parameters.setPictureFormat(imageFormat);
            parameters.setJpegQuality(JPEG_CAPTURE_QUALITY);
        } else {
            parameters.setPreviewFormat(imageFormat);
            parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
            setPreviewCallback();
        }
        List<int[]> list = parameters.getSupportedPreviewFpsRange();
        int[] fpsRange = list.get(list.size() - 1);
        parameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        if (parameters.getSupportedSceneModes() != null && parameters.getSupportedSceneModes()
                .contains(Camera.Parameters.SCENE_MODE_AUTO)) {
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        }
        if (parameters.getSupportedWhiteBalance() != null && parameters.getSupportedWhiteBalance()
                .contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }
        setAutoFocusMoveCallback();
        setFocusMode();
        setDeviceOrientation(deviceOrientation);
        cameraSubject.onNext(Optional.of(camera));
        cameraParametersSubject.onNext(parameters);
    }

    /**
     * @return a surface texture view.
     */
    private SurfaceTexture createTexture() {
        int[] texture = new int[1];
        texture[0] = TEXTURE_ID;
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, TEXTURE_ID);

        return new SurfaceTexture(TEXTURE_ID);
    }

    /**
     * Initializes the camera focus mode.
     */
    private void setFocusMode() {
        if (parameters != null) {
            boolean supportsInfinity = parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_INFINITY);
            if (supportsInfinity) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                focusMode = FOCUS_MODE_STATIC;
            } else {
                boolean continuousSupported = parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                if (continuousSupported) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else {
                    if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                }
                focusMode = FOCUS_MODE_DYNAMIC;
            }
        }

    }

    /**
     * Creates a list containing all the supported back cameras and sets as default the first encountered camera.
     */
    private void setupBackFacingCameras() {
        int noOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < noOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Camera camera = Camera.open(i);
                double horizontalFoV = camera.getParameters().getHorizontalViewAngle() != CAMERA_API_V1_ANGLE_UNKOWN ? camera.getParameters().getHorizontalViewAngle() : 0D;
                double verticalFoV = camera.getParameters().getVerticalViewAngle() != CAMERA_API_V1_ANGLE_UNKOWN ? camera.getParameters().getVerticalViewAngle() : 0D;
                float focalLength = camera.getParameters().getFocalLength();
                cameraDeviceDataList.add(new CameraDeviceData(String.valueOf(i), horizontalFoV, verticalFoV, focalLength, 0));
                camera.release();
            }
        }
        cameraDeviceData = cameraDeviceDataList.get(0);
    }

    /**
     * Dispose the camera observer.
     */
    private void disposeObservable() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}