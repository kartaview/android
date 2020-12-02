package com.telenav.osv.recorder.camera.model;

import android.graphics.ImageFormat;

/**
 * Model class for a camera frame.
 * This is used for all the possible image formats of the camera.
 */
public class CameraFrame {
    /**
     * The format of the camera frame.
     * This should be one of the values from the {@link ImageFormat} class.
     */
    private int imageFormat;

    /**
     * The buffer containing the frame data.
     */
    private byte[] frameData;

    /**
     * Constructor for the frame data.
     * @param frameData the data of the frame.
     * @param imageFormat the format of the current frame.
     */
    public CameraFrame(byte[] frameData, int imageFormat) {
        this.frameData = frameData;
        this.imageFormat = imageFormat;
    }


    /**
     * @return the image format as one of the following values defined in the {@link ImageFormat} class.
     */
    public int getImageFormat() {
        return imageFormat;
    }

    /**
     * @return the frame data as an concatenated array for the YUV format in this order, or
     * the whole frame data for the JPEG or NV format.
     */
    public byte[] getFrameData() {
        return frameData;
    }
}