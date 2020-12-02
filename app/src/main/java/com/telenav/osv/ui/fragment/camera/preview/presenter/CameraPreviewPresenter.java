package com.telenav.osv.ui.fragment.camera.preview.presenter;

import com.telenav.osv.recorder.camera.util.TextureViewPreview;
import com.telenav.osv.utils.Size;
import io.reactivex.CompletableObserver;

/**
 * The camera preview presenter which holds all the available business logic required by the view.
 */
public interface CameraPreviewPresenter {

    /**
     * @return {@code true} if the recording is started, {@code false} otherwise.
     */
    boolean isRecording();

    /**
     * Opens the camera and sets the observer which will be notified when the operation was finished.
     * @param observer the instance of the observer which will be subscribed for camera open events.
     */
    void openCamera(CompletableObserver observer);

    /**
     * Closes the camera session.
     */
    void closeCamera();

    /**
     * Sets the size for the camera container to transform the camera preview.
     * @param cameraContainerSize the size of the camera container
     */
    void setCameraContainerSize(Size cameraContainerSize);

    /**
     * @return {@code true} if the camera is in a small container and the map is in the large container, {@code false} otherwise.
     */
    boolean isCameraContainerSmall();

    /**
     * Focuses the camera on the tapped area.
     * @param x the value on the Ox axis.
     * @param y the value on the Oy axis.
     */
    void focusOnArea(float x, float y);

    /**
     * Locks the camera focus until the camera preview is tapped again.
     */
    void lockFocus();

    //TODO: Investigate the need of static focus
    boolean isFocusModeStatic();

    /**
     * Sets the custom texture view class representing the camera preview holder.
     * @param textureView the instance of the custom {@code TextureViewPreview}
     */
    void setTextureView(TextureViewPreview textureView);

    /**
     * Sets the device orientation.
     * Possible values for orientation:
     * <ul>
     * <li>{@link android.view.Surface#ROTATION_0}</li> portrait
     * <li>{@link android.view.Surface#ROTATION_90}</li> landscape right
     * <li>{@link android.view.Surface#ROTATION_180}</li> landscape left
     * </ul>
     * @param deviceOrientation the device orientation value
     */
    void setDeviceOrientation(int deviceOrientation);
}
