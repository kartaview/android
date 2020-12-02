package com.telenav.osv.ui.fragment.camera.controls.viewmodel;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.snackbar.Snackbar;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.common.model.data.SnackBarItem;
import com.telenav.osv.location.AccuracyType;
import com.telenav.osv.recorder.gpsTrail.ListenerRecordingGpsTrail;
import com.telenav.osv.recorder.score.event.ScoreChangedEvent;
import com.telenav.osv.ui.fragment.camera.controls.event.RecordingDetails;
import com.telenav.osv.ui.fragment.camera.controls.presenter.RecordingContract;
import com.telenav.osv.ui.fragment.camera.controls.presenter.RecordingContract.CameraControlsView;
import com.telenav.osv.ui.fragment.camera.controls.presenter.RecordingPresenterImpl;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;

import java.util.ArrayList;

import javax.annotation.Nullable;

/**
 * {@code ViewModel} class that handles the view logic for the camera controls screen regarding the recording operations.
 * Implements {@link CameraControlsView} in order to receive events from the business logic.
 */
public class RecordingViewModel extends AndroidViewModel implements RecordingContract.CameraControlsView {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    private static final String TAG = RecordingViewModel.class.getSimpleName();

    private static final String FORMAT_COVERAGE_MULTIPLIER_SUFFIX = "x";

    private static final String IMG_VALUE = "imgValue";

    private static final String SIZE_VALUE = "sizeValue";

    private static final String DISTANCE_VALUE = "distanceValue";

    private static final String SCORE_VALUE = "scoreValue";

    private static final String SCORE_MULTIPLIER = "scoreMultiplier";

    private static final String DISTANCE_LABEL = "distanceLabel";

    private static final String SIZE_LABEL = "sizeLabel";

    /**
     * Instance to the presenter which handles the business logic.
     */
    private RecordingContract.CameraControlsPresenter presenter;

    /**
     * Observable instance for sending recording events.
     * The value sent by the observable is {@code true} if the recording started, {@code false} otherwise.
     */
    private MutableLiveData<Boolean> recordingObservable;

    /**
     * Observable instance for sending an event when a new image was saved.
     * The value sent by the observable represents the total number of images for the current track.
     */
    private MutableLiveData<RecordingDetails> capturedImagesObservable;

    /**
     * Observable instance for sending an event when the distance for the current track changes.
     * The value sent by the observable represents an array of {@code String} with 2 elements.
     * The first element represents the distance of the track and the second element represents the label for the unit distance(e.g km, miles).
     */
    private MutableLiveData<RecordingDetails> distanceObservable;

    /**
     * Observable instance for sending an event when the size on disk of the current track changes.
     * The value sent by the observable represents an array of {@code String} with 2 elements.
     * The first element represents the size on disk and the second element represents the label for the unit size(e.g MB).
     */
    private MutableLiveData<RecordingDetails> sizeObservable;

    /**
     * Observable instance for sending an event when a snack bar should be displayed.
     * The observable emits a {@link SnackBarItem} which contains all the details for displaying the snack bar.
     */
    private MutableLiveData<SnackBarItem> snackBarObservable;

    /**
     * Observable instance for sending an event when the recording permissions are required to be granted.
     * The observable emits an array of {@code String}, containing all the required permissions.
     */
    private MutableLiveData<String[]> recordingPermissionObservable;

    /**
     * Observable instance for sending an event when the score points is changed.
     * The observable emits the value of the updated score points.
     */
    private MutableLiveData<String> scorePointsObservable;

    /**
     * Observable instance for sending an event with the score multiplier.
     */
    private MutableLiveData<String> scoreMultiplierObservable;

    /**
     * Observable instance for sending an event when the location accuracy changed.
     * The observable emits the {@code DrawableRes} id for accuracy type which should be displayed.
     */
    private MutableLiveData<Integer> locationAccuracyObservable;

    /**
     * Observable instance for sending an event when an error occurred during recording.
     * The observable emits a {@code DialogFragment} which should be displayed.
     */
    private MutableLiveData<Boolean> recordingErrorObservable;

    /**
     * Default constructor of the current class
     * @param application instance of the application required by the extended {@code ViewModel}.
     */
    public RecordingViewModel(@NonNull Application application) {
        super(application);
        ApplicationPreferences appPrefs = ((KVApplication) application).getAppPrefs();
        KVApplication kvApplication = (KVApplication) application;
        presenter = new RecordingPresenterImpl(appPrefs,
                kvApplication.getRecorder(),
                kvApplication.getScore(),
                kvApplication.getRecordingPersistence(),
                this);
    }

    @Nullable
    @Override
    public String getCurrentSequenceIdIfSet() {
        return presenter.getCurrentRecordingSequenceId();
    }

    @Override
    public void onSavedImagesChanged(int frameCount) {
        if (capturedImagesObservable != null) {
            capturedImagesObservable.setValue(new RecordingDetails(String.valueOf(frameCount), getApplication().getString(R.string.partial_img_label)));
        }
    }

    @Override
    public void onDistanceChanged(int distance) {
        if (distanceObservable != null) {
            String[] distanceArray = FormatUtils.formatDistanceFromMeters(getApplication().getApplicationContext(), distance, StringUtils.EMPTY_STRING);
            distanceObservable.setValue(new RecordingDetails(distanceArray[0], distanceArray[1]));
        }
    }

    @Override
    public void onSizeChanged(float sizeOnDisk) {
        if (sizeObservable != null) {
            String[] sizeArray = FormatUtils.formatSizeDetailed(sizeOnDisk);
            sizeObservable.setValue(new RecordingDetails(sizeArray[0], sizeArray[1]));
        }
    }

    @Override
    public void onRecordingStateChanged(boolean isRecordingStarted) {
        if (recordingObservable == null) {
            return;
        }
        if (isRecordingStarted) {
            initRecordingDetails();
        }
        if (!presenter.getHintBackgroundPreference() && isRecordingStarted) {
            snackBarObservable.setValue(new SnackBarItem(getApplication().getString(R.string.turn_off_screen_hint),
                    Snackbar.LENGTH_INDEFINITE, getApplication().getString(R.string.got_it_label),
                    v -> {
                        presenter.saveHintBackgroundPreference(true);
                        snackBarObservable.setValue(null);
                    }));
        }
        recordingObservable.setValue(isRecordingStarted);
    }

    @Override
    public void onScoreChanged(ScoreChangedEvent scoreChangedEvent) {
        if (scorePointsObservable == null || scoreMultiplierObservable == null) {
            return;
        }
        String multiplier = scoreChangedEvent.getMultiplier() + FORMAT_COVERAGE_MULTIPLIER_SUFFIX;
        scoreMultiplierObservable.setValue(multiplier);
        scorePointsObservable.setValue(String.valueOf(scoreChangedEvent.getScore()));
    }

    @Override
    public void onLocationAccuracyChanged(int accuracyType) {
        if (locationAccuracyObservable == null) {
            return;
        }
        switch (accuracyType) {
            case AccuracyType.ACCURACY_BAD:
                locationAccuracyObservable.setValue(R.drawable.vector_gps_low);
                break;
            case AccuracyType.ACCURACY_MEDIUM:
                locationAccuracyObservable.setValue(R.drawable.vector_gps_medium);
                break;
            default:
                locationAccuracyObservable.setValue(null);
                break;
        }
    }

    @Override
    public void onRecordingErrors() {
        recordingErrorObservable.setValue(true);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "release recording viewModel");
        presenter.release();
    }

    /**
     * Starts a recording if the recording permissions are granted.
     */
    public void startRecording() {
        if (checkPermissionsForRecording() && !isRecording()) {
            presenter.startRecording();
            recordingPermissionObservable.setValue(null);
        }
    }

    /**
     * Stops a recording session.
     */
    public void stopRecording() {
        presenter.stopRecording();
    }

    /**
     * Method defining the logic for the {@code onStart} method from view's lifecycle event.
     */
    public void onStart() {
        locationAccuracyObservable.setValue(locationAccuracyObservable.getValue());
    }

    /**
     * Method defining the logic for the {@code onStop} method from view's lifecycle event.
     */
    public void onStop() {
        recordingErrorObservable.setValue(false);
    }

    /**
     * @return the observable instance of {@link #recordingObservable}.
     */
    public LiveData<Boolean> getRecordingObservable() {
        if (recordingObservable == null) {
            recordingObservable = new MutableLiveData<>();
        }
        return recordingObservable;
    }

    /**
     * @return {@code milliseconds} for the recording start time.
     */
    public long getRecordingStartTime() {
        return presenter.getRecordingStartTime();
    }

    /**
     * @return the observable instance of {@link #capturedImagesObservable}.
     */
    public LiveData<RecordingDetails> getCapturedImagesObservable() {
        if (capturedImagesObservable == null) {
            capturedImagesObservable = new MutableLiveData<>();
        }
        return capturedImagesObservable;
    }

    /**
     * @return the observable instance of {@link #distanceObservable}.
     */
    public LiveData<RecordingDetails> getDistanceObservable() {
        if (distanceObservable == null) {
            distanceObservable = new MutableLiveData<>();
        }
        return distanceObservable;
    }

    /**
     * @return the observable instance of {@link #sizeObservable}.
     */
    public LiveData<RecordingDetails> getSizeObservable() {
        if (sizeObservable == null) {
            sizeObservable = new MutableLiveData<>();
        }
        return sizeObservable;
    }

    /**
     * @return the observable instance of {@link #snackBarObservable}.
     */
    public LiveData<SnackBarItem> getSnackBarObservable() {
        if (snackBarObservable == null) {
            snackBarObservable = new MutableLiveData<>();
        }
        return snackBarObservable;
    }

    /**
     * @return the observable instance of {@link #recordingPermissionObservable}.
     */
    public LiveData<String[]> getRecordingPermissionObservable() {
        if (recordingPermissionObservable == null) {
            recordingPermissionObservable = new MutableLiveData<>();
        }
        return recordingPermissionObservable;
    }

    /**
     * @return the observable instance of {@link #scorePointsObservable}.
     */
    public LiveData<String> getScorePointsObservable() {
        if (scorePointsObservable == null) {
            scorePointsObservable = new MutableLiveData<>();
        }
        return scorePointsObservable;
    }

    /**
     * @return the observable instance of {@link #scoreMultiplierObservable}.
     */
    public LiveData<String> getScoreMultiplierObservable() {
        if (scoreMultiplierObservable == null) {
            scoreMultiplierObservable = new MutableLiveData<>();
        }
        return scoreMultiplierObservable;
    }

    /**
     * @return the observable instance of {@link #locationAccuracyObservable}.
     */
    public LiveData<Integer> getLocationAccuracyObservable() {
        if (locationAccuracyObservable == null) {
            locationAccuracyObservable = new MutableLiveData<>();
        }
        return locationAccuracyObservable;
    }

    /**
     * @return the observable instance of {@link #recordingErrorObservable}.
     */
    public MutableLiveData<Boolean> getRecordingErrorObservable() {
        if (recordingErrorObservable == null) {
            recordingErrorObservable = new MutableLiveData<>();
        }
        return recordingErrorObservable;
    }

    public void setListenerRecordingGpsTrail(ListenerRecordingGpsTrail listenerRecordingGpsTrail) {
        presenter.setRecordingListenerGpsTrail(listenerRecordingGpsTrail);
    }

    public void removeListenerRecordingGpsTrail(ListenerRecordingGpsTrail listenerRecordingGpsTrail) {
        presenter.removeRecordingListenerGpsTrail(listenerRecordingGpsTrail);
    }

    /**
     * @return {@code true} if the recording started, {@code false} otherwise.
     */
    public boolean isRecording() {
        return presenter.isRecording();
    }

    public void saveInstanceState(Bundle outState) {
        if (!presenter.isRecording()) {
            return;
        }
        Log.d(TAG, "save state");
        if (capturedImagesObservable.getValue() != null) {
            outState.putInt(IMG_VALUE, Integer.parseInt(capturedImagesObservable.getValue().getValue()));
        }
        if (distanceObservable.getValue() != null) {
            outState.putString(DISTANCE_VALUE, distanceObservable.getValue().getValue());
            outState.putString(DISTANCE_LABEL, distanceObservable.getValue().getLabel());
        }
        if (sizeObservable.getValue() != null) {
            outState.putString(SIZE_VALUE, sizeObservable.getValue().getValue());
            outState.putString(SIZE_LABEL, sizeObservable.getValue().getLabel());
        }
        if (scorePointsObservable.getValue() != null) {
            outState.putString(SCORE_VALUE, scorePointsObservable.getValue());
            outState.putString(SCORE_MULTIPLIER, scoreMultiplierObservable.getValue());
        }
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        if (!presenter.isRecording()) {
            return;
        }
        Log.d(TAG, "restore state");
        onSavedImagesChanged(savedInstanceState.getInt(IMG_VALUE, 0));
        if (distanceObservable != null) {
            distanceObservable.setValue(new RecordingDetails(
                    savedInstanceState.getString(DISTANCE_VALUE),
                    savedInstanceState.getString(DISTANCE_LABEL)
            ));
        }
        if (sizeObservable != null) {
            sizeObservable.setValue(new RecordingDetails(
                    savedInstanceState.getString(SIZE_VALUE),
                    savedInstanceState.getString(SIZE_LABEL)
            ));
        }
        if (isScoreVisible()) {
            scorePointsObservable.setValue(savedInstanceState.getString(SCORE_VALUE));
            scoreMultiplierObservable.setValue(savedInstanceState.getString(SCORE_MULTIPLIER));
            if (scorePointsObservable.getValue() != null) {
                presenter.setScoreValue(Long.parseLong(scorePointsObservable.getValue()));
            }
        }
    }

    private boolean isScoreVisible() {
        return presenter.isGamificationEnable();
    }

    /**
     * Initializes the recording details with the default values for a new recording session.
     */
    private void initRecordingDetails() {
        onSavedImagesChanged(0);
        onDistanceChanged(0);
        onSizeChanged(0);
    }

    /**
     * Checks the permissions for a recording session.
     * @return {@code true} if the permissions are granted, {@code false} if the permission are denied.
     */
    private boolean checkPermissionsForRecording() {
        Log.d(TAG, "checkPermissionsForRecording: ");
        ArrayList<String> needed = new ArrayList<>();
        Context context = getApplication().getApplicationContext();
        int cameraPermitted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        int locationPermitted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        int storagePermitted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (locationPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (storagePermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (needed.size() > 0) {
            String[] array = new String[needed.size()];
            needed.toArray(array);
            recordingPermissionObservable.setValue(array);
            return false;
        }
        return true;
    }
}
