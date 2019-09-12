package com.telenav.osv.ui.fragment.camera.preview.viewmodel;

import java.util.ArrayList;
import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Application;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.command.PhotoCommand;
import com.telenav.osv.common.model.data.SnackBarItem;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.recorder.camera.util.CameraError;
import com.telenav.osv.recorder.camera.util.TextureViewPreview;
import com.telenav.osv.ui.fragment.camera.preview.presenter.CameraPreviewPresenter;
import com.telenav.osv.ui.fragment.camera.preview.presenter.CameraPreviewPresenterImpl;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

/**
 * {@code ViewModel} class that handles the view logic for the camera preview screen regarding the camera operations.
 */
public class CameraPreviewViewModel extends AndroidViewModel {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    private static final String TAG = CameraPreviewViewModel.class.getSimpleName();

    /**
     * The animation duration of the camera focus indicator.
     */
    private final static int FOCUS_ANIMATION_DURATION = 1000;

    /**
     * Observable instance for sending an event when a snack bar should be displayed.
     * The observable emits a {@link SnackBarItem} which contains all the details for displaying the snack bar.
     */
    private MutableLiveData<SnackBarItem> snackBarObservable;

    /**
     * Observable instance for sending an event when the camera permission need to be granted in order to open the camera.
     * The observable emits an array of {@code String}, containing all the required permissions.
     */
    private MutableLiveData<String[]> cameraPermissionsObservable;

    /**
     * Instance to the presenter which handles the business logic.
     */
    private CameraPreviewPresenter presenter;

    /**
     * Default constructor for the current class.
     */
    public CameraPreviewViewModel(Application application) {
        super(application);
        this.presenter = new CameraPreviewPresenterImpl(
                ((OSVApplication) application).getCamera(),
                ((OSVApplication) application).getRecorder(),
                ((OSVApplication) application).getAppPrefs());
    }


    /**
     * @return {@code true} if the recording is started, {@code false} otherwise.
     */
    public boolean isRecording() {
        return presenter.isRecording();
    }

    /**
     * Opens the camera if the permissions are granted.
     */
    public void openCamera() {
        if (!checkPermissionsForCamera()) {
            return;
        }
        presenter.openCamera(new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Open camera completed.");
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "openCamera. Status: error. Message: " + e.getMessage());
                if (e.getMessage().equals(CameraError.ERROR_CAMERA_PERMISSION_NEEDED)) {
                    checkPermissionsForCamera();
                } else if (e.getMessage().equals(CameraError.ERROR_CAMERA_INIT_FAILED_TO_OPEN)) {
                    if (snackBarObservable != null) {
                        snackBarObservable.setValue(new SnackBarItem(getApplication().getString(R.string.open_camera_failed), Snackbar.LENGTH_SHORT, null, null));
                    }
                }
            }
        });
    }

    /**
     * @return an observable representing {@link #snackBarObservable}.
     */
    public LiveData<SnackBarItem> getSnackBarObservable() {
        if (snackBarObservable == null) {
            snackBarObservable = new MutableLiveData<>();
        }
        snackBarObservable.setValue(null);
        return snackBarObservable;
    }

    /**
     * @return an observable representing {@link #cameraPermissionsObservable}.
     */
    public LiveData<String[]> getCameraPermissionsObservable() {
        if (cameraPermissionsObservable == null) {
            cameraPermissionsObservable = new MutableLiveData<>();
        }
        return cameraPermissionsObservable;
    }

    /**
     * Closes the camera session.
     */
    public void closeCamera() {
        presenter.closeCamera();
    }

    /**
     * Locks the focus and animates a focus indicator at the tapped area.
     * @param focusLockedIndicator the focus indicator to be animated.
     * @param x the value on the Ox axis for the tapped area.
     * @param y the value on the Oy axis for the tapped area.
     */
    public void onLongPressGesture(FrameLayout focusLockedIndicator, float x, float y) {
        if (!presenter.isFocusModeStatic()) {
            animateFocusIndicator(focusLockedIndicator, x, y);
            presenter.lockFocus();
        }
    }

    /**
     * The double tapped event will trigger the capture of a picture.
     */
    public void onDoubleTapGesture() {
        EventBus.post(new PhotoCommand());
    }

    /**
     * Focuses the camera on the given tapped area and animates a focus indicator on that area.
     * @param focusIndicator the focus indicator to be animated.
     * @param x the value on the Ox axis for the tapped area.
     * @param y the value on the Oy axis for the tapped area.
     */
    public void onSingleTapGesture(ImageView focusIndicator, float x, float y) {
        if (presenter.isCameraContainerSmall()) {
            //TODO Switch Map with Camera preview
        } else if (!presenter.isFocusModeStatic()) {
            animateFocusIndicator(focusIndicator, x, y);
            presenter.focusOnArea(x, y);
        }
    }

    /**
     * Sets the texture view to the camera.
     * @param textureView the custom texture view instance.
     */
    public void setTextureView(TextureViewPreview textureView) {
        presenter.setTextureView(textureView);
    }

    /**
     * Sets the camera container size in order to fit the camera preview if the camera is not in full screen mode.
     * @param size the size of the camera container.
     */
    public void setCameraContainerSize(Size size) {
        presenter.setCameraContainerSize(size);
    }

    public void setDeviceOrientation(int deviceOrientation) {
        presenter.setDeviceOrientation(deviceOrientation);
    }

    public boolean isCameraContainerSmall() {
        return presenter.isCameraContainerSmall();
    }

    /**
     * @return {@code true} if the camera permissions are granted, {@code false} otherwise.
     */
    private boolean checkPermissionsForCamera() {
        ArrayList<String> needed = new ArrayList<>();
        int cameraPermitted = ContextCompat.checkSelfPermission(getApplication().getApplicationContext(), Manifest.permission.CAMERA);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }

        if (needed.size() > 0) {
            String[] array = new String[needed.size()];
            needed.toArray(array);
            cameraPermissionsObservable.setValue(array);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Animates the focus indicator to the given area.
     * @param focusIndicator the view of the focus indicator that should be animated.
     * @param focusX the value on the Ox axis.
     * @param focusY the value on the Oy axis.
     */
    private void animateFocusIndicator(View focusIndicator, float focusX, float focusY) {
        int halfDivider = 2;
        float circleRadius = focusIndicator.getWidth() / halfDivider;
        if (focusX - circleRadius < 0) {
            focusX = 0;
        } else {
            focusX = focusX - circleRadius;
        }
        if (focusY - circleRadius < 0) {
            focusY = 0;
        } else {
            focusY = focusY - circleRadius;
        }
        focusIndicator.setVisibility(View.VISIBLE);
        focusIndicator.setX(focusX);
        focusIndicator.setY(focusY);
        ObjectAnimator circleFade = ObjectAnimator.ofFloat(focusIndicator, View.ALPHA, 1f, 0f);
        circleFade.setDuration(FOCUS_ANIMATION_DURATION);
        circleFade.start();
    }
}
