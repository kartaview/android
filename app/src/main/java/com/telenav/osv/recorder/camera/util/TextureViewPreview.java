package com.telenav.osv.recorder.camera.util;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;

/**
 * Custom class handling the camera texture view and it's preview size.
 * @author cameliao
 */

public class TextureViewPreview {

    private static final String TAG = TextureViewPreview.class.getSimpleName();

    /**
     * The value for a preview rotation when the device is in landscape.
     */
    private static final int DEGREES_90 = 90;

    /**
     * The value for a preview rotation when the device is in portrait.
     */
    private static final int DEGREES_180 = 180;

    /**
     * Instance of {@code TextureView} representing the camera preview.
     */
    private TextureView textureView;

    /**
     * The camera preview size defined in pixels.
     */
    private Size previewSize;

    /**
     * The size of the camera preview container.
     */
    private Size containerSize;

    /**
     * The width of the  camera surface received when the camera surface texture preview is available.
     */
    private int width;

    /**
     * The height of the  camera surface received when the camera surface texture preview is available.
     */
    private int height;

    /**
     * The listener for the camera surface texture changes.
     */
    private SurfaceChangedListener listener;

    /**
     * Handler to execute operations on the UI thread.
     */
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Possible values for orientation:
     * <ul>
     * <li>{@link android.view.Surface#ROTATION_0}</li> portrait
     * <li>{@link android.view.Surface#ROTATION_90}</li> landscape right
     * <li>{@link android.view.Surface#ROTATION_180}</li> landscape left
     * </ul>
     */
    private int orientation;

    /**
     * Default constructor for the current class.
     * @param textureView the view holding the camera preview.
     */
    public TextureViewPreview(TextureView textureView) {
        this.textureView = textureView;
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable");
                setSize(width, height);
                configureTransform();
                resizeTextureView();
                if (listener != null) {
                    listener.onSurfaceChanged(textureView.getSurfaceTexture(), new Size(width, height));
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureSizeChanged");
                setSize(width, height);
                configureTransform();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed");
                setSize(0, 0);
                listener.onSurfaceChanged(null, new Size(width, height));
                //return false if the surface will be reused, otherwise return true and the surface will be destroyed.
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * Sets the size of camera preview container.
     * @param containerSize the size of the camera preview container, used to resize the preview size.
     */
    public void setCameraContainerSize(Size containerSize) {
        this.containerSize = containerSize;
        Log.d(TAG, String.format("Camera container size: height - %s width -  %s", containerSize.getHeight(), containerSize.getWidth()));
        if (previewSize != null) {
            resizeTextureView();
        }
    }

    /**
     * Sets the surface listener for receiving updates when the camera surface texture changes.
     * @param listener the {@link android.view.TextureView.SurfaceTextureListener} instance.
     */
    public void setSurfaceChangedListener(SurfaceChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the preview size to the {@link #textureView}.
     * @param previewSize the camera preview size.
     */
    public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;
        Log.d(TAG, String.format("Camera preview size: height - %s width -  %s", previewSize.getHeight(), previewSize.getWidth()));
        if (containerSize != null) {
            resizeTextureView();
        }
    }

    /**
     * @return the width of the camera surface.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height of the camera surface.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return {@code true} if the surface texture is available for camera preview, {@code false} otherwise.
     */
    public boolean isAvailable() {
        return textureView.getSurfaceTexture() != null;
    }

    /**
     * Sets the device orientation.
     * @param orientation the device orientation value.
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    /**
     * Sets the size for the camera surface view when a new update is received from {@link android.view.TextureView.SurfaceTextureListener}.
     * @param width the width of the camera surface.
     * @param height the height of the camera surface.
     */
    private void setSize(int width, int height) {
        Log.d(TAG, String.format("Camera surface size: height - %s width -  %s", height, width));
        this.width = width;
        this.height = height;
    }

    /**
     * Configures the necessary transformation of the {@link #textureView},
     * taking into account the device orientation, the size of camera preview and the size of camera surface.
     */
    private void configureTransform() {
        if (null == textureView || null == previewSize) {
            return;
        }
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == orientation || Surface.ROTATION_270 == orientation) {
            //Rotate camera preview when is landscape.
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) height / previewSize.getHeight(),
                    (float) width / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(DEGREES_90 * (orientation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == orientation) {
            matrix.postRotate(DEGREES_180, centerX, centerY);
        }
        mainHandler.post(() -> textureView.setTransform(matrix));
    }

    /**
     * Used to resize the camera preview to fit to the camera container
     * and also to maintain the aspect ratio of the preview.
     */
    private void resizeTextureView() {
        if (previewSize == null || containerSize == null) {
            return;
        }
        mainHandler.post(() -> {
            textureView.setLayoutParams(CameraHelper.getPreviewLayoutParams(containerSize, previewSize));
            textureView.requestLayout();
        });
    }

    /**
     * Listener interface for camera surface texture updates.
     */
    public interface SurfaceChangedListener {
        /**
         * The method is used for sending updates when the camera surface texture is changed.
         * @param surface the surface of the camera preview.
         * @param previewSize the size of the camera surface.
         */
        void onSurfaceChanged(SurfaceTexture surface, Size previewSize);
    }
}