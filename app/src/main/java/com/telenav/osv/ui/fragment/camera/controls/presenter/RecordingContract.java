package com.telenav.osv.ui.fragment.camera.controls.presenter;

import com.telenav.osv.location.AccuracyType;
import com.telenav.osv.recorder.score.event.ByodDriverPayRateUpdatedEvent;
import com.telenav.osv.recorder.score.event.ScoreChangedEvent;

/**
 * The contract between {@link CameraControlsView} and {@link CameraControlsPresenter}.
 * @author cameliao
 */
public interface RecordingContract {
    /**
     * The camera controls presenter which holds all the available business functionality required by the view model.
     */
    interface CameraControlsPresenter {

        /**
         * Starts a recording session.
         */
        void startRecording();

        /**
         * Stops a recording session.
         */
        void stopRecording();

        /**
         * @return {@code true} if the hint for recording in background should be visible, {@code false} otherwise.
         */
        boolean getHintBackgroundPreference();

        /**
         * Saves the preference for displaying the recording in background hint.
         * @param displayHintBackgroundRecording the value to be saved.
         */
        void saveHintBackgroundPreference(boolean displayHintBackgroundRecording);

        /**
         * @return {@code true} if recording is started, {@code false} otherwise.
         */
        boolean isRecording();

        /**
         * Requests the driver's pay rate data.
         */
        void requestPayRateData();

        /**
         * @return {@code true} if the user preference distance unit is miles, {@code false} otherwise.
         */
        boolean isImperial();

        /**
         * @return {@code true} if the user type is byod driver 2.0, {@code false} otherwise.
         */
        boolean isByodDriver();

        /**
         * @return {@code true} if the points setting is enabled, {@code false} otherwise.
         */
        boolean isGamificationEnable();

        /**
         * @return the recording start time in milliseconds from preferences.
         */
        long getRecordingStartTime();

        /**
         * Release all the resources fro the current presenter.
         */
        void release();

        /**
         * Sets the score value to the score manager.
         * @param score
         */
        void setScoreValue(long score);

        /**
         * @return {@code true} if the tagging feature is enabled, {@code false} otherwise.
         */
        boolean isTaggingFeatureEnabled();

        /**
         * @return {@code true} if the tagging screen is visible, {@code false} otherwise.
         */
        boolean isTaggingMode();

        /**
         * Sets the mode for recording which can be enable or disabled.
         * @param isTaggingModeEnable {@code true} if recording is in tagging mode, {@code false} otherwise.
         */
        void setTaggingMode(boolean isTaggingModeEnable);
    }

    /**
     * The camera controls view which holds all the available ui related functionality required by the presenter
     */
    interface CameraControlsView {

        /**
         * The method is called when a new image was saved.
         * @param frameCount the number of taken photos for the current track.
         */
        void onSavedImagesChanged(int frameCount);

        /**
         * This method is called when the distance during a track is changed.
         * @param distance the distance of the current track.
         */
        void onDistanceChanged(int distance);

        /**
         * This method is called when the size on disk for the current track is changed.
         * @param sizeOnDisk the size on disk of the current track.
         */
        void onSizeChanged(float sizeOnDisk);

        /**
         * This method is called when the recording state changes.
         * @param isRecordingStarted a flag to determine if the recording is started or stopped.
         */
        void onRecordingStateChanged(boolean isRecordingStarted);

        /**
         * Method called when the score changes during a recording.
         * This method will be called only if the points setting is enable and the user type is not BYOD.
         * @param scoreChangedEvent the score event holding the score new value.
         */
        void onScoreChanged(ScoreChangedEvent scoreChangedEvent);

        /**
         * Method called when the pay rate changes during a recording.
         * This method is called when the user type is BYOD 2.0.
         * @param byodDriverPayRateUpdatedEvent the new pay rate value.
         */
        void onPaymentChanged(ByodDriverPayRateUpdatedEvent byodDriverPayRateUpdatedEvent);

        /**
         * Method called when the location accuracy changes.
         * @param accuracyType the type for the current accuracy value.
         */
        void onLocationAccuracyChanged(@AccuracyType int accuracyType);

        /**
         * Method called when an error occurred during recording.
         */
        void onRecordingErrors();

        /**
         * @return {@code true} if tagging is enabled, {@code false} otherwise.
         */
        boolean isTaggingEnabled();
    }
}
