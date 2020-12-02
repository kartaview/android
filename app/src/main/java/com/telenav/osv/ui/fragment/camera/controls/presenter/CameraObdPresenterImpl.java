package com.telenav.osv.ui.fragment.camera.controls.presenter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardBase;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.ui.fragment.camera.controls.presenter.CameraObdContract.CameraObdPresenter;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Concrete implementation for {@link CameraObdPresenter}.
 * @author cameliao
 * @see CameraObdPresenter
 */
public class CameraObdPresenterImpl implements CameraObdPresenter, ObdManager.ObdConnectionListener {

    /**
     * The maxim number of run time for display the tooltips.
     */
    private static final int MAX_TIMES_FOR_TIPS_DISPLAY = 3;

    /**
     * The {@code String} representing the TAG of the current class.
     */
    private static final String TAG = CameraObdPresenterImpl.class.getSimpleName();

    /**
     * The default value for an unknown resource.
     */
    private static final int UNKNOWN_VALUE = -1;

    /**
     * Instance to the OBD manager.
     */
    private ObdManager obdManager;

    /**
     * Instance to the recording manager.
     */
    private RecorderManager recorder;

    /**
     * The application preferences instance.
     */
    private ApplicationPreferences appPrefs;

    /**
     * The view instance form {@link CameraObdContract}.
     */
    private CameraObdContract.CameraObdView view;

    /**
     * The UI handler to switch the received events from the background thread to the UI thread.
     */
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * The disposable object used for realising the recording state observer.
     */
    private Disposable recordingStateDisposable;

    /**
     * The disposable object used to get from persistence the sequence which is currently being recorded.
     */
    private Disposable recordingSequenceDisposable;

    /**
     * Local data source for the sequence data type. Used to access any persistence related
     */
    private SequenceLocalDataSource sequenceLocalDataSource;

    /**
     * Constructor for the current class.
     * @param appPrefs the application preferences.
     * @param recorder the instance to the recording manager.
     * @param obdManager the instance to the OBD manager.
     * @param view the instance to the view from {@link CameraObdContract}.
     */
    public CameraObdPresenterImpl(@NonNull ApplicationPreferences appPrefs, @NonNull RecorderManager recorder, @NonNull ObdManager obdManager,
                                  SequenceLocalDataSource sequenceLocalDataSource,
                                  @NonNull CameraObdContract.CameraObdView view) {
        this.appPrefs = appPrefs;
        this.recorder = recorder;
        this.obdManager = obdManager;
        this.sequenceLocalDataSource = sequenceLocalDataSource;
        this.view = view;
        observeOnRecordingState();
        obdManager.addObdConnectionListener(this);
    }

    @Override
    public void stopObdConnection() {
        if (!recorder.isRecording()) {
            obdManager.stopCollecting();
        }
    }

    @Override
    public void startObdConnection() {
        int obdType = appPrefs.getIntPreference(PreferenceTypes.K_OBD_TYPE, ObdManager.ObdTypes.NOT_SET);
        boolean obdSetup = obdManager.setupObd(obdType);
        Log.d(TAG, String.format("Obd setup. Success status: %s. Type: %s", obdSetup, obdType));
        if (obdSetup) {
            obdManager.startCollecting();
        }
        handleObdState();
    }

    @Override
    public void processObdDetails() {
        if (obdManager.isConnected() && recorder.isRecording()) {
            String sequenceId = appPrefs.getStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID);
            if (!sequenceId.equals(StringUtils.EMPTY_STRING)) {
                recordingSequenceDisposable = sequenceLocalDataSource
                        .getSequence(sequenceId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                item -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putInt(Constants.ARG_DISTANCE, (int) item.getDetails().getDistance());
                                    SequenceDetailsRewardBase rewardBase = item.getRewardDetails();
                                    if (rewardBase != null) {
                                        bundle.putInt(Constants.ARG_POINTS, (int) item.getRewardDetails().getValue());
                                    } else {
                                        bundle.putInt(Constants.ARG_POINTS, 0);
                                    }
                                    int frameCount = item.getCompressionDetails().getLocationsCount();
                                    if (frameCount == UNKNOWN_VALUE) {
                                        frameCount = 0;
                                    }
                                    bundle.putInt(Constants.ARG_PICS, frameCount);
                                    view.openObdDetails(bundle);
                                },
                                throwable -> {
                                    Log.d(TAG, String.format("processObdDetails. Status: error. Message: %s", throwable.getLocalizedMessage()));
                                    // the unknown error is passed since this case is extremely unlikely to happen.
                                    view.displayObdErrors(CameraObdContract.CameraObdErrors.UNKOWN_ERROR);
                                },
                                () -> {
                                    Log.d(TAG, "processObdDetails. Status: error. Message: Recording sequence not found in the persistence.");
                                    view.displayObdErrors(CameraObdContract.CameraObdErrors.RECORDING_SEQUENCE_NOT_FOUND);
                                });
            }
        } else {
            view.openObdDetails(new Bundle());
        }
    }

    @Override
    public boolean isObdConnected() {
        return obdManager.isConnected();
    }

    @Override
    public void stopObdWhenIsAttemptingToConnect() {
        if (obdManager.getObdState() != ObdManager.ObdState.OBD_CONNECTED && obdManager.getObdState() != ObdManager.ObdState.OBD_DISCONNECTED) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_MANUAL_STOPPED, true);
            obdManager.stopCollecting();
        }
    }

    @Override
    public boolean isObdManualStopped() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_OBD_MANUAL_STOPPED);
    }

    @Override
    public boolean isInFtueMode() {
        return appPrefs.getIntPreference(PreferenceTypes.K_APP_RUN_TIME_COUNTER) <= MAX_TIMES_FOR_TIPS_DISPLAY;
    }

    @Override
    public boolean isRecording() {
        return recorder.isRecording();
    }

    @Override
    public int getObdState() {
        return obdManager.getObdState();
    }

    @Override
    public void release() {
        recordingStateDisposable.dispose();
        if (recordingSequenceDisposable != null) {
            recordingSequenceDisposable.dispose();
        }
        obdManager.removeObdConnectionListener(this);
    }

    @Override
    public void onSpeedObtained(SpeedData speed) {
        Log.d(TAG, "onObdSpeedObtain");
        uiHandler.post(() -> {
            Log.d(TAG, String.format("onSpeedObtained: %s", speed.getSpeed()));
            if (speed.getSpeed() != -1) {
                String[] speedArray = FormatUtils.fromDistanceFromMetersToUnit(
                        !appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC),
                        speed.getSpeed());
                view.onSpeedObtained(speedArray);
            } else {
                view.onSpeedObtained(null);
            }
        });
    }

    @Override
    public void onObdConnected() {
        Log.d(TAG, "onObdConnected");
        handleObdState();
        appPrefs.saveBooleanPreference(PreferenceTypes.K_OBD_MANUAL_STOPPED, false);
    }

    @Override
    public void onObdDisconnected() {
        Log.d(TAG, "onObdDisconnected");
        handleObdState();
    }

    @Override
    public void onObdConnecting() {
        Log.d(TAG, "onObdConnecting");
        handleObdState();
    }

    @Override
    public void onObdInitialised() {
        Log.d(TAG, "onObdInitialised");
        handleObdState();
    }

    /**
     * Subscribes for receiving the recording state changes.
     */
    private void observeOnRecordingState() {
        recordingStateDisposable = recorder.getRecordingStateObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(isRecordingStarted ->
                        view.onRecordingStatusChanged(isRecordingStarted));
    }

    /**
     * Handles the tooltip for the current OBD state.
     */
    private void handleObdState() {
        uiHandler.post(() -> view.displayObdTooltip(obdManager.getObdState()));
    }
}
