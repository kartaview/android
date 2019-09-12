package com.telenav.osv.ui.fragment.camera.controls.presenter;

import android.os.Bundle;
import com.telenav.osv.ui.fragment.camera.controls.event.TooltipStyleEvent;
import androidx.lifecycle.LiveData;

/**
 * The contract between {@link CameraObdView} and {@link CameraObdPresenter}.
 * @author cameliao
 */
public interface CameraObdContract {

    /**
     * The camera obd presenter which holds all the available business functionality required by the view.
     */
    interface CameraObdPresenter {

        /**
         * Starts the start in order to obtain the OBD details - images, size on disk, distance
         */
        void processObdDetails();

        /**
         * @return {@code true} if the OBD is connected, {@code false} otherwise.
         */
        boolean isObdConnected();

        /**
         * Start the OBD connection in order to receive the car speed.
         */
        void startObdConnection();

        /**
         * Stops the OBD connection.
         */
        void stopObdConnection();

        /**
         * Stops the OBD when is attempting to connect.
         * Method used for manual disconnect.
         */
        void stopObdWhenIsAttemptingToConnect();

        /**
         * @return the value for the manual disconnect stored in {@code SharedPreferences}.
         */
        boolean isObdManualStopped();

        /**
         * @return {@code true} if the application is in the FTUE mode( first 3 runs), {@code false} otherwise.
         */
        boolean isInFtueMode();

        /**
         * @return {@code true} if the recording is started, {@code false} otherwise.
         */
        boolean isRecording();

        /**
         * @return the current OBD state defined as one of {@link com.telenav.osv.obd.manager.ObdManager.ObdState}
         */
        int getObdState();

        /**
         * Releases the resources.
         */
        void release();
    }

    /**
     * The camera obd view interface which holds all the available functionality required by the presenter.
     */
    interface CameraObdView {

        /**
         * Handles obd state by setting the tooltip in case the current screen is {@code RECORDING} and loads up the UI elements to display the state.
         */
        void onObdStateChanged(int obdState);

        /**
         * Displays the OBD tooltip with the given type.
         * @param obdTooltipType the type of the OBD tooltip.
         */
        void displayObdTooltip(int obdTooltipType);

        /**
         * Called when the OBD speed is obtained.
         * @param speed the OBD speed.
         */
        void onSpeedObtained(String[] speed);

        /**
         * Hides the OBD information on the screen.
         */
        void hideObdInfo();

        /**
         * Called when the recording state changed.
         * @param isRecording {@code true} if teh recording is started, {@code false} otherwise.
         */
        void onRecordingStatusChanged(boolean isRecording);

        /**
         * Called when the details screen for obd is required to be open.
         * @param bundle the bundle containing the obd details.
         */
        void openObdDetails(Bundle bundle);

        /**
         * Displays an error for the obd. The error is represented in the interface {@code CameraObdErrors}.
         * @see CameraObdErrors
         */
        void displayObdErrors(@CameraObdErrors int errorId);
    }

    /**
     * The camera obd view model which holds all the functionality required by the fragment.
     */
    interface CameraObdViewModel {

        /**
         * The observable emits the resource id of a drawable which should be display for each state.
         * @return an observable instance for sending an event when the OBD state changes.
         */
        LiveData<Integer> getObdStateObservable();

        /**
         * The observable emits an array of {@code String} with 2 elements, containing the speed value and the speed unit (e.g km/h).
         * @return an observable instance for sending the OBD speed event.
         */
        LiveData<String[]> getObdSpeedObservable();

        /**
         * The observable emits values are represented by the interface {@link CameraObdErrors}.
         * @return an observable instance for sending the error values for obd.
         */
        LiveData<Integer> getObdErrorObservable();

        /**
         * The observable emits a {@link TooltipStyleEvent} which holds the properties used for displaying the tooltip.
         * @return an observable instance for sending the OBD tooltip event.
         */
        LiveData<TooltipStyleEvent> getObdTooltipObservable();

        /**
         * The observable emits the bundle which contains details of the current recording sequence, i.e. distance, pictures, recording, points if enabled.
         * @return an observable instance for sending the OBD details bundle.
         */
        LiveData<Bundle> getObdDetailsObservable();

        /**
         * Start the start in order to obtain the obd details regarding the current recording sequence to open the obd details screen.
         * <p> Once they are obtain the observable regarding obd details will be notified with either the result or the error.
         */
        void startObdDetailsAcquire();

        /**
         * Start the OBD connection start.
         */
        void startObdConnection();

        /**
         * Stop the OBD connection start.
         */
        void stopObdConnection();

        /**
         * Force stops the OBD connection start.
         */
        void stopObdWhenIsAttemptingToConnect();

        /**
         * @return {@code true} if the device is connected to the obd, {@code false} otherwise.
         */
        boolean isObdConnected();

        /**
         * Resume lifecycle callback notifier for the view model. This will kick-start the tooltip display for obd connection status.
         */
        void onResume();

        /**
         * Pause lifecycle callback notifier for the view model.
         */
        void onPause();
    }

    /**
     * Interface which holds errors values related to obd such as:
     * <ul>
     * <li>{@link #UNKOWN_ERROR}</li>
     * <li>{@link #RECORDING_SEQUENCE_NOT_FOUND}</li>
     * </ul>
     */
    @interface CameraObdErrors {

        /**
         * Error value for a behaviour that could not be identified.
         */
        int UNKOWN_ERROR = 0;

        /**
         * Error value for when a recording sequence could not be found in the persistence.
         */
        int RECORDING_SEQUENCE_NOT_FOUND = 1;
    }
}
