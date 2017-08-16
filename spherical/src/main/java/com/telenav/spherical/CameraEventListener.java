package com.telenav.spherical;

import com.telenav.spherical.view.JpegInputStream;

/**
 * Created by Kalman on 2/6/16.
 */
public interface CameraEventListener {
    void onStarted(WifiCamManager.CameraTask task);

    void onImageSizeChanged(int width, int height);

    void onImageSizeReceived();

    void onImageTaken();

    void onImageReceived();

    void onPreviewStarted(JpegInputStream inputStream);

    void onPreviewStopped();

    void onImageListReceived();

    void onDisconnected();
}
