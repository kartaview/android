package com.telenav.osv.ui.fragment.camera.preview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProviders;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.model.base.KVBaseFragment;
import com.telenav.osv.common.toolbar.KVToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.obd.ObdContract;
import com.telenav.osv.recorder.camera.util.TextureViewPreview;
import com.telenav.osv.service.CameraHandlerService;
import com.telenav.osv.ui.fragment.camera.preview.viewmodel.CameraPreviewViewModel;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.PermissionCode;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.UiUtils;

/**
 * Fragment which handles the camera interaction.
 * Use the {@link CameraPreviewFragment#newInstance()} factory method to create an instance of this fragment.
 */
public class CameraPreviewFragment extends KVBaseFragment implements ObdContract.PermissionsListener {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = CameraPreviewFragment.class.getSimpleName();

    /**
     * The {@code ViewModel} which handles the camera operations and the interaction with the preview.
     */
    private CameraPreviewViewModel viewModel;

    /**
     * The {@code ImageView} of the focus indicator, used when tapping on the camera preview.
     */
    private ImageView focusIndicator;

    /**
     * The focus indicator used for locked focus when holding long press on the camera preview.
     */
    private FrameLayout focusLockedIndicator;

    /**
     * The {@code TextureView} representing the surface for the camera preview.
     */
    private TextureView textureView;

    private FrameLayout cameraSurfaceHolder;

    /**
     * Listener which should be notified when the application is in background or in foreground mode.
     */
    private BackgroundLifecycleListener serviceBackgroundListener;

    /**
     * Listener for recording service connection.
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraHandlerService.CameraServiceBinder binder = (CameraHandlerService.CameraServiceBinder) service;
            serviceBackgroundListener = binder.getService();
            serviceBackgroundListener.onResume();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBackgroundListener = null;
        }
    };

    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * @return a new instance of {@link CameraPreviewFragment}.
     */
    public static CameraPreviewFragment newInstance() {
        return new CameraPreviewFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.viewModel = ViewModelProviders.of(this).get(CameraPreviewViewModel.class);
        observeOnSnackBarEvent();
        observeOnCameraPermissionEvent();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_camera_preview, container, false);
        cameraSurfaceHolder = view.findViewById(R.id.camera_holder);
        initCameraLayout(view);
        initCameraContainer();
        viewModel.openCamera();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        addPreviewSurface();
        viewModel.setTextureView(new TextureViewPreview(textureView));
    }

    @Override
    public void onStop() {
        super.onStop();
        removeCameraPreviewSurface();
    }

    @Override
    public void onResume() {
        super.onResume();
        //set landscape orientation
        viewModel.setDeviceOrientation(Surface.ROTATION_90);
        Activity activity = getActivity();
        if (activity != null) {
            activity.startService(new Intent(getContext(), CameraHandlerService.class));
            activity.bindService(new Intent(getContext(), CameraHandlerService.class), serviceConnection, 0);
        }
    }

    /**
     * @param portrait if portrait
     * @param previewWidth scaled to screen size
     * @param previewHeight scaled to screen size
     */
    private void resizePreview(final boolean portrait, final int previewWidth, final int previewHeight) {
        Point point = new Point();
        DimenUtils.getContentSize(getActivity(), portrait, point);
        Log.d(TAG, "resizePreview: contentHeight = " + point.y + " contentWidth = " + point.x);
        float ratio;
        if (!portrait) {
            ratio = (float) point.x / (float) previewHeight;
        } else {
            ratio = (float) point.x / (float) previewHeight;
        }

        RelativeLayout.LayoutParams previewLayoutParams;
        if (portrait) {
            int value = (int) ((float) previewWidth * ratio);
            Log.d(TAG, "resizePreview: surface height = " + value);
            previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, value);
            previewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else {
            int value = (int) ((float) previewWidth * ratio);
            Log.d(TAG, "resizePreview: surface width = " + value);
            previewLayoutParams = new RelativeLayout.LayoutParams(value, RelativeLayout.LayoutParams.MATCH_PARENT);
            previewLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        cameraSurfaceHolder.setLayoutParams(previewLayoutParams);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (serviceBackgroundListener != null) {
            serviceBackgroundListener.onPause();
        }
        Activity activity = getActivity();
        if (activity != null) {
            activity.unbindService(serviceConnection);
            if (!viewModel.isRecording()) {
                activity.stopService(new Intent(getContext(), CameraHandlerService.class));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        if (!viewModel.isRecording()) {
            viewModel.closeCamera();
        }
    }

    @Override
    public ToolbarSettings getToolbarSettings(KVToolbar toolbar) {
        return null;
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public void onPermissionGranted(int permissionCode) {
        if (permissionCode == PermissionCode.PERMISSION_CAMERA) {
            viewModel.openCamera();
        }
    }

    private void addPreviewSurface() {
        if (textureView == null) {
            textureView = new TextureView(getActivity());
        }
        textureView.setLayoutParams(
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //textureView.setOnTouchListener(previewTouchListener);
        cameraSurfaceHolder.removeAllViews();
        cameraSurfaceHolder.addView(textureView);
        textureView.bringToFront();
    }

    private void removeCameraPreviewSurface() {
        if (cameraSurfaceHolder != null) {
            cameraSurfaceHolder.removeAllViews();
        }
    }

    /**
     * Initializes the camera preview surface and the focus indicator.
     * @param view the holder of the camera views.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initCameraLayout(View view) {
        focusIndicator = view.findViewById(R.id.image_view_fragment_camera_preview_focus_indicator);
        focusLockedIndicator = view.findViewById(R.id.layout_fragment_camera_preview_focus_locked_indicator);
    }

    /**
     * Initializes the camera container for the camera preview.
     */
    private void initCameraContainer() {
        if (getActivity() == null) {
            return;
        }
        View decorView = getActivity().getWindow().getDecorView();
        Rect r = new Rect();
        decorView.getWindowVisibleDisplayFrame(r);
        int screenHeight = r.bottom;
        int screenWidth = r.right;
        viewModel.setCameraContainerSize(new Size(screenWidth, screenHeight));
    }

    /**
     * Registers an observer to listen for the snack bar event.
     */
    private void observeOnSnackBarEvent() {
        viewModel.getSnackBarObservable().observe(this, snackBarItem -> {
            if (snackBarItem != null) {
                UiUtils.showSnackBar(getContext(), getView(), snackBarItem);
            }
        });
    }

    /**
     * Registers an observer to listen for the required camera permission in order to open the camera.
     * When the event is received requests the camera permissions.
     */
    private void observeOnCameraPermissionEvent() {
        viewModel.getCameraPermissionsObservable().observe(this, permissions -> {
            if (getActivity() != null && permissions != null) {
                ActivityCompat.requestPermissions(getActivity(), permissions, KVApplication.CAMERA_PERMISSION);
            }
        });
    }

    /**
     * Listener for background events.
     */
    public interface BackgroundLifecycleListener {
        void onResume();

        void onPause();
    }

    /**
     * Gesture class which implements the {@link GestureDetector.SimpleOnGestureListener},
     * used for interacting with the camera preview.
     */
    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            if (viewModel.isCameraContainerSmall() && getActivity() != null) {
                getActivity().onBackPressed();
            } else {
                viewModel.onLongPressGesture(focusLockedIndicator, e.getX(), e.getY());
            }
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (viewModel.isCameraContainerSmall() && getActivity() != null) {
                getActivity().onBackPressed();
            } else {
                viewModel.onDoubleTapGesture();
            }
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (viewModel.isCameraContainerSmall() && getActivity() != null) {
                getActivity().onBackPressed();
            } else {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    viewModel.onSingleTapGesture(focusIndicator, e.getX(), e.getY());
                }
            }
            return super.onSingleTapConfirmed(e);
        }
    }
}
