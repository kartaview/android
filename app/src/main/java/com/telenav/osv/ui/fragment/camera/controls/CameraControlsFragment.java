package com.telenav.osv.ui.fragment.camera.controls;

import java.util.Objects;
import org.joda.time.LocalDateTime;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.snackbar.Snackbar;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrPosition;
import com.telenav.osv.R;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.common.model.base.OSCBaseFragment;
import com.telenav.osv.common.toolbar.OSCToolbar;
import com.telenav.osv.common.toolbar.ToolbarSettings;
import com.telenav.osv.common.tooltip.OscTooltip;
import com.telenav.osv.common.ui.loader.LoadingScreen;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.ObdActivity;
import com.telenav.osv.obd.ObdContract;
import com.telenav.osv.ui.fragment.camera.RecordingStoppedDialogFragment;
import com.telenav.osv.ui.fragment.camera.controls.event.RecordingDetails;
import com.telenav.osv.ui.fragment.camera.controls.presenter.CameraObdContract;
import com.telenav.osv.ui.fragment.camera.controls.presenter.CameraObdContract.CameraObdViewModel;
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.CameraObdViewModelImpl;
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.RecordingViewModel;
import com.telenav.osv.ui.fragment.camera.preview.CameraPreviewFragment;
import com.telenav.osv.ui.fragment.camera.tagging.TaggingFragment;
import com.telenav.osv.utils.ActivityUtils;
import com.telenav.osv.utils.AnimationUtils;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.PermissionCode;
import com.telenav.osv.utils.UiUtils;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

/**
 * Fragment which handles the recording interaction.
 * Use the {@link CameraControlsFragment#newInstance()} factory method to create an instance of this fragment.
 */
public class CameraControlsFragment extends OSCBaseFragment implements ObdContract.PermissionsListener {

    public static final String TAG = CameraControlsFragment.class.getSimpleName();

    /**
     * Padding value in dp used for pay rate view.
     */
    private static final int BYOD_PAY_RATE_PADDING = 13;

    /**
     * Padding value in dp for multiplier view.
     */
    private static final int GAMIFICATION_MULTIPLIER_PADDING = 8;

    /**
     * Format representing the converted minutes format "00:59:59" to milliseconds before switching to hours format.
     */
    public static long FORMAT_MINUTES_FINISHED = 3599000;

    /**
     * Format representing the converted hours format "9:59:59" to milliseconds in order to switch HH:MM:SS format.
     */
    public static long FORMAT_HOUR_FINISHED = 35999000;

    /**
     * The format used for converting the MM:SS format to HH:MM:SS.
     */
    public static String FORMAT_FROM_MM_SS_TO_HH_MM_SS = "00:%s";

    /**
     * The format used for converting the H:MM:SS time format to HH:MM:SS.
     */
    public static String FORMAT_FROM_H_MM_SS_TO_HH_MM_SS = "0%s";

    /**
     * The format used for having the time format as HH:MM:SS. This format doesn't need a conversion.
     */
    public static String FORMAT_FROM_HH_MM_SS_TO_HH_MM_SS = "%s";

    /**
     * Default init speed value for OBD.
     */
    private static String DEFAULT_OBD_SPEED = "0";

    /**
     * The velocity of the swipe down gesture.
     * This should be big enough in order to dismiss the camera screen only when is intended and not by a simple, small gesture.
     */
    private static int SWIPE_DOWN_VELOCITY = 5000;

    /**
     * The percentage of the camera preview from which will be considered that should be dismiss.
     * If the user swipe the preview half way(0.5) the screen will not be dismissed.
     */
    private static float SWIPE_DOWN_DISTANCE_IN_PERCENTAGE = 0.75f;

    /**
     * The {@code TextView} representing the images value.
     */
    private TextView textViewImagesValue;

    /**
     * The {@code TextView} representing the images label.
     */
    private TextView textViewImagesLabel;

    /**
     * The {@code TextView} representing the track size value.
     */
    private TextView textViewSizeValue;

    /**
     * The {@code TextView} representing the size's unit label.
     */
    private TextView textViewSizeLabel;

    /**
     * The {@code TextView} representing the distance value.
     */
    private TextView textViewDistanceValue;

    /**
     * The {@code TextView} representing the distance's unit label.
     */
    private TextView textViewDistanceLabel;

    /**
     * The {@code TextView} representing the points or the pay rate estimation label.
     */
    private TextView textViewPointsValue;

    /**
     * The {@code TextView} representing the multiplier value or the pay rate value.
     */
    private TextView textViewPointsMultiplier;

    /**
     * The holder of the recording details.
     */
    private View layoutRecordingDetails;

    /**
     * The holder of the points details.
     */
    private View layoutRecordingPoints;

    /**
     * The {@code Button} to start the recording.
     */
    private Button startRecordingButton;

    /**
     * The {@code Button} to start the tagging.
     */
    private Button startTaggingButton;

    /**
     * The {@code ImageView} to close the camera preview.
     */
    private ImageView closeCameraButton;

    /**
     * The {@code TextView} to open the recording hints screen.
     */
    private TextView tipsAndTricksButton;

    /**
     * The {@code ImageView} representing the OBD connection state.
     */
    private ImageView imageViewObdConnection;

    /**
     * The {@code TextView} representing the OBD speed value, visible only when the OBD is connected.
     */
    private TextView textViewObdSpeedValue;

    /**
     * The {@code TextView} representing the Obd speed in km or miles depending on the user preference.
     */
    private TextView textViewObdSpeedUnit;

    /**
     * The {@code Chronometer} which is used to display the recording time when a recording is started.
     */
    private Chronometer chronometer;

    /**
     * The tooltip for the OBD icon.
     * The OBD has four states:
     * <ul>
     * <li>
     * NOT_YET_CONNECTED, CONNECTED:
     * The tooltip should be dismissed in one of the following cases:
     * <ul>
     * <li>when is tapped</li>
     * <li>after 10 seconds since entering the recording screen</li>
     * <li>when recording starts</li>
     * </ul>
     * </li>
     * <li>
     * CONNECTING,DISCONNECTED
     * The tooltip should be dismissed only when is tapped by the user.
     * </li>
     * </ul>
     */
    private OscTooltip obdTooltip;

    /**
     * The layout holder for the current fragment.
     */
    private View fragmentLayout;

    /**
     * The {@code ViewModel} which handles the logic for recording details and recording operations.
     */
    private RecordingViewModel recordingViewModel;

    /**
     * The {@code ViewModel} which handles the logic for OBD state and OBD speed.
     */
    private CameraObdViewModel obdViewModel;

    /**
     * The {@code ImageView} representing the state of the location accuracy.
     */
    private ImageView imageViewLocationConnection;

    /**
     * Instance of {@code SlidrInterface} which is used for controlling the sliding from top to bottom
     * in order to close the camera screen. The swipe gesture should be available only in the preview mode
     * and should be locked in the recording screen.
     */
    private SlidrInterface slider;

    private ProgressBar progressBar;

    private RecordingStoppedDialogFragment dialogFragment;

    /**
     * Use this factory method to create a new instance of this fragment using the provided parameters.
     * @return a new instance of the {@link CameraControlsFragment}.
     */
    public static CameraControlsFragment newInstance() {
        return new CameraControlsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        this.recordingViewModel = ViewModelProviders.of(this).get(RecordingViewModel.class);
        this.obdViewModel = ViewModelProviders.of(this).get(CameraObdViewModelImpl.class);
        obdViewModel.startObdConnection();
        observeOnObdEvent();
        observeOnObdTooltipEvent();
        observeOnRecordingEvent();
        observeOnRecordingDetailsEvent();
        observeOnSnackBarDisplay();
        observeOnRecordingPermissionsEvent();
        observeOnLocationEvent();
        observeOnRecordingErrors();
        observeOnObdDetailsErrors();
        observeOnObdDetailsButtonTap();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        fragmentLayout = inflater.inflate(R.layout.fragment_camera_controls, container, false);
        fragmentLayout.bringToFront();
        recordingViewModel.setTaggingMode(false);
        progressBar = fragmentLayout.findViewById(R.id.progress_bar);
        initRecordingDetailsLayout();
        initObdLayout();
        initCloseCamera();
        initTipsAndTricks();
        initRecordingOperations();
        initTaggingOperation();
        imageViewLocationConnection = fragmentLayout.findViewById(R.id.image_view_fragment_camera_controls_gps_icon);
        initSlider();
        if (recordingViewModel.isRecording()) {
            showRecordingDetailsLayout();
            setRecordingDetails();
        }
        if (savedInstanceState != null) {
            recordingViewModel.restoreInstanceState(savedInstanceState);
        }
        return fragmentLayout;
    }

    @Nullable
    @Override
    public LoadingScreen setupLoadingScreen() {
        return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        recordingViewModel.saveInstanceState(outState);
    }

    @Override
    public ToolbarSettings getToolbarSettings(OSCToolbar oscToolbar) {
        return null;
    }

    @Override
    public boolean handleBackPressed() {
        if (!recordingViewModel.isRecording() && getActivity() != null) {
            OSVApplication application = (OSVApplication) getActivity().getApplication();
            application.releaseRecording();
            application.releaseScore();
            getActivity().getSupportFragmentManager().popBackStack(CameraPreviewFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            getActivity().finish();
        } else {
            showRecordingDialog();
        }
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        setCameraWindowSettings();
        recordingViewModel.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recordingViewModel.isRecording()) {
            lockSwipeGesture();
        } else {
            unlockSwipeGesture();
        }
        obdViewModel.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        obdViewModel.onPause();
        lockSwipeGesture();
    }

    @Override
    public void onStop() {
        super.onStop();
        recordingViewModel.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        restoreDefaultWindowSettings();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        obdViewModel.stopObdConnection();
    }

    @Override
    public void onPermissionGranted(int permissionCode) {
        if (permissionCode == PermissionCode.PERMISSION_RECORD) {
            recordingViewModel.startRecording();
        }
    }

    private void resolveLocationProblem() {
        UiUtils.showSnackBar(getContext(), fragmentLayout, getString(R.string.record_request_location), Snackbar.LENGTH_LONG, getString(R.string.enable_label),
                () -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                });
    }

    /**
     * Sets the values to the recording details when navigating back to the screen when recording is started.
     */
    private void setRecordingDetails() {
        RecordingDetails recordingDetails = recordingViewModel.getCapturedImagesObservable().getValue();
        if (recordingDetails != null) {
            textViewImagesValue.setText(recordingDetails.getValue());
            textViewImagesLabel.setText(recordingDetails.getLabel());
        }
        recordingDetails = recordingViewModel.getSizeObservable().getValue();
        if (recordingDetails != null) {
            textViewSizeValue.setText(recordingDetails.getValue());
            textViewSizeLabel.setText(recordingDetails.getLabel());
        }
        recordingDetails = recordingViewModel.getDistanceObservable().getValue();
        if (recordingDetails != null) {
            textViewDistanceValue.setText(recordingDetails.getValue());
            textViewDistanceLabel.setText(recordingDetails.getLabel());
        }

    }

    /**
     * Registers to observe on recording error in order to display a dialog.
     */
    private void observeOnRecordingErrors() {
        recordingViewModel.getRecordingErrorObservable().observe(this, wasError -> {
            if (wasError != null && wasError &&
                    getFragmentManager() != null && getFragmentManager().findFragmentByTag(RecordingStoppedDialogFragment.TAG) == null) {
                dialogFragment = new RecordingStoppedDialogFragment();
                dialogFragment.show(getFragmentManager(), RecordingStoppedDialogFragment.TAG);
            }
        });
    }

    /**
     * Registers to observe on obd details errors in order to display either
     */
    private void observeOnObdDetailsErrors() {
        obdViewModel.getObdErrorObservable().observe(this, errorId -> {
            Log.d(TAG, String.format("observeOnObdDetailsErrors. Status: error. Message: Error id passed: %s.", errorId));
            @StringRes int messageRes;
            switch (errorId) {
                case CameraObdContract.CameraObdErrors.RECORDING_SEQUENCE_NOT_FOUND:
                    messageRes = R.string.recording_sequence_not_found;
                    break;
                default:
                    messageRes = R.string.something_wrong_try_again;
                    break;
            }
            Toast.makeText(this.getContext(), messageRes, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Registers to observe on obd details button tap.
     */
    private void observeOnObdDetailsButtonTap() {
        obdViewModel.getObdDetailsObservable().observe(this, bundle -> {
            if (bundle == null) {
                return;
            }
            restoreDefaultWindowSettings();
            obdViewModel.stopObdWhenIsAttemptingToConnect();
            ((ObdActivity) getActivity()).openObdFtueScreen(obdViewModel.isObdConnected(), bundle);
        });
    }

    /**
     * Initializes the {@code Slidr} for the swipe down gesture.
     */
    private void initSlider() {
        SlidrConfig slidingConfig = new SlidrConfig.Builder()
                .position(SlidrPosition.TOP)
                .distanceThreshold(SWIPE_DOWN_DISTANCE_IN_PERCENTAGE)
                .velocityThreshold(SWIPE_DOWN_VELOCITY)
                .build();
        Activity activity = getActivity();
        if (activity != null) {
            slider = Slidr.attach(activity, slidingConfig);
        }
    }

    /**
     * Sets the window to full screen with a transparent status bar.
     */
    private void setCameraWindowSettings() {
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //make the view full screen with status bar
            getActivity().getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    /**
     * Restores the window to fit the screen.
     */
    private void restoreDefaultWindowSettings() {
        Activity activity = getActivity();
        if (activity != null) {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            activity.getWindow().setStatusBarColor(getResources().getColor(R.color.md_grey_200));
            getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /**
     * Initializes tagging operation which will modify the current screen to have also tagging into it.
     */
    private void initTaggingOperation() {
        if (recordingViewModel.isTaggingEnabled()) {
            startTaggingButton = fragmentLayout.findViewById(R.id.button_fragment_camera_controls_tagging);
            startTaggingButton.setOnClickListener(v -> {
                restoreDefaultWindowSettings();
                AnimationUtils.resizeCameraPreview(true, getActivity(), fragmentLayout);
                ActivityUtils.replaceFragment(getActivity().getSupportFragmentManager(),
                        TaggingFragment.newInstance(createTaggingBundle()), R.id.layout_activity_obd_fragment_container,
                        true, TaggingFragment.TAG);

            });
        }
    }

    private Bundle createTaggingBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ARG_DISTANCE, textViewDistanceValue.getText().toString());
        bundle.putString(Constants.ARG_PICS, textViewImagesValue.getText().toString());
        bundle.putString(Constants.ARG_SIZE, textViewSizeValue.getText().toString());
        bundle.putString(Constants.ARG_DISTANCE_LABEL, textViewDistanceLabel.getText().toString());
        bundle.putString(Constants.ARG_SIZE_LABEL, textViewSizeLabel.getText().toString());
        if (recordingViewModel.getLocationAccuracyObservable() != null) {
            bundle.putInt(Constants.ARG_GPS_ICON, recordingViewModel.getLocationAccuracyObservable().getValue() != null ?
                    recordingViewModel.getLocationAccuracyObservable().getValue() : R.drawable.ic_gps_on_big);
        }
        return bundle;
    }

    /**
     * Initializes the view for the start and stop recording operations.
     */
    private void initRecordingOperations() {
        startRecordingButton = fragmentLayout.findViewById(R.id.button_fragment_camera_controls_start_recording);
        startRecordingButton.setOnClickListener(v -> {
            Context context = getContext();
            if (context == null) {
                return;
            }
            if (Utils.isGPSEnabled(context)) {
                recordingViewModel.startRecording();
                lockSwipeGesture();
            } else {
                resolveLocationProblem();
            }
        });
        chronometer = fragmentLayout.findViewById(R.id.chronometer_fragment_camera_controls_stop_recording);
        chronometer.setOnClickListener(v -> {
            chronometer.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            recordingViewModel.stopRecording();
        });
    }

    /**
     * Initializes the tips and tricks for displaying a camera guide.
     */
    private void initTipsAndTricks() {
        tipsAndTricksButton = fragmentLayout.findViewById(R.id.text_view_fragment_camera_controls_tips_and_tricks);
        tipsAndTricksButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((ObdActivity) getActivity()).openHintsFragment();
            }
        });
    }

    /**
     * Initializes the close button in order to exit the camera preview screen.
     * The button should be visible only when the recording is stopped.
     */
    private void initCloseCamera() {
        closeCameraButton = fragmentLayout.findViewById(R.id.image_view_fragment_camera_controls_close_camera);
        closeCameraButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    /**
     * Initializes the views to display the OBD state and speed.
     */
    private void initObdLayout() {
        imageViewObdConnection = fragmentLayout.findViewById(R.id.image_view_fragment_camera_controls_obd);
        textViewObdSpeedValue = fragmentLayout.findViewById(R.id.text_view_fragment_camera_controls_obd_speed);
        textViewObdSpeedUnit = fragmentLayout.findViewById(R.id.text_view_fragment_camera_controls_obd_speed_unit);
        View obdClickLayout = fragmentLayout.findViewById(R.id.view_fragment_camera_controls_obd_click_view);
        obdClickLayout.setOnClickListener(v -> {
            //open the screen for connecting to OBD, only when the user is not in recording mode
            //open OBD connected screen if the user is in recording mode and the obd state is connected
            if ((!recordingViewModel.isRecording() || (recordingViewModel.isRecording() && obdViewModel.isObdConnected()))
                    && getActivity() != null) {
                obdViewModel.startObdDetailsAcquire();
            }
        });
    }

    /**
     * Registers an observer to listen for a recording permission event.
     * The event received contains the required permission which must be requested in order to start a recording.
     */
    private void observeOnRecordingPermissionsEvent() {
        recordingViewModel.getRecordingPermissionObservable().observe(this, permissions -> {
            if (getActivity() != null && permissions != null) {
                ActivityCompat.requestPermissions(getActivity(), permissions, OSVApplication.START_RECORDING_PERMISSION);
            }
        });
    }

    /**
     * Registers an observer to listen for recording events.
     * The event received contains a flag which is {@code true} if the recording was started, {@code false} otherwise.
     */
    private void observeOnRecordingEvent() {
        recordingViewModel.getRecordingObservable().observe(this, isRecording -> {
            if (isRecording == null) {
                return;
            }
            if (isRecording) {
                showRecordingDetailsLayout();
            } else {
                progressBar.setVisibility(View.GONE);
                hideRecordingDetailsLayout();
            }
        });
    }

    /**
     * Registers an observer which should display a snack bar with the received details.
     */
    private void observeOnSnackBarDisplay() {
        recordingViewModel.getSnackBarObservable().observe(this, snackBarItem -> {
            if (snackBarItem != null) {
                UiUtils.showSnackBar(getContext(), fragmentLayout, snackBarItem);
            }
        });

    }

    /**
     * Registers multiple observers to listen for the updates of a track captured images, size and distance.
     */
    private void observeOnRecordingDetailsEvent() {
        recordingViewModel.getCapturedImagesObservable().observe(this, image -> {
            if (image != null) {
                textViewImagesValue.setText(image.getValue());
                textViewImagesLabel.setText(image.getLabel());
            }
        });
        recordingViewModel.getSizeObservable().observe(this, size -> {
            if (size != null) {
                textViewSizeValue.setText(size.getValue());
                textViewSizeLabel.setText(size.getLabel());
            }
        });
        recordingViewModel.getDistanceObservable().observe(this, distance -> {
            if (distance != null) {
                textViewDistanceValue.setText(distance.getValue());
                textViewDistanceLabel.setText(distance.getLabel());
            }
        });
        recordingViewModel.getScorePointsObservable().observe(this, score -> {
            if (score != null && recordingViewModel.isRecording()) {
                setScoreLayoutVisible();
                if (recordingViewModel.isByodDriver()) {
                    textViewPointsValue.setText(score);
                } else {
                    textViewPointsValue.setText(String.format("%s %s", score, getString(R.string.points)));
                }
            }
        });
        recordingViewModel.getScoreMultiplierObservable().observe(this, multiplier -> {
            if (multiplier != null && recordingViewModel.isRecording()) {
                setScoreLayoutVisible();
                textViewPointsMultiplier.setText(multiplier);
            }

        });
    }

    /**
     * Set the score layout to VISIBLE if the previous visibility is GONE.
     */
    private void setScoreLayoutVisible() {
        if (layoutRecordingPoints.getVisibility() == View.GONE) {
            layoutRecordingPoints.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Displays the recording dialog when the application is sent to background.
     */
    private void showRecordingDialog() {
        if (getContext() == null || getActivity() == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialog);
        builder.setMessage(R.string.stop_recording_warning_message).setTitle(R.string.stop_recording_warning_title)
                .setNegativeButton(R.string.stop_value_string, (dialog, which) -> {
                    recordingViewModel.stopRecording();
                    getActivity().onBackPressed();
                })
                .setPositiveButton(R.string.keep_value_string, (dialog, which) -> {
                    Toast.makeText(getContext(), R.string.recording_in_background_toast_message, Toast.LENGTH_SHORT).show();
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(homeIntent);
                }).create().show();
        setCameraWindowSettings();
    }

    /**
     * Registers an observer for the OBD tooltip event, in order to display it or to dismiss it.
     */
    private void observeOnObdTooltipEvent() {
        obdViewModel.getObdTooltipObservable().observe(this, tooltipStyle -> {
            if (tooltipStyle == null) {
                dismissTooltip(true);
                return;
            }
            if (obdTooltip != null) {
                boolean animateTooltip = obdTooltip.isShowing();
                obdTooltip.invalidateTooltip(new OscTooltip.Builder(imageViewObdConnection, (ViewGroup) fragmentLayout, tooltipStyle.getTooltipStyle())
                        .setMessage(tooltipStyle.getTooltipMessage())
                        .setGravity(Gravity.BOTTOM | Gravity.START));
                obdTooltip.show(!animateTooltip);
            } else {
                obdTooltip = new OscTooltip.Builder(imageViewObdConnection, (ViewGroup) fragmentLayout, tooltipStyle.getTooltipStyle())
                        .setMessage(tooltipStyle.getTooltipMessage())
                        .setGravity(Gravity.BOTTOM | Gravity.START)
                        .show(true);
            }
        });
    }

    /**
     * Registers an observer for the OBD state and OBD speed.
     */
    private void observeOnObdEvent() {
        obdViewModel.getObdStateObservable().observe(this, this::updateObdIcon);
        obdViewModel.getObdSpeedObservable().observe(this, speed -> {
            if (speed != null) {
                textViewObdSpeedValue.setText(speed[0]);
                textViewObdSpeedUnit.setText(speed[1]);
            } else {
                textViewObdSpeedValue.setText("");
            }
        });
    }

    /**
     * Registers an observer for the location accuracy type.
     */
    private void observeOnLocationEvent() {
        recordingViewModel.getLocationAccuracyObservable().observe(this, imageResource -> {
            if (imageResource == null) {
                imageViewLocationConnection.setVisibility(View.GONE);
            } else {
                imageViewLocationConnection.setVisibility(View.VISIBLE);
                imageViewLocationConnection.setImageDrawable(getResources().getDrawable(imageResource));
            }
        });
    }

    /**
     * Hides the recording details when the recording is stopped.
     */
    private void hideRecordingDetailsLayout() {
        unlockSwipeGesture();
        chronometer.setVisibility(View.GONE);
        chronometer.setOnChronometerTickListener(null);
        chronometer.stop();
        layoutRecordingDetails.setVisibility(View.GONE);
        layoutRecordingPoints.setVisibility(View.GONE);
        if (startTaggingButton != null) {
            startTaggingButton.setVisibility(View.GONE);
        }
        startRecordingButton.setVisibility(View.VISIBLE);
        tipsAndTricksButton.setVisibility(View.VISIBLE);
        closeCameraButton.setVisibility(View.VISIBLE);
    }

    /**
     * Displays the recording details when the recording is started an initializes the values.
     */
    private void showRecordingDetailsLayout() {
        startRecordingButton.setVisibility(View.GONE);
        tipsAndTricksButton.setVisibility(View.GONE);
        closeCameraButton.setVisibility(View.GONE);
        if (startTaggingButton != null) {
            startTaggingButton.setVisibility(View.VISIBLE);
        }
        chronometer.setVisibility(View.VISIBLE);
        layoutRecordingDetails.setVisibility(View.VISIBLE);
        if (getActivity() != null) {
            long baseTime = recordingViewModel.getRecordingStartTime() -
                    LocalDateTime.now().toDateTime().getMillis();
            Log.d(TAG, "startRecording baseTime - " + baseTime);
            chronometer.setBase(SystemClock.elapsedRealtime() - Math.abs(baseTime));
        }
        chronometer.setOnChronometerTickListener(chronometer -> {
            long milliseconds = SystemClock.elapsedRealtime() - chronometer.getBase();
            if (milliseconds >= FORMAT_HOUR_FINISHED && !chronometer.getFormat().equals(FORMAT_FROM_HH_MM_SS_TO_HH_MM_SS)) {
                chronometer.setFormat(FORMAT_FROM_HH_MM_SS_TO_HH_MM_SS);
            } else if (milliseconds >= FORMAT_MINUTES_FINISHED && milliseconds < FORMAT_HOUR_FINISHED
                    && !chronometer.getFormat().equals(FORMAT_FROM_H_MM_SS_TO_HH_MM_SS)) {
                chronometer.setFormat(FORMAT_FROM_H_MM_SS_TO_HH_MM_SS);
            }
            chronometer.setText(FormatUtils.formatChronometerText((String) chronometer.getText()));
        });
        chronometer.setFormat(FORMAT_FROM_MM_SS_TO_HH_MM_SS);
        chronometer.start();
    }

    /**
     * Initializes the recording details layout, which will be hidden when entering the screen until the recording is started.
     */
    private void initRecordingDetailsLayout() {
        layoutRecordingDetails = fragmentLayout.findViewById(R.id.layout_fragment_camera_controls_recording_details);
        View layoutImagesDetails = layoutRecordingDetails.findViewById(R.id.layout_fragment_camera_controls_images);
        View layoutDistanceDetails = layoutRecordingDetails.findViewById(R.id.layout_fragment_camera_controls_distance);
        View layoutSizeDetails = layoutRecordingDetails.findViewById(R.id.layout_fragment_camera_controls_size);
        layoutRecordingPoints = fragmentLayout.findViewById(R.id.layout_fragment_camera_controls_recording_points);
        textViewImagesValue = layoutImagesDetails.findViewById(R.id.text_view_recording_details_value);
        textViewImagesLabel = layoutImagesDetails.findViewById(R.id.text_view_recording_details_label);
        textViewSizeValue = layoutSizeDetails.findViewById(R.id.text_view_recording_details_value);
        textViewSizeLabel = layoutSizeDetails.findViewById(R.id.text_view_recording_details_label);
        textViewDistanceValue = layoutDistanceDetails.findViewById(R.id.text_view_recording_details_value);
        textViewDistanceLabel = layoutDistanceDetails.findViewById(R.id.text_view_recording_details_label);
        textViewPointsValue = layoutRecordingPoints.findViewById(R.id.text_view_layout_recording_points);
        textViewPointsMultiplier = layoutRecordingPoints.findViewById(R.id.text_view_layout_recording_points_multiplier);
        if (recordingViewModel.isByodDriver()) {
            int padding = DimenUtils.dpToPx(BYOD_PAY_RATE_PADDING);
            textViewPointsMultiplier.setPadding(padding, 0, padding, 0);
        } else {
            int padding = DimenUtils.dpToPx(GAMIFICATION_MULTIPLIER_PADDING);
            textViewPointsMultiplier.setPadding(padding, 0, padding, 0);
        }
    }

    /**
     * Updates the OBD icon for the current OBD state.
     * @param imageViewResId the id of the OBD image drawable representing the OBD state.
     */
    private void updateObdIcon(Integer imageViewResId) {
        if (imageViewResId == null) {
            imageViewObdConnection.setVisibility(View.GONE);
            textViewObdSpeedValue.setVisibility(View.GONE);
            textViewObdSpeedUnit.setVisibility(View.GONE);
            return;
        } else if (imageViewObdConnection.getVisibility() == View.GONE) {
            imageViewObdConnection.setVisibility(View.VISIBLE);
        }
        if (imageViewObdConnection != null) {
            imageViewObdConnection.setImageDrawable(getResources().getDrawable(imageViewResId));
        }
        if (textViewObdSpeedValue != null) {
            if (imageViewResId != R.drawable.vector_car_obd_speed) {
                textViewObdSpeedValue.setVisibility(View.GONE);
                textViewObdSpeedUnit.setVisibility(View.GONE);
            } else {
                textViewObdSpeedValue.setVisibility(View.VISIBLE);
                textViewObdSpeedUnit.setVisibility(View.VISIBLE);
                textViewObdSpeedValue.setText(DEFAULT_OBD_SPEED);
                textViewObdSpeedUnit.setText(FormatUtils.formatSpeedFromKmph(Objects.requireNonNull(getContext()), 0)[1]);
            }
        }
    }

    /**
     * Dismisses the tooltip for the OBD state.
     */
    private void dismissTooltip(boolean dismissWithAnimation) {
        if (obdTooltip != null) {
            obdTooltip.dismiss(dismissWithAnimation);
            obdTooltip = null;
        }
    }

    /**
     * Locks the swipe down gesture.
     */
    private void lockSwipeGesture() {
        if (getActivity() != null) {
            slider.lock();
        }
    }

    /**
     * Unlock the swipe down gesture.
     */
    private void unlockSwipeGesture() {
        if (getActivity() != null) {
            slider.unlock();
        }
    }
}
