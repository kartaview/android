package com.telenav.osv.recorder.camera.init;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.view.OrientationEventListener;

import com.telenav.osv.event.EventBus;
import com.telenav.osv.recorder.camera.model.CameraDeviceData;
import com.telenav.osv.recorder.camera.model.CameraFrame;
import com.telenav.osv.recorder.camera.util.CameraHelper;
import com.telenav.osv.recorder.camera.util.SizeMap;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.SingleEmitter;

/**
 * Abstract class containing all the common functionality for the Camera1 and Camera2 APIs.
 */
public abstract class CameraInitManager implements CameraInitialization {

    static final int JPEG_CAPTURE_QUALITY = 100;

    private static final String TAG = CameraInitManager.class.getSimpleName();

    /**
     * Instance of the camera preview containing the camera view, {@link android.graphics.SurfaceTexture}.
     */
    SurfaceTexture surfaceTexture;

    /**
     * The surface preview size for a camera session.
     */
    Size surfacePreviewSize;

    /**
     * The optimal preview size for a camera session.
     */
    Size optimalPreviewSize = new Size(0, 0);

    /**
     * The picture sizes supported by the current camera.
     */
    SizeMap pictureSizes = new SizeMap();

    /**
     * Represents a background handler for receiving all the camera callbacks.
     */
    Handler backgroundHandler;

    /**
     * Possible values for device orientation: 0, 90, 180, 270.
     */
    int deviceOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;

    OrientationEventListener orientationListener;

    /**
     * The current picture size applied for taking a picture.
     */
    Size pictureSize;

    /**
     * The size of the camera container.
     */
    Size containerSize;

    /**
     * The current image format which is an element from {@link ImageFormat}.
     */
    int imageFormat = ImageFormat.JPEG;

    /**
     * Emitter used when a frame is available for sending the bytes array and the timestamp.
     */
    SingleEmitter<CameraFrame> frameByteEmitter;

    /**
     * A flag used to determine when a frame should be taken.
     */
    boolean shouldTakeFrame;

    /**
     * The frame buffer which contains the raw frame data and free allocated spaces to fill it with the converted data.
     */
    byte[] frameData;

    /**
     * Object holding the camera id used for camera opening.
     */
    CameraDeviceData cameraDeviceData;

    /**
     * The list with all the supported back cameras.
     */
    List<CameraDeviceData> cameraDeviceDataList = new ArrayList<>();

    CameraInitManager(Context context, Size containerSize, int imageFormat) {
        setImageFormat(imageFormat);
        orientationListener = new OrientationListener(context);
        this.containerSize = containerSize;
    }

    @Override
    public Size getSurfacePreviewSize() {
        return surfacePreviewSize;
    }

    @Override
    public Size getOptimalPreviewSize() {
        return optimalPreviewSize;
    }

    @Override
    public void setPreviewSurface(SurfaceTexture surfaceTexture, Size previewSize) {
        this.surfacePreviewSize = previewSize;
        this.surfaceTexture = surfaceTexture;
    }

    @Override
    public Size getPictureSize() {
        return pictureSize;
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        if (pictureSize == null || pictureSize.getWidth() == 0 || pictureSize.getHeight() == 0) {
            this.pictureSize = getDefaultPictureSize();
            Log.d(TAG, String.format("setPictureSize. Status: Cached picture size not detected. Message: width - %s height - %s", this.pictureSize.getWidth(), this.pictureSize.getHeight()));
        } else {
            this.pictureSize = pictureSize;
            Log.d(TAG, String.format("setPictureSize. Status: Cached picture size detected. Message: width - %s height - %s", this.pictureSize.getWidth(), this.pictureSize.getHeight()));
        }
    }

    @Override
    public void setBackgroundThread(Handler backgroundHandler) {
        this.backgroundHandler = backgroundHandler;
    }

    @Override
    public int getImageFormat() {
        return imageFormat;
    }

    @Override
    public void setImageFormat(int imageFormat) {
        Log.d(TAG, "setImageFormat. ImageFormat value: " + imageFormat);
        this.imageFormat = imageFormat;
        addCameraPreviewSession();
    }

    @Override
    public CameraDeviceData getCameraDeviceData() {
        return cameraDeviceData;
    }

    /**
     * @return {@code true} if the image format is JPEG, {@code false} otherwise.
     */
    boolean isImageFormatJpeg() {
        return imageFormat == ImageFormat.JPEG;
    }

    /**
     * Initializes the frame buffer by allocating the size for the raw frame and for the converted data.
     * The buffer length is depending on the image format.
     * <ul>
     * <li>{@link ImageFormat#NV21}</li>
     * <p>
     * The raw frame data length is: width * height + (width * height) / 2.
     * The converted frame data length is: width * height + (width * height) / 2.
     * </p>
     * <li>{@link ImageFormat#YUV_420_888}</li>
     * <p>
     * The raw frame data length is: width * height + (width * height) / 2.
     * The converted frame data length is: width * height + (width * height) / 2.
     * </p>
     * </ul>
     * The total frame buffer is set to the highest needed space which is for the YUV format, therefore the total length for the frame buffer is:
     * 3 * width * height.
     * In case the frame format is NV21 between the raw frame data and the converted frame data will be a padding of (width * height) / 2.
     */
    void initFrameBuffer(Size frameSize) {
        //The buffer multiplier is defined by summing up the raw data length and converted data length.
        int bufferMultiplier = 3;
        frameData = ByteBuffer.allocateDirect(bufferMultiplier * frameSize.getWidth() * frameSize.getHeight()).array();
    }

    protected abstract void setDeviceOrientation(int deviceOrientation);

    /**
     * @return a default resolution for the picture size.
     * The resolution should be the closest size to the preview container size without exceeding the container.
     */
    private Size getDefaultPictureSize() {
        return CameraHelper.chooseOptimalPictureSize(pictureSizes, optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight());
    }

    /**
     * @param cameraId camera identifier.
     * @return the camera data for the given id if was found in the {@link #cameraDeviceDataList},
     * otherwise returns null.
     */
    protected CameraDeviceData getCameraById(String cameraId) {
        for (CameraDeviceData data : cameraDeviceDataList) {
            if (data.getCameraId().equals(cameraId)) {
                return data;
            }
        }
        return null;
    }

    private class OrientationListener extends OrientationEventListener {

        OrientationListener(Context context) {
            super(context);
            EventBus.register(this);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }
            int value = Utils.roundOrientation(orientation, deviceOrientation);
            if (value == deviceOrientation) {
                return;
            } else {
                Log.d(TAG, "onOrientationChanged. Status: changed. Message: The new orientation is " + value);
                deviceOrientation = value;
                setDeviceOrientation(value);
            }
        }

    }
}
