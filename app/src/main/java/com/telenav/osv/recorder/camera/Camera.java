package com.telenav.osv.recorder.camera;

import java.util.List;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.utils.Size;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Interface that holds all the available camera operations.
 * Created by cameliao on 1/30/18.
 */

public interface Camera {

    /**
     * Opens the camera and notifies the registered subscribers about the camera initialisation status.
     * If camera is already opened, the method returns {@code complete} without trying to reopen the camera.
     * @return a {@link Completable} instance to indicate when open camera operation is completed or when an exception was encountered.
     */
    Completable openCamera();

    /**
     * @return true if the camera is open, false otherwise.
     */
    boolean isCameraOpen();

    /**
     * Closes the camera by releasing the camera resources.
     */
    void closeCamera();

    /**
     * Takes a picture and notifies the registered subscribers when the picture was taken and saved.
     * @return a {@code Single} instance who's observer will be notified with the frames's data
     * when a picture was successfully taken.
     */
    Single<CameraFrame> takePicture();

    /**
     * Focuses the camera on the given screen point
     * @param x the x coordinate relative to the camera surface width and the screen view.
     * @param y the y coordinate relative to the camera surface height and the screen view.
     */
    void focusOnArea(int x, int y);

    /**
     * Locks the camera focus.
     * While the focus is locked on an object, the object will be still in focus even if the device is moving.
     */
    void lockFocus();

    /**
     * Unlocks the focus.
     */
    void unlockFocus();

    /**
     * @return a list representing the {@code Size} in width and height for the picture resolutions
     */
    List<Size> getSupportedPictureResolutions();

    /**
     * Returns the size of the camera preview which has 4:3 aspect ratio.
     * @return the size of the camera preview.
     */
    Size getPreviewSize();

    /**
     * @return the size of the pictures taken by the camera having aspect ration 4:3.
     */
    Size getPictureSize();

    /**
     * Sets the picture resolution to the camera.
     * If the given resolution has the width or height equals to 0, a default picture resolution will be automatically selected.
     * When selecting an automatically resolution, the 5MP has priority, than the rest of the resolutions from the lowest to highest one.
     * @param pictureSize the size for a picture taken by the camera.
     */
    void setPictureSize(Size pictureSize);

    /**
     * Updates the surface of the camera preview and the size of it.
     * @param surface the {@code SurfaceTexture} which holds the camera preview.
     * @param previewSize the size of the camera preview which is equal with the size of camera surface.
     */
    void setPreviewSurface(SurfaceTexture surface, Size previewSize);

    /**
     * @return the format of the images received from camera.
     * The format is one of the following:
     * <ul>
     * <li>{@link ImageFormat#JPEG} - representing the format for the picture mode.</li>
     * <li>{@link ImageFormat#NV21} - representing the format for camera1 preview frames.</li>
     * <li>{@link ImageFormat#YUV_420_888} -  representing the format for the camera2 preview frames.</li>
     * </ul>
     */
    int getImageFormat();

    /**
     * Sets the image format for camera frames.
     * If the parameters is {@code false} then the default image format for Camera1 API is {@link ImageFormat#NV21}
     * and for Camera2 API {@link ImageFormat#YUV_420_888}.
     * @param isJpegFormat {@code true} if the image format is {@link ImageFormat#JPEG}, {@code false} otherwise.
     */
    void setImageFormat(boolean isJpegFormat);

    /**
     * @return {@code true} if the camera API used is Camera2, {@code false} otherwise.
     */
    boolean isCamera2Api();
}