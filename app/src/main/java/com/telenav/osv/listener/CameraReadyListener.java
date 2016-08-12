package com.telenav.osv.listener;

/**
 * Created by Kalman on 11/18/15.
 */
public interface CameraReadyListener {
    /**
     * Called when a camera has been successfully opened. This allows the
     * main activity to continue setup operations while the camera
     * sets up in a different thread.
     */
    void onCameraReady();

    /**
     * Called when the camera failed to initialize
     */
    void onCameraFailed();
}
