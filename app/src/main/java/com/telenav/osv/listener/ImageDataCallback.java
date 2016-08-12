package com.telenav.osv.listener;

import com.telenav.osv.external.network.ImageData;

/**
 * Created by Kalman on 2/7/16.
 */
public interface ImageDataCallback {
    void onImageDataReceived(ImageData imgData);

    void onRequestFailed();
}
