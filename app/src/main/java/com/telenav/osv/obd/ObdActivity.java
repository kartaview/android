package com.telenav.osv.obd;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.telenav.osv.R;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.model.base.KVBaseActivity;
import com.telenav.osv.common.model.base.KVBaseFragment;
import com.telenav.osv.map.MapFragment;
import com.telenav.osv.map.model.MapModes;
import com.telenav.osv.obd.connect.ConnectToObdFragment;
import com.telenav.osv.obd.connected.ObdConnectedFragment;
import com.telenav.osv.obd.pair.ble.devices.ObdBleDevicesFragment;
import com.telenav.osv.ui.fragment.HintsFragment;
import com.telenav.osv.ui.fragment.LocalSequenceFragment;
import com.telenav.osv.ui.fragment.UploadFragment;
import com.telenav.osv.ui.fragment.camera.PreviewAreaFragment;
import com.telenav.osv.ui.fragment.camera.controls.CameraControlsFragment;
import com.telenav.osv.ui.fragment.camera.preview.CameraPreviewFragment;
import com.telenav.osv.ui.fragment.settings.SettingsFragment;
import com.telenav.osv.ui.fragment.settings.viewmodel.SettingsViewModel;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.utils.ActivityUtils;
import com.telenav.osv.utils.ExtensionsKt;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Activity which is a container for all obd related logic and ui.
 */
public class ObdActivity extends KVBaseActivity {

    private static final String TAG = ObdActivity.class.getSimpleName();

    /**
     * The key of the session used to determine which screen should be displayed first.
     */
    public static final String KEY_SESSION = "KEY_SESSION";

    /**
     * The value for the recording session.
     * Using this value the activity will display the camera screen with the recording flow.
     */
    public static final int SESSION_RECORDING = 0;

    /**
     * The value for the settings session.
     * Using this value the activity will display the settings menu.
     */
    public static final int SESSION_SETTINGS = 1;

    /**
     * The value for the upload session.
     * Using this value will display the upload screen or the sequence screen based on upload status.
     */
    public static final int SESSION_UPLOAD = 2;

    /**
     * The value for the sequence session preview.
     * Using this value will display the sequence screen for the sequence and display the information related to it.
     */
    public static final int SESSION_SEQUENCE_PREVIEW = 3;

    /**
     * Instance to the presenter which handles the business logic.
     */
    private ObdContract.ObdPresenter presenter;

    /**
     * The current session for this activity.
     */
    private int currentSession = -1;

    /**
     * Method used to create a new intent for the current activity with the given parameters.
     * @param context the activity context.
     * @param sessionType the session to start for the current activity.
     * @return an {@code Intent} used for starting teh current activity.
     */
    public static Intent newIntent(Context context, @SessionIdentifier int sessionType) {
        Intent intent = new Intent(context, ObdActivity.class);
        intent.putExtra(KEY_SESSION, sessionType);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        presenter = new ObdPresenterImpl();
        Intent intent = getIntent();
        if (savedInstanceState == null && intent != null) {
            onNewIntent(intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int newSession = intent.getIntExtra(KEY_SESSION, SESSION_RECORDING);
        Log.d(TAG, "onNewIntent. Current session: " + currentSession + " .new session: " + newSession + ".");
        if (currentSession == newSession) {
            return;
        }
        currentSession = newSession;
        switch (currentSession) {
            case SESSION_UPLOAD:
                ActivityUtils.clearBackStack(getSupportFragmentManager());
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                openUploadScreen();
                break;
            case SESSION_SETTINGS:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                openSettingsScreen();
                break;
            case SESSION_SEQUENCE_PREVIEW:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            case SESSION_RECORDING:
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                openRecordingScreen();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.exit_from_up, R.anim.exit_to_down);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int i = 0;
        switch (requestCode) {
            case KVApplication.START_RECORDING_PERMISSION:
                for (i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    i++;
                }
                presenter.notifyRecordingPermissionListeners();
                break;
            case KVApplication.CAMERA_PERMISSION:
                for (String perm : permissions) {
                    if (perm.equals(Manifest.permission.CAMERA)) {
                        if (grantResults.length > i && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            presenter.notifyCameraPermissionListeners();
                        } else {
                            finish();
                            return;
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Utils.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                return;
            } else if (resultCode == Activity.RESULT_OK) {
                ActivityUtils.replaceFragment(getSupportFragmentManager(),
                        ObdBleDevicesFragment.newInstance(),
                        R.id.layout_activity_obd_fragment_container,
                        true,
                        ObdBleDevicesFragment.TAG);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_obd;
    }

    /**
     * Opens the camera preview and the camera controls.
     */
    public void openRecordingScreen() {
        Log.d(TAG, "openRecordingScreen");
        CameraPreviewFragment cameraPreviewFragment = CameraPreviewFragment.newInstance();
        CameraControlsFragment cameraControlsFragment = CameraControlsFragment.newInstance();
        presenter.addPermissionListener(cameraPreviewFragment);
        presenter.addPermissionListener(cameraControlsFragment);
        ActivityUtils.addFragmentToContainer(getSupportFragmentManager(),
                cameraPreviewFragment,
                R.id.layout_activity_obd_fragment_camera_preview_container,
                true,
                CameraPreviewFragment.TAG);
        ActivityUtils.addFragmentToContainer(getSupportFragmentManager(),
                cameraControlsFragment,
                R.id.layout_activity_obd_fragment_container,
                true,
                CameraControlsFragment.TAG);
        MapModes mapMode =
                ExtensionsKt.enableMapForRecording(Injection.provideApplicationPreferences(getApplicationContext())) ?
                        MapModes.RECORDING :
                        MapModes.RECORDING_MAP_DISABLED;
        if (mapMode.getMode() == MapModes.RECORDING.getMode()) {
            Log.d(TAG, "openRecordingScreen. Status: adding map preview.");
            MapFragment mapFragment = MapFragment.newInstance(mapMode, null);
            ActivityUtils.addFragmentToContainer(getSupportFragmentManager(),
                    mapFragment,
                    R.id.layout_activity_obd_fragment_camera_preview_map_container,
                    true,
                    MapFragment.TAG);
            mapFragment.switchMapMode(mapMode, null);
            ActivityUtils.addFragmentToContainer(getSupportFragmentManager(),
                    PreviewAreaFragment.newInstance(),
                    R.id.frame_layout_activity_obd_click_area,
                    true,
                    PreviewAreaFragment.TAG);
        }
    }

    /**
     * Opens the OBD FTUE screen.
     * @param isObdConnected the connection state of the OBD.
     * @param obdDetailsBundle the OBD details to display.
     */
    public void openObdFtueScreen(boolean isObdConnected, Bundle obdDetailsBundle) {
        if (isObdConnected) {
            ActivityUtils.replaceFragment(getSupportFragmentManager(),
                    ObdConnectedFragment.newInstance(obdDetailsBundle),
                    R.id.layout_activity_obd_fragment_container,
                    true,
                    ObdConnectedFragment.TAG);
        } else {
            ActivityUtils.replaceFragment(getSupportFragmentManager(),
                    ConnectToObdFragment.newInstance(),
                    R.id.layout_activity_obd_fragment_container,
                    true,
                    ConnectToObdFragment.TAG);
        }
        View view = findViewById(R.id.layout_activity_obd_fragment_container);
        view.bringToFront();
    }

    /**
     * Opens the hints screen.
     */
    public void openHintsFragment() {
        ActivityUtils.replaceFragment(getSupportFragmentManager(),
                HintsFragment.newInstance(),
                R.id.layout_activity_obd_fragment_container,
                true,
                HintsFragment.TAG);
        View view = findViewById(R.id.layout_activity_obd_fragment_container);
        view.bringToFront();
    }

    @Override
    public void resolveLocationProblem() {
        //ToDO: move logic from main to obd
    }

    /**
     * Opens the local sequence screen or upload screen based in the upload is currently in progress or not.
     */
    private void openUploadScreen() {
        UploadManager uploadManager = Injection.provideUploadManager();
        KVBaseFragment kvBaseFragment;
        if (uploadManager.isInProgress()) {
            kvBaseFragment = UploadFragment.newInstance();
        } else {
            kvBaseFragment = LocalSequenceFragment.newInstance();
        }
        ActivityUtils.addFragmentToContainer(getSupportFragmentManager(),
                kvBaseFragment,
                R.id.layout_activity_obd_fragment_container,
                false,
                kvBaseFragment.getTag());
    }

    /**
     * Opens the settings screen.
     */
    private void openSettingsScreen() {
        SettingsFragment settingsFragment = SettingsFragment.newInstance(SettingsViewModel.class);
        presenter.addPermissionListener(settingsFragment);
        ActivityUtils.addFragmentToContainer(getSupportFragmentManager(),
                settingsFragment,
                R.id.layout_activity_obd_fragment_container,
                false,
                SettingsFragment.TAG);
    }

    /**
     * Interface used in the intent in order to denote which screen should be open at first.
     */
    @IntDef(value = {SESSION_RECORDING, SESSION_UPLOAD, SESSION_SETTINGS, SESSION_SEQUENCE_PREVIEW})
    @Retention(RetentionPolicy.SOURCE)
    @interface SessionIdentifier {}
}
