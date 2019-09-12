package com.telenav.osv.ui.fragment.camera.preview.presenter;

import android.graphics.SurfaceTexture;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.camera.util.TextureViewPreview;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import androidx.annotation.NonNull;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Concrete implementation for {@code CameraPreviewPresenter}.
 * @author cameliao
 * @see CameraPreviewPresenter
 */
public class CameraPreviewPresenterImpl implements CameraPreviewPresenter, TextureViewPreview.SurfaceChangedListener {

    private static final String TAG = CameraPreviewPresenterImpl.class.getSimpleName();

    /**
     * Instance to the camera preview logic.
     */
    private TextureViewPreview textureViewPreview;

    /**
     * Instance to the {@link Camera} implementation.
     */
    private Camera camera;

    /**
     * Instance to the {@link RecorderManager} implementation.
     */
    private RecorderManager recorder;

    /**
     * The application preferences for reading the user's preferences.
     */
    private ApplicationPreferences appPrefs;

    /**
     * {@code true} if the camera container is small and the focus is on the  map container,
     * {@code false} otherwise.
     */
    private boolean isCameraContainerSmall;

    /**
     * Default constructor for the current class.
     * @param camera the camera manager instance.
     * @param recorder the recorder manager instance.
     * @param appPrefs the application preferences.
     */
    public CameraPreviewPresenterImpl(@NonNull Camera camera, @NonNull RecorderManager recorder,
                                      @NonNull ApplicationPreferences appPrefs) {
        this.camera = camera;
        this.recorder = recorder;
        this.appPrefs = appPrefs;
        recorder.setCamera(camera);
        setCameraImageFormat();
        setPictureSize();
    }

    @Override
    public void setDeviceOrientation(int deviceOrientation) {
        textureViewPreview.setOrientation(deviceOrientation);
    }

    @Override
    public boolean isRecording() {
        return recorder.isRecording();
    }

    @Override
    public void openCamera(CompletableObserver observer) {
        if (observer == null) {
            return;
        }
        Log.d(TAG, "openCamera:texture available");
        camera.openCamera()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    @Override
    public void closeCamera() {
        camera.closeCamera();
    }

    @Override
    public void setCameraContainerSize(Size cameraContainerSize) {
        if (textureViewPreview != null) {
            textureViewPreview.setCameraContainerSize(cameraContainerSize);
        }
    }

    @Override
    public boolean isCameraContainerSmall() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_TAGGING_MODE);
    }

    @Override
    public void focusOnArea(float x, float y) {
        // A single tap equals to touch-to-focus
        camera.unlockFocus();
        camera.focusOnArea((int) x, (int) y);
    }

    @Override
    public void lockFocus() {
        camera.lockFocus();
    }

    @Override
    public boolean isFocusModeStatic() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_FOCUS_MODE_STATIC);
    }

    @Override
    public void setTextureView(TextureViewPreview textureView) {
        this.textureViewPreview = textureView;
        textureViewPreview.setSurfaceChangedListener(this);
        Size previewSize = camera.getPreviewSize();
        textureViewPreview.setPreviewSize(previewSize);
    }

    @Override
    public void onSurfaceChanged(SurfaceTexture surface, Size previewSize) {
        camera.setPreviewSurface(surface, previewSize);
    }

    /**
     * Sets the saved pictures size from the preferences to the camera.
     */
    private void setPictureSize() {
        camera.setPictureSize(new Size(appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_WIDTH),
                appPrefs.getIntPreference(PreferenceTypes.K_RESOLUTION_HEIGHT)));
    }

    /**
     * Sets the camera image format for current recording mode.
     */
    private void setCameraImageFormat() {
        camera.setImageFormat(!appPrefs.getBooleanPreference(PreferenceTypes.K_VIDEO_MODE_ENABLED));
    }
}