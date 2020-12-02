package com.telenav.osv.recorder.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;

import com.google.common.base.Optional;
import com.telenav.osv.recorder.camera.focus.Camera1FocusManager;
import com.telenav.osv.recorder.camera.focus.Camera2FocusManager;
import com.telenav.osv.recorder.camera.focus.Focus;
import com.telenav.osv.recorder.camera.init.Camera1InitManager;
import com.telenav.osv.recorder.camera.init.Camera2InitManager;
import com.telenav.osv.recorder.camera.init.CameraInitialization;
import com.telenav.osv.recorder.camera.model.CameraDeviceData;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.recorder.camera.util.CameraError;
import com.telenav.osv.recorder.metadata.callback.MetadataCameraCallback;
import com.telenav.osv.utils.ExtensionsKt;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;

import org.joda.time.LocalDateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Implementation of the {@link Camera} interface that holds all the available camera operations.
 * Created by cameliao on 1/30/18.
 */

public class CameraManager implements Camera {

    private static final String TAG = CameraManager.class.getSimpleName();

    private static final byte CAMERA_API_V1 = 0;

    private static final byte CAMERA_API_V2 = 1;

    /**
     * The timeout value when the callback for taking a picture is not received.
     */
    private static final int TIME_OUT_DELAY = 10;

    /**
     * Instance to the camera initialization which handles the open/close operation.
     */
    private CameraInitialization cameraInitialization;

    /**
     * Instance to the focus controller which handles all the logic for lock/unlock focus and focus on a tapped area.
     */
    private Focus focus;

    /**
     * A flag which is {@code true} when the camera is opened, {@code false} otherwise.
     */
    private boolean isCameraOpen = false;

    /**
     * Represents a handler thread.
     */
    private HandlerThread backgroundThread;

    /**
     * Represents a background handler for receiving all the camera callback.
     */
    private Handler backgroundHandler;

    /**
     * Holder for all the related camera observers.
     */
    private CompositeDisposable compositeDisposable;

    private Scheduler ioScheduler;

    private byte cameraVersion;

    private boolean isJpegMode;

    /**
     * Default constructor for the current class.
     *
     * @param context     the application context.
     * @param pictureSize the size of the picture resolution.
     * @param screenSize  the screen dimensions.
     * @param isJpegMode  a flag which defines if the camera is in picture mode.
     */
    public CameraManager(Context context, Size pictureSize, Size screenSize, boolean isJpegMode) {
        this.isJpegMode = isJpegMode;
        cameraVersion = CAMERA_API_V2;
        cameraInitialization = new Camera2InitManager(context, pictureSize, screenSize, getSupportedImageFormat());
        if (cameraInitialization.getHardwareLevel() == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            Log.d(TAG, "CameraApiV1: hardware level is legacy.");
            cameraVersion = CAMERA_API_V1;
            cameraInitialization = new Camera1InitManager(context, pictureSize, screenSize, getSupportedImageFormat());
        } else {
            Log.d(TAG, String.format("CameraApiV2: hardware level is %s.", cameraInitialization.getHardwareLevel()));
        }
        this.ioScheduler = Schedulers.single();
    }

    @Override
    public Completable openCamera() {
        if (setUpCameraBeforeOpening()) {
            //camera already opened, then complete
            return Completable.complete();
        }
        return cameraInitialization.openCamera().subscribeOn(ioScheduler);
    }

    @Override
    public Completable openCamera(String cameraId) {
        if (setUpCameraBeforeOpening()) {
            //camera already opened, then complete
            return Completable.complete();
        }
        return cameraInitialization.openCamera(cameraId).subscribeOn(ioScheduler);
    }

    /**
     * @return {@code true} if camera is already opened, {@code false} otherwise
     */
    private boolean setUpCameraBeforeOpening() {
        if (isCameraOpen) {
            if (focus != null) {
                focus.setCameraCaptureSession(null);
            }
            cameraInitialization.addCameraPreviewSession();
            return true;
        }
        isCameraOpen = true;
        startBackgroundThread();
        cameraInitialization.setBackgroundThread(backgroundHandler);
        createFocusManager();
        return false;
    }

    @Override
    public boolean isCameraOpen() {
        return isCameraOpen;
    }

    @Override
    public void closeCamera() {
        if (!isCameraOpen) {
            return;
        }
        cameraInitialization.setBackgroundThread(null);
        if (focus != null) {
            focus.setBackgroundThread(null);
        }
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        focus = null;
        isCameraOpen = false;
        cameraInitialization.closeCamera();
        stopBackgroundThread();
    }

    @Override
    public Single<CameraFrame> takePicture() {
        if (!isCameraOpen) {
            return Single.error(new CameraError(CameraError.ERROR_CAMERA_IS_NOT_OPENED));
        }
        Single<CameraFrame> captureStillPicture = cameraInitialization.captureStillPicture();
        return captureStillPicture
                .subscribeOn(ioScheduler)
                .timeout(TIME_OUT_DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void focusOnArea(int x, int y) {
        if (focus != null) {
            Size previewSize = cameraInitialization.getSurfacePreviewSize();
            if (previewSize.getWidth() * previewSize.getHeight() != 0) {
                focus.focusOnArea(new Point(x, y), previewSize);
            }
        }
    }

    @Override
    public void lockFocus() {
        if (focus != null) {
            focus.lockFocus();
        }
    }

    @Override
    public void unlockFocus() {
        if (focus != null) {
            focus.unlockFocus();
        }
    }

    @Override
    public List<Size> getSupportedPictureResolutions(String cameraId) {
        return cameraInitialization.getSupportedPictureResolutions(cameraId);
    }

    @Override
    public Size getPictureSize() {
        return cameraInitialization.getPictureSize();
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        cameraInitialization.setPictureSize(pictureSize);
    }

    @Override
    public Size getPreviewSize() {
        return cameraInitialization.getOptimalPreviewSize();
    }

    @Override
    public void setPreviewSurface(SurfaceTexture surface, Size previewSize) {
        cameraInitialization.setPreviewSurface(surface, previewSize);
        if (isCameraOpen) {
            cameraInitialization.startPreview();
        }
    }

    @Override
    public int getImageFormat() {
        return cameraInitialization.getImageFormat();
    }

    @Override
    public void setImageFormat() {
        cameraInitialization.setImageFormat(getSupportedImageFormat());
    }

    @Override
    public boolean isCamera2Api() {
        return cameraInitialization instanceof Camera2InitManager;
    }

    @Override
    public boolean isVideoMode() {
        return !isJpegMode;
    }

    @Override
    public void getCameraSensorDataAsync(MetadataCameraCallback metadataCameraCallback) {
        if (cameraInitialization == null) {
            Log.w(TAG, "getCameraSensorDataAsync. Status: Null initialisation object cannot read the camera sensor data.");
            return;
        }
        Size currentSize = ExtensionsKt.getResolution(this);
        Log.d(TAG, "getCameraSensorDataAsync. Status: Logging camera sensor data.");
        CameraDeviceData data = cameraInitialization.getCameraDeviceData();
        metadataCameraCallback.onCameraSensorCallback(
                LocalDateTime.now().toDateTime().getMillis(),
                data.getFocalLength(),
                data.getHorizontalFOV(),
                data.getVerticalFOV(),
                data.getLensAperture(),
                currentSize.getWidth(),
                currentSize.getHeight());
        Log.d(TAG,
                String.format("getCameraSensorDataAsync. Status: success. Current fLength: %s. Current hFoV: %s. Current vFoV: %s. Current apperture: %s",
                        data.getFocalLength(),
                        data.getHorizontalFOV(),
                        data.getVerticalFOV(),
                        data.getLensAperture()));
    }

    @Override
    public List<CameraDeviceData> getCameraDevices() {
        return cameraInitialization.getCameraDevices();
    }

    private int getSupportedImageFormat() {
        if (isJpegMode) {
            //default format for picture mode
            return ImageFormat.JPEG;
        }
        //select default raw format for camera frames.
        if (cameraVersion == CAMERA_API_V1) {
            //Default format for Camera 1 API in recording mode.
            //Format which is guaranteed to be supported by the old Camera since API level 12
            //having the generic format YVU.
            return ImageFormat.NV21;
        } else {
            //Default format for Camera 2 API in recording mode.
            //Format represented by three separate buffers of data, one for each color plane,
            //having the generic format YUV.
            return (ImageFormat.YUV_420_888);
        }
    }

    /**
     * Create the focus manager when the camera is ready.
     * Wait for the {@link android.hardware.camera2.CameraCharacteristics}, {@link CaptureRequest.Builder}
     * and {@link CameraCaptureSession}.
     */
    private void createFocusManager() {
        compositeDisposable = new CompositeDisposable();
        if (cameraVersion == CAMERA_API_V2) {
            compositeDisposable.add(Observable.combineLatest(cameraInitialization.getCameraCharacteristicsEvent(),
                    cameraInitialization.getCaptureRequestBuilderEvent(),
                    cameraInitialization.getCaptureSessionEvent(),
                    Camera2FocusManager::new)
                    .take(1)
                    .subscribe(focusManager -> {
                        focus = focusManager;
                        focus.setBackgroundThread(backgroundHandler);
                        observeOnCaptureSession();
                        observeOnCaptureRequestBuilder();
                    }));
        } else if (cameraVersion == CAMERA_API_V1) {
            compositeDisposable.add(Observable.combineLatest(cameraInitialization.getCameraParameter(),
                    cameraInitialization.getCameraService(),
                    Camera1FocusManager::new)
                    .take(1)
                    .subscribe(focusManager -> {
                        focus = focusManager;
                        focus.setBackgroundThread(backgroundHandler);
                        observeOnCameraChanges();
                    }));
        }
    }

    /**
     * Sets an observer for the {@code Camera} session event.
     */
    private void observeOnCameraChanges() {
        cameraInitialization.getCameraService()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Optional<android.hardware.Camera>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(Optional<android.hardware.Camera> camera) {
                        if (focus != null) {
                            if (camera.isPresent()) {
                                focus.setCamera(camera.get());
                            } else {
                                focus.setCamera(null);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (focus != null) {
                            focus.setCamera(null);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (focus != null) {
                            focus.setCamera(null);
                        }
                    }
                });
    }

    /**
     * Set an observer for the {@code CameraCaptureSession} event.
     */
    private void observeOnCaptureSession() {
        cameraInitialization.getCaptureSessionEvent()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CameraCaptureSession>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(CameraCaptureSession cameraCaptureSession) {
                        if (focus != null) {
                            focus.setCameraCaptureSession(cameraCaptureSession);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (focus != null) {
                            focus.setCameraCaptureSession(null);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (focus != null) {
                            focus.setCameraCaptureSession(null);
                        }
                    }
                });
    }

    /**
     * Set an observer for the {@code CaptureRequest.Builder}e event.
     */
    private void observeOnCaptureRequestBuilder() {
        cameraInitialization.getCaptureRequestBuilderEvent().subscribe(new Observer<CaptureRequest.Builder>() {
            @Override
            public void onSubscribe(Disposable d) {
                compositeDisposable.add(d);
            }

            @Override
            public void onNext(CaptureRequest.Builder builder) {
                if (focus != null) {
                    focus.setCaptureRequestBuilder(builder);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (focus != null) {
                    focus.setCameraCaptureSession(null);
                }
            }

            @Override
            public void onComplete() {
                if (focus != null) {
                    focus.setCaptureRequestBuilder(null);
                }
            }
        });
    }

    /**
     * Starts a background thread and its {@link Handler} for running the camera tasks that shouldn't block the UI.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackgroundHandler");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if (backgroundThread == null) {
            return;
        }
        try {
            backgroundThread.quitSafely();
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, String.format("stopBackground: %s", e.getMessage()));
        }
    }

}