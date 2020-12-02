package com.telenav.osv.recorder.camera.init;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;

import com.telenav.osv.recorder.camera.model.CameraDeviceData;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.utils.Size;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.subjects.Subject;

/**
 * Interface containing all the available camera initialisation operations.
 *
 * @author cameliao
 */

public interface CameraInitialization {

    /**
     * Opens the camera.
     *
     * @return a {@code Completable} instance in order to notify the observer about the camera state.
     */
    Completable openCamera();

    /**
     * Opens the camera that has the given id.
     *
     * @return a {@code Completable} instance in order to notify the observer about the camera state.
     */
    Completable openCamera(String cameraId);

    /**
     * Closes the camera.
     */
    void closeCamera();

    /**
     * @return {@code Size} of the surface camera preview.
     */
    Size getSurfacePreviewSize();

    /**
     * @return a list representing the {@code Size} in width and height for the picture resolutions
     */
    List<Size> getSupportedPictureResolutions(String cameraId);

    /**
     * Captures a picture by taking into consideration all the selected capture requests for the auto exposure, auto focus, flash mode and orientation.
     *
     * @return a {@code Single} source in order to frame data to its subscribers.
     */
    Single<CameraFrame> captureStillPicture();

    /**
     * Method called when the camera preview changes in order to addChild the preview to the camera
     * and to set the camera surface.
     *
     * @param surfaceTexture the camera surface texture used for displaying the camera preview.
     * @param previewSize    the size of the camera preview surface.
     */
    void setPreviewSurface(SurfaceTexture surfaceTexture, Size previewSize);

    /**
     * Start the preview for the camera session.
     */
    void startPreview();

    /**
     * @return the resolution used for taking a picture.
     */
    Size getPictureSize();

    /**
     * Sets tha picture size to the camera
     *
     * @param pictureSize the resolution that should be used for taking a picture.
     */
    void setPictureSize(Size pictureSize);

    /**
     * Sets a background handler for executing all the camera operations.
     *
     * @param backgroundHandler represents a background handler for executing all the camera operations.
     */
    void setBackgroundThread(Handler backgroundHandler);

    /**
     * Add a {@code surfaceTexture} to the camera and creates a capture session with a preview.
     * The preview should be available only when the application is in foreground on the camera screen.
     */
    void addCameraPreviewSession();

    /**
     * Creates a capture session without a camera preview.
     */
    void createNoCameraPreviewSession();

    /**
     * @return {@code Size} of the optimal camera preview.
     */
    Size getOptimalPreviewSize();

    /**
     * @return the image format of the camera frames.
     */
    int getImageFormat();

    /**
     * Sets the image format.
     * The default image format is {@link android.graphics.ImageFormat#JPEG}.
     *
     * @param imageFormat teh format selected from {@link android.graphics.ImageFormat}.
     */
    void setImageFormat(int imageFormat);

    /**
     * @return a list containing all the supported back cameras
     */
    List<CameraDeviceData> getCameraDevices();


    /**
     * @return the current selected camera from the given list {@link #getCameraDevices()}
     */
    CameraDeviceData getCameraDeviceData();

    //Defined for Camera2

    default int getHardwareLevel() {
        return 0;
    }

    /**
     * @return an observable which emits the starting from the most recent {@code CameraCaptureSession} event.
     */
    default Subject<CameraCaptureSession> getCaptureSessionEvent() {
        return null;
    }

    /**
     * @return an observable which emits starting from the most recent {@code CaptureRequest.Builder} event.
     */
    default Subject<CaptureRequest.Builder> getCaptureRequestBuilderEvent() {
        return null;
    }

    /**
     * @return an observable which emits starting from the most recent {@code CameraCharacteristics} event.
     */
    default Subject<CameraCharacteristics> getCameraCharacteristicsEvent() {
        return null;
    }

    //Defined for Camera1

    /**
     * @return an observable which emits starting from the most recent {@code Camera.Parameters} event.
     */
    default Subject<Camera.Parameters> getCameraParameter() {
        return null;
    }

    /**
     * @return an observable which emits starting from the most recent {@code Camera} event.
     */
    default Subject<com.google.common.base.Optional<Camera>> getCameraService() {
        return null;
    }
}