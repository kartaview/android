package com.telenav.osv.ui.fragment.camera.controls.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.ui.fragment.camera.controls.event.TooltipStyleEvent;
import com.telenav.osv.ui.fragment.camera.controls.presenter.CameraObdContract;
import com.telenav.osv.ui.fragment.camera.controls.presenter.CameraObdPresenterImpl;
import com.telenav.osv.utils.Log;

import static com.telenav.osv.obd.manager.ObdManager.ObdState.OBD_CONNECTED;
import static com.telenav.osv.obd.manager.ObdManager.ObdState.OBD_CONNECTING;
import static com.telenav.osv.obd.manager.ObdManager.ObdState.OBD_DISCONNECTED;
import static com.telenav.osv.obd.manager.ObdManager.ObdState.OBD_INITIALISING;
import static com.telenav.osv.obd.manager.ObdManager.ObdState.OBD_NOT_CONNECTED;


/**
 * {@code ViewModel} class that handles the view logic regarding the OBD operations.
 * Implements {@link CameraObdContract.CameraObdView} in order to receive events from the business logic.
 */
public class CameraObdViewModelImpl extends AndroidViewModel implements CameraObdContract.CameraObdView, CameraObdContract.CameraObdViewModel {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    private static final String TAG = CameraObdViewModelImpl.class.getSimpleName();

    /**
     * The time in seconds representing the duration of a tooltip visibility.
     */
    private static final int TOOLTIP_DISPLAY_TIME = 10000;

    /**
     * Default type for OBD tooltip until an event is received.
     */
    private static final int UNDEFINED_OBD_TOOLTIP_TYPE = 100;

    /**
     * Instance to the presenter which handles the business logic.
     */
    private CameraObdContract.CameraObdPresenter presenter;

    /**
     * The {@code Handler} which will dismiss the obd tooltip after 10 seconds.
     */
    private Handler obdTooltipHandler;

    /**
     * Observable instance for sending an event when the OBD state changes.
     * The observable emits the resource id of a drawable which should be display for each state.
     */
    private MutableLiveData<Integer> obdStateObservable;

    /**
     * Observable instance for sending the OBD speed event.
     * The observable emits an array of {@code String} with 2 elements, containing the speed value and the speed unit (e.g km/h).
     */
    private MutableLiveData<String[]> obdSpeedObservable;

    /**
     * Observable instance for sending the OBD details bundle.
     * The observable emits the bundle which contains details of the current recording sequence, i.e. distance, pictures, recording, points if enabled.
     */
    private MutableLiveData<Bundle> obdDetailsObservable;

    /**
     * Observable instance for sending the error values for obd.
     * The observable emits values are represented by the interface {@link CameraObdContract.CameraObdErrors}.
     */
    private MutableLiveData<Integer> obdErrorObservable;

    /**
     * Observable instance for sending the OBD tooltip event.
     * The observable emits a {@link TooltipStyleEvent} which holds the properties used for displaying the tooltip.
     */
    private MutableLiveData<TooltipStyleEvent> obdTooltipObservable;

    /**
     * The tooltip type which is different for each OBD state.
     */
    private int obdTooltipType = UNDEFINED_OBD_TOOLTIP_TYPE;

    /**
     * Flag to determine if the OBD UI is hidden.
     * {@code true} if the OBD UI is hidden, {@code false} otherwise.
     */
    private boolean isObdStateHidden;

    /**
     * Default constructor of the current class
     * @param application instance of the application required by the extended {@code ViewModel}.
     */
    public CameraObdViewModelImpl(@NonNull Application application) {
        super(application);
        Context context = application.getApplicationContext();
        ApplicationPreferences appPrefs = ((KVApplication) application).getAppPrefs();
        presenter = new CameraObdPresenterImpl(
                appPrefs, ((KVApplication) application).getRecorder(),
                Injection.provideObdManager(context, appPrefs),
                Injection.provideSequenceLocalDataSource(context,
                        Injection.provideFrameLocalDataSource(context),
                        Injection.provideScoreLocalDataSource(context),
                        Injection.provideLocationLocalDataSource(context),
                        Injection.provideVideoDataSource(context)),
                this);
    }

    @Override
    public void onObdStateChanged(int obdState) {
        if (obdStateObservable == null) {
            return;
        }
        Integer imageViewResId;
        switch (obdState) {
            case OBD_CONNECTED:
                imageViewResId = R.drawable.vector_car_obd_speed;
                break;
            case OBD_NOT_CONNECTED:
            case OBD_CONNECTING:
            case OBD_INITIALISING:
                imageViewResId = R.drawable.vector_car_obd_add;
                break;
            case OBD_DISCONNECTED:
                if (presenter.isObdManualStopped()) {
                    if (presenter.isRecording()) {
                        imageViewResId = null;
                        isObdStateHidden = true;
                    } else {
                        imageViewResId = R.drawable.vector_car_obd_add;
                    }
                } else {
                    imageViewResId = R.drawable.vector_car_obd_disconnected;
                }
                break;
            default:
                Log.d(TAG, String.format("Handle obd state. State not handled: %s", obdState));
                return;
        }
        obdStateObservable.setValue(imageViewResId);
    }

    @Override
    public void displayObdTooltip(int obdSate) {
        if (obdTooltipObservable == null || obdSate == obdTooltipType) {
            return;
        }
        onObdStateChanged(obdSate);
        obdTooltipType = obdSate;
        boolean isRecording = presenter.isRecording();
        switch (obdSate) {
            case OBD_NOT_CONNECTED:
                if (presenter.isInFtueMode() && !isRecording) {
                    obdTooltipObservable.setValue(new TooltipStyleEvent(getApplication().getString(R.string.recording_info_connect_car_obd), R.style.tooltipStyleDefault));
                    dismissTooltipByHandler();
                } else {
                    obdTooltipObservable.setValue(null);
                }
                break;
            case OBD_CONNECTED:
                if (!isRecording) {
                    obdTooltipObservable.setValue(new TooltipStyleEvent(getApplication().getString(R.string.recording_info_obd_connected), R.style.tooltipStyleConnected));
                    dismissTooltipByHandler();
                } else {
                    obdTooltipObservable.setValue(null);
                }
                break;
            case OBD_DISCONNECTED:
                if (!presenter.isObdManualStopped()) {
                    obdTooltipObservable.setValue(new TooltipStyleEvent(getApplication().getString(R.string.recording_info_obd_disconnected), R.style.tooltipStyleDisconnected));
                } else if (presenter.isInFtueMode() && !isRecording) {
                    obdTooltipType = OBD_NOT_CONNECTED;
                    obdTooltipObservable.setValue(new TooltipStyleEvent(getApplication().getString(R.string.recording_info_connect_car_obd), R.style.tooltipStyleDefault));
                    dismissTooltipByHandler();
                } else {
                    obdTooltipObservable.setValue(null);
                }
                break;
            case OBD_CONNECTING:
            case OBD_INITIALISING:
                obdTooltipObservable.setValue(new TooltipStyleEvent(getApplication().getString(R.string.recording_info_obd_connecting), R.style.tooltipStyleDefault));
                break;
            default:
                Log.d(TAG, String.format("Not supported. State: %s", obdSate));
                break;
        }
    }

    @Override
    public void onSpeedObtained(String[] speed) {
        if (obdSpeedObservable != null) {
            obdSpeedObservable.setValue(speed);
        }
    }

    @Override
    public void hideObdInfo() {
        if (obdStateObservable == null) {
            return;
        }
        obdStateObservable.setValue(null);
        dismissObdTooltip();
        isObdStateHidden = true;
    }

    @Override
    public void onRecordingStatusChanged(boolean isRecording) {
        int obdState = presenter.getObdState();
        if (isRecording) {
            if (obdState != OBD_CONNECTED && obdState != OBD_CONNECTING) {
                hideObdInfo();
            } else if (obdTooltipType == OBD_CONNECTED || obdTooltipType == OBD_NOT_CONNECTED) {
                dismissObdTooltip();
            }
        } else if (isObdStateHidden) {
            isObdStateHidden = false;
            onObdStateChanged(presenter.getObdState());
        }
    }

    @Override
    public void openObdDetails(Bundle bundle) {
        if (obdDetailsObservable == null) {
            return;
        }
        obdDetailsObservable.setValue(bundle);
    }

    @Override
    public void displayObdErrors(int errorId) {
        if (obdErrorObservable == null) {
            Log.d(TAG, "displayObdErrors. Status: return. Message: The obd errors observable not set. No set value will be set.");
            return;
        }
        obdErrorObservable.setValue(errorId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        presenter.release();
    }

    @Override
    public LiveData<Integer> getObdStateObservable() {
        if (obdStateObservable == null) {
            return obdStateObservable = new MutableLiveData<>();
        }
        return obdStateObservable;
    }

    @Override
    public LiveData<String[]> getObdSpeedObservable() {
        if (obdSpeedObservable == null) {
            return obdSpeedObservable = new MutableLiveData<>();
        }
        return obdSpeedObservable;
    }

    @Override
    public LiveData<Integer> getObdErrorObservable() {
        if (obdErrorObservable == null) {
            return obdErrorObservable = new MutableLiveData<>();
        }
        return obdErrorObservable;
    }

    @Override
    public LiveData<TooltipStyleEvent> getObdTooltipObservable() {
        if (obdTooltipObservable == null) {
            return obdTooltipObservable = new MutableLiveData<>();
        }
        return obdTooltipObservable;
    }

    @Override
    public boolean isObdConnected() {
        return presenter.isObdConnected();
    }

    @Override
    public LiveData<Bundle> getObdDetailsObservable() {
        if (obdDetailsObservable == null) {
            obdDetailsObservable = new MutableLiveData<>();
        }
        return obdDetailsObservable;
    }

    @Override
    public void startObdDetailsAcquire() {
        presenter.processObdDetails();
    }

    @Override
    public void startObdConnection() {
        presenter.startObdConnection();
    }

    @Override
    public void stopObdWhenIsAttemptingToConnect() {
        presenter.stopObdWhenIsAttemptingToConnect();
    }

    @Override
    public void stopObdConnection() {
        presenter.stopObdConnection();
    }

    @Override
    public void onResume() {
        obdStateObservable.setValue(obdStateObservable.getValue());
        displayObdTooltip(presenter.getObdState());
    }

    @Override
    public void onPause() {
        if (obdTooltipHandler != null) {
            obdTooltipHandler.removeCallbacksAndMessages(null);
        }
        if (obdDetailsObservable != null) {
            obdDetailsObservable.setValue(null);
        }
    }

    /**
     * Dismisses the OBD tooltip taking into account the animation preference.
     */
    private void dismissObdTooltip() {
        if (obdTooltipObservable != null) {
            obdTooltipObservable.setValue(null);
        }
        if (obdTooltipHandler != null) {
            obdTooltipHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Dismiss the OBD tooltip after 10 seconds.
     */
    private void dismissTooltipByHandler() {
        obdTooltipHandler = new Handler();
        obdTooltipHandler.postDelayed(this::dismissObdTooltip,
                TOOLTIP_DISPLAY_TIME);
    }
}
