package com.telenav.osv.recorder.camera.init;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.recorder.camera.util.CameraError;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.recorder.camera.util.SizeMap;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * Camera initialization which implements {@link CameraInitialization} interface for handling the camera initialization operations.
 * for listen when the device orientation is changed.
 * @author cameliao
 */

public class Camera2InitManager extends CameraInitManager {

    private static final String TAG = Camera2InitManager.class.getSimpleName();

    /**
     * Time out for waiting to open the camera.
     */
    private static final int LOCK_TIME_OUT = 2500;

    /**
     * Divider for frame data pixels array.
     */
    private static final int QUARTER_DIVIDER = 4;

    /**
     * The {@code String} id of the back camera.
     */
    private String cameraId;

    /**
     * Instance of {@link CameraCharacteristics} containing the properties describing the {@link CameraDevice}.
     */
    private CameraCharacteristics cameraCharacteristics;

    /**
     * Instance of the connected {@link CameraDevice} representing the selected camera, in our case is always the back one.
     */
    private CameraDevice cameraDevice;

    /**
     * The builder for setting the needed configurations for a picture capture.
     */
    private CaptureRequest.Builder previewRequestBuilder;

    /**
     * The capture request containing all the setups for capturing a picture.
     */
    private CaptureRequest previewRequest;

    /**
     * Instance of the {@link ImageReader} containing the image data.
     */
    private ImageReader imageReader;

    /**
     * Emitter for sending events when the camera is opened or when an error occurred.
     */
    private CompletableEmitter openCameraEmitter;

    /**
     * The capture session used for capturing an image.
     * The main operations used are:
     * <ul>
     * <li>{@link CameraCaptureSession#setRepeatingRequest(CaptureRequest, CameraCaptureSession.CaptureCallback, Handler)}
     * which requests endlessly repeating capture requests which maintains the preview camera open.</li>
     * <li>{@link CameraCaptureSession#capture(CaptureRequest, CameraCaptureSession.CaptureCallback, Handler)}
     * which requests a picture capture.
     * If the {@code setRepeatingRequest} method is used with a {@code capture} method, the
     * {@code capture} method has priority over the other one and after is executed camera will continue to execute repeated capture requests./li>
     */
    private CameraCaptureSession captureSession;

    /**
     * {@code Subject} that emits the most recent instance of {@code CameraCaptureSession}.
     */
    private BehaviorSubject<CameraCaptureSession> captureSessionSubject = BehaviorSubject.create();

    /**
     * {@code Subject} that emits the most recent instance of {@code CaptureRequest.Builder}.
     */
    private BehaviorSubject<CaptureRequest.Builder> captureRequestBuilderSubject = BehaviorSubject.create();

    /**
     * {@code Subject} that emits the most recent instance of {@code CameraCharacteristics}.
     */
    private BehaviorSubject<CameraCharacteristics> cameraCharacteristicsSubject = BehaviorSubject.create();

    /**
     * Used for blocking the active close/open camera operation, in order to avoid calling the close and open camera while another operation is already executed.
     */
    private Semaphore lock = new Semaphore(1);

    /**
     * Possible values for device orientation:
     * <ul>
     * <li>{@link android.view.Surface#ROTATION_0}</li> portrait
     * <li>{@link android.view.Surface#ROTATION_90}</li> landscape right
     * <li>{@link android.view.Surface#ROTATION_180}</li> landscape left
     * </ul>
     */
    private int deviceOrientation;

    private int hardwareLevel;

    /**
     * Callback to listen for new images or frames having the required image format.
     * The received frame or image byte array are redirected to the camera observer which
     * is responsible to save or encode the data.
     */
    private ImageReader.OnImageAvailableListener imageAvailableListener = imageReader -> {
        Image image = imageReader.acquireLatestImage();
        if (image == null) {
            return;
        }
        Image.Plane[] planes;
        try {
            planes = image.getPlanes();
        } catch (IllegalStateException e) {
            Log.d(TAG, String.format("onImageAvailable. Status: error. Operation: getPlanes. Message: %s", e.getMessage()));
            return;
        }
        if (planes.length > 0 && shouldTakeFrame) {
            if (frameByteEmitter != null) {
                CameraFrame cameraFrame;
                if (isImageFormatJpeg()) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    cameraFrame = new CameraFrame(data, imageFormat);
                } else {
                    try {
                        cameraFrame = getCameraFrame(planes);
                    } catch (IllegalStateException e) {
                        Log.d(TAG, String.format("onImageAvailable. Status: error. Operation: getCameraFrame Message: %s", e.getMessage()));
                        image.close();
                        return;
                    }
                }
                shouldTakeFrame = false;
                frameByteEmitter.onSuccess(cameraFrame);
            }
        }
        image.close();
    };

    /**
     * Callback to listen for the camera states as: opened, disconnected, error.
     */
    private CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened");
            // This method is called when the camera is opened.  We start camera preview here.
            cameraDevice = camera;
            orientationListener.enable();
            if (surfaceTexture != null) {
                addCameraPreviewSession();
            } else {
                createNoCameraPreviewSession();
            }
            lock.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected");
            lock.release();
            closeCamera();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            Log.d(TAG, "onClosed");
            orientationListener.disable();
            super.onClosed(camera);
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.d(TAG, "onError");
            orientationListener.disable();
            switch (error) {
                case ERROR_CAMERA_IN_USE:
                    Log.d(TAG, "onError: ERROR_CAMERA_IN_USE");
                    break;
                case ERROR_MAX_CAMERAS_IN_USE:
                    Log.d(TAG, "onError: ERROR_MAX_CAMERAS_IN_USE");
                    break;
                case ERROR_CAMERA_DISABLED:
                    Log.d(TAG, "onError: ERROR_CAMERA_DISABLED");
                    break;
                case ERROR_CAMERA_DEVICE:
                    Log.d(TAG, "onError: ERROR_CAMERA_DEVICE");
                    break;
                case ERROR_CAMERA_SERVICE:
                    Log.d(TAG, "onError: ERROR_CAMERA_SERVICE");
                    break;
                default:
                    Log.d(TAG, "onError: " + error);
            }
            lock.release();
            if (openCameraEmitter != null) {
                openCameraEmitter.onError(new CameraError(CameraError.ERROR_CAMERA_INIT_FAILED_TO_OPEN));
                openCameraEmitter = null;
            }
            closeCamera();
        }
    };

    /**
     * Instance to the system service camera manager used for opening, detecting, characterizing, and connecting to the camera.
     */
    private CameraManager cameraManager;

    /**
     * Default constructor for the current class.
     * @param context the application context for creating a {@link CameraManager} instance.
     * @param pictureSize the picture resolution size.
     * @param containerSize the size of the camera container.
     */
    public Camera2InitManager(Context context, Size pictureSize, Size containerSize, int imageFormat) {
        super(context, containerSize, imageFormat);
        //The CameraManager should be instantiated using the application context in order to fix a bug on Android 5.0
        // when the manager leaks the context that creates it.
        cameraManager = (CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        this.containerSize = containerSize;
        this.pictureSize = pictureSize;
        prepareCameraCharacteristicForTheCaptureSession();
        setPictureSize(pictureSize);
    }

    @Override
    public Completable openCamera() {
        return Completable.create(emitter -> {
            if (cameraId.isEmpty()) {
                emitter.onError(new CameraError(CameraError.ERROR_CAMERA_INIT_FAILED_TO_OPEN));
            }
            try {
                if (lock.tryAcquire(LOCK_TIME_OUT, TimeUnit.MILLISECONDS)) {
                    cameraManager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);
                    openCameraEmitter = emitter;
                } else {
                    emitter.onError(new CameraError(CameraError.ERROR_CAMERA_INIT_FAILED_TO_OPEN));
                    openCameraEmitter = null;
                }
            } catch (CameraAccessException | IllegalArgumentException e) {
                Log.d(TAG, "Couldn't open the camera.");
                emitter.onError(new CameraError(CameraError.ERROR_CAMERA_INIT));
                openCameraEmitter = null;
            } catch (SecurityException e) {
                Log.d(TAG, "The user needs to grant camera permission.");
                emitter.onError(new CameraError(CameraError.ERROR_CAMERA_PERMISSION_NEEDED));
                openCameraEmitter = null;
            }
        });
    }

    @Override
    public void closeCamera() {
        Log.d(TAG, "closeCamera");
        try {
            lock.acquire();
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "Close camera: " + e.getMessage());
        } finally {
            lock.release();
        }
    }

    @Override
    public Single<CameraFrame> captureStillPicture() {
        return Single.create(emitter -> {
            if (frameData == null) {
                initFrameBuffer(pictureSize);
            }
            shouldTakeFrame = true;
            frameByteEmitter = emitter;
            if (!isImageFormatJpeg()) {
                return;
            }
            capturePicture();
        });
    }

    @Override
    public void addCameraPreviewSession() {
        if (cameraDevice == null) {
            Log.d(TAG, "Camera device is null.");
            return;
        }
        if (surfaceTexture == null) {
            Log.d(TAG, "Camera surface texture is null.");
            return;
        }
        removePreviewFromCaptureSession();
        prepareImageReader();
        try {
            Surface surface = new Surface(surfaceTexture);
            surfaceTexture.setDefaultBufferSize(optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight());
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            if (!isImageFormatJpeg()) {
                previewRequestBuilder.addTarget(imageReader.getSurface());
            }
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "Added a new camera preview.");
                    captureSession = session;
                    if (openCameraEmitter != null) {
                        openCameraEmitter.onComplete();
                        openCameraEmitter = null;
                    }
                    captureSessionSubject.onNext(session);
                    captureRequestBuilderSubject.onNext(previewRequestBuilder);
                    setAutoFocus();
                    setAutoExposure();
                    previewRequest = previewRequestBuilder.build();
                    try {
                        if (cameraDevice == null || captureSession == null) {
                            return;
                        }
                        captureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                        if (isImageFormatJpeg() && shouldTakeFrame) {
                            capturePicture();
                        }
                    } catch (CameraAccessException | IllegalStateException e) {
                        Log.d(TAG, String.format("addCameraPreviewSession: Failed to start camera preview - %s", e.getMessage()));
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "addCameraPreviewSession - onConfigureFailed: capture session config failed");
                    if (openCameraEmitter != null) {
                        openCameraEmitter.onError(new CameraError(CameraError.ERROR_CAMERA_PREVIEW_FAILED));
                        openCameraEmitter = null;
                    }
                    captureSession = null;
                }
            }, backgroundHandler);
        } catch (CameraAccessException | NullPointerException | IllegalStateException | IllegalArgumentException e) {
            Log.d(TAG, String.format("addCameraPreviewSession: %s", e.getMessage()));
        }
    }

    @Override
    public void createNoCameraPreviewSession() {
        if (cameraDevice == null) {
            return;
        }
        removePreviewFromCaptureSession();
        prepareImageReader();
        try {
            if (!isImageFormatJpeg()) {
                previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(imageReader.getSurface());
            }
            cameraDevice.createCaptureSession(Collections.singletonList(imageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    captureSession = cameraCaptureSession;
                    if (openCameraEmitter != null) {
                        openCameraEmitter.onComplete();
                        openCameraEmitter = null;
                    }
                    captureSessionSubject.onNext(cameraCaptureSession);
                    Log.d(TAG, "Created no camera preview session.");
                    if (isImageFormatJpeg()) {
                        if (shouldTakeFrame) {
                            capturePicture();
                        }
                        return;
                    }
                    captureRequestBuilderSubject.onNext(previewRequestBuilder);
                    previewRequest = previewRequestBuilder.build();
                    try {
                        if (cameraDevice == null || captureSession == null) {
                            return;
                        }
                        captureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
                    } catch (CameraAccessException | IllegalStateException e) {
                        Log.d(TAG, String.format("createNoCameraPreviewSession: Failed to start camera preview - %s", e.getMessage()));
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "createNoCameraPreviewSession - onConfigureFailed: capture session config failed");
                    captureSession = null;
                }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.d(TAG, String.format("createNoCameraPreviewSession: %s", e.getMessage()));
        }
    }

    @Override
    public Subject<CameraCaptureSession> getCaptureSessionEvent() {
        return captureSessionSubject;
    }

    @Override
    public Subject<CaptureRequest.Builder> getCaptureRequestBuilderEvent() {
        return captureRequestBuilderSubject;
    }

    @Override
    public Subject<CameraCharacteristics> getCameraCharacteristicsEvent() {
        return cameraCharacteristicsSubject;
    }

    @Override
    public int getHardwareLevel() {
        return hardwareLevel;
    }

    @Override
    public void setDeviceOrientation(int deviceOrientation) {
        this.deviceOrientation = deviceOrientation;
    }

    @Override
    public void startPreview() {
        if (surfaceTexture == null) {
            createNoCameraPreviewSession();
        } else {
            addCameraPreviewSession();
        }
    }

    /**
     * Starts teh request for capturing a picture.
     */
    private void capturePicture() {
        CaptureRequest captureRequest = createCaptureRequest();
        if (captureRequest != null && captureSession != null) {
            try {
                captureSession.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp,
                                                 long frameNumber) {
                        Log.d(TAG, "onCaptureStarted");
                    }

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {
                        Log.d(TAG, "onCaptureCompleted");
                    }

                    @Override
                    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                @NonNull CaptureFailure failure) {
                        Log.d(TAG, "onCaptureFailed: " + (failure.getReason() == CaptureFailure.REASON_ERROR ? "error" : "flushed"));
                    }

                    @Override
                    public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target,
                                                    long frameNumber) {
                        Log.d(TAG, "onCaptureBufferLost");
                    }
                }, backgroundHandler);
            } catch (CameraAccessException e) {
                Log.d(TAG, String.format("Cannot take pictures - %s", e.getMessage()));
                frameByteEmitter.onError(new CameraError(CameraError.ERROR_CAPTURE_FAILED));
            }
        }
    }

    /**
     * Handles teh conversion from camera output format to encoding input format.
     * @param planes the planes containing the data YUV information.
     * @return the frame data containing the YUV planes.
     * @throws IllegalStateException if the frame buffer is inaccessible.
     */
    private CameraFrame getCameraFrame(Image.Plane[] planes) throws IllegalStateException {
        //the length for Y component is equal with the picture dimension
        int yLength = pictureSize.getWidth() * pictureSize.getHeight();
        //the length for U or V component is a quarter of the picture dimension.
        int uvLength = yLength / QUARTER_DIVIDER;
        //get Y data frame with padding
        Image.Plane yPlane = planes[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        Log.d(TAG, String.format("getCameraFrame. Y frame data remaining: %s  row stride: %s  pixel stride: %s",
                yBuffer.remaining(), yPlane.getRowStride(), yPlane.getPixelStride()));
        readYFrameData(yBuffer, yPlane.getRowStride(), pictureSize.getWidth(), pictureSize.getHeight());
        //get U data frame with padding
        Image.Plane uPlane = planes[1];
        ByteBuffer uBuffer = uPlane.getBuffer();
        Log.d(TAG, String.format("getCameraFrame. U frame data remaining: %s row stride: %s pixel stride: %s",
                uBuffer.remaining(), uPlane.getRowStride(), uPlane.getPixelStride()));
        readUVFrameData(uBuffer, uPlane.getRowStride(), uPlane.getPixelStride(), yLength, pictureSize.getWidth(), pictureSize.getHeight());
        //get V data frame with padding
        Image.Plane vPlane = planes[2];
        ByteBuffer vBuffer = vPlane.getBuffer();
        Log.d(TAG, String.format("getCameraFrame. V frame data remaining: %s row stride: %s pixel stride: %s",
                vBuffer.remaining(), vPlane.getRowStride(), vPlane.getPixelStride()));
        readUVFrameData(vBuffer, vPlane.getRowStride(), vPlane.getPixelStride(), yLength + uvLength, pictureSize.getWidth(), pictureSize.getHeight());
        return new CameraFrame(frameData, imageFormat);
    }

    /**
     * Reads the U or V data from the given buffer and updates {@link #frameData} array.
     * Frame data <tt>length</tt>:
     * <ul>
     * <li>Y data <tt>length</tt>: <tt>width * height</tt></li>
     * <li>U data <tt>length</tt>: <tt>(width * height) / 2</tt></li>
     * <li>V data <tt>length</tt>: <tt>(width * height) / 2</tt></li>
     * </ul>
     * @param uvFrameData the buffer for the U or V component.
     * @param rowStride the row stride for determining the row padding for the frame.
     * @param pixelStride the pixel stride for determining the vertical padding for teh current frame
     * @param index the start index from where the data should be written in the {@link #frameData} array.
     * @param width the frame width
     * @param height the frame height.
     */
    private void readUVFrameData(ByteBuffer uvFrameData, int rowStride, int pixelStride, int index, int width, int height) {
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                frameData[index] = uvFrameData.get(col * pixelStride + row * (rowStride));
                index++;
            }
        }
    }

    /**
     * Reads the Y data from the given buffer and updates {@link #frameData} array.
     * Frame data <tt>length</tt>:
     * <ul>
     * <li>Y data <tt>length</tt>: <tt>width * height</tt></li>
     * <li>U data <tt>length</tt>: <tt>(width * height) / 2</tt></li>
     * <li>V data <tt>length</tt>: <tt>(width * height) / 2</tt></li>
     * </ul>
     * @param yFrameData teh buffer for the Y component.
     * @param rowStride the row stride for determining the row padding for the current frame.
     * @param width the frame width.
     * @param height the frame height.
     */
    private void readYFrameData(ByteBuffer yFrameData, int rowStride, int width, int height) {
        ByteBuffer yBuffer = ByteBuffer.allocate(width * height);
        byte[] yData = new byte[yFrameData.remaining()];
        yFrameData.get(yData, 0, yFrameData.remaining());
        for (int i = 0; i < height; i++) {
            yBuffer.put(yData, i * rowStride, width);
        }
        yBuffer.flip();
        yBuffer.get(frameData, 0, width * height);
    }

    /**
     * Sets the auto exposure ON for the capture session.
     */
    private void setAutoExposure() {
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        if (modes == null || modes.length == 0 || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AE_MODE_OFF)) {
            //The auto exposure mode is not supported
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        } else {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        }
    }

    /**
     * Removes the camera preview from the capture session by creating another session without preview.
     */
    private void removePreviewFromCaptureSession() {
        if (captureSession != null && cameraDevice != null) {
            try {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            } catch (CameraAccessException | IllegalStateException e) {
                Log.d(TAG, String.format("removePreviewFromCaptureSession: %s", e.getMessage()));
            }
        }
    }

    /**
     * Creates a capture request for taking a picture.
     * @return a {@code CaptureRequest} containing all the settings for taking a picture.
     */
    private CaptureRequest createCaptureRequest() {
        CaptureRequest.Builder captureRequestBuilder = null;
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        } catch (CameraAccessException e) {
            Log.d(TAG, String.format("createCaptureRequest - %s", e.getMessage()));
        }
        if (captureRequestBuilder == null) {
            return null;
        }
        captureRequestBuilder.addTarget(imageReader.getSurface());
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF);
        Integer sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (sensorOrientation == null) {
            sensorOrientation = 0;
        }
        int jpegOrientation = CameraHelper.getOrientation(deviceOrientation, sensorOrientation);
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);
        captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) JPEG_CAPTURE_QUALITY);
        return captureRequestBuilder.build();
    }

    /**
     * Prepares {@code ImageReader} for the capture session by setting a listener which will be notified when a new image is available.
     */
    private void prepareImageReader() {
        Log.d(TAG, "prepareImageReader");
        if (imageReader != null) {
            imageReader.close();
        }
        Log.d(TAG, String.format("Image Reader: %s %s", pictureSize.getWidth(), pictureSize.getHeight()));
        imageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), imageFormat, 2);
        imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
    }

    /**
     * Sets the auto focus ON for the capture session.
     */
    private void setAutoFocus() {
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (modes == null || modes.length == 0 || (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
            //The auto focus mode is not supported
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            Log.d(TAG, "Auto focus mode not supported");
        } else {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        }
    }

    /**
     * Prepare the camera properties for the capture session by setting the back camera id,
     * camera characteristics object, the supported preview and picture sizes.
     */
    private void prepareCameraCharacteristicForTheCaptureSession() {
        Log.d(TAG, "prepareCameraCharacteristicForTheCaptureSession");

        try {
            StreamConfigurationMap map = null;
            if (cameraManager.getCameraIdList().length == 0) {
                Log.d(TAG, "Camera id list is empty.");
                return;
            }
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = cameraManager.getCameraCharacteristics(cameraId);
                hardwareLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                // We don't use a front facing camera.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                this.cameraId = cameraId;
                this.cameraCharacteristics = characteristics;
            }
            if (cameraId != null && map != null) {
                SizeMap previewSizes = CameraHelper.getPreviewSizes(map);
                pictureSizes = CameraHelper.getPictureSizes(map, imageFormat);
                optimalPreviewSize = CameraHelper.chooseOptimalPreviewSize(previewSizes.allSizes(), containerSize.getWidth(), containerSize.getHeight());
            }
            cameraCharacteristicsSubject.onNext(cameraCharacteristics);
        } catch (CameraAccessException e) {
            Log.d(TAG, String.format("prepareCameraCharacteristicForTheCaptureSession: %s", e.getMessage()));
        }
    }
}