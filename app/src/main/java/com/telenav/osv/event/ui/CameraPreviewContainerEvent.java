package com.telenav.osv.event.ui;

import com.telenav.osv.event.OSVEvent;

/**
 * Event class used for sending the camera preview size.
 */
public class CameraPreviewContainerEvent extends OSVEvent {

    /**
     * {@code true} if the camera container is small and the focus is on the  map container,
     * {@code false} otherwise.
     */
    private boolean isCameraContainerSmall;

    /**
     * The width dimension of the camera preview.
     */
    private int width;

    /**
     * The height dimension of the camera preview.
     */
    private int height;

    /**
     * Default constructor for the current event class.
     * @param width the width of the camera preview.
     * @param height the height of the camera preview.
     * @param isCameraContainerSmall a flag represented by {@link #isCameraContainerSmall}.
     */
    public CameraPreviewContainerEvent(int width, int height, boolean isCameraContainerSmall) {
        this.width = width;
        this.height = height;
        this.isCameraContainerSmall = isCameraContainerSmall;
    }

    /**
     * @return the width of the camera preview.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height of the camera preview.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the value of the {@link #isCameraContainerSmall}.
     */
    public boolean isCameraContainerSmall() {
        return isCameraContainerSmall;
    }
}
