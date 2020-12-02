package com.telenav.osv.ui.fragment.camera.controls.presenter;

import javax.annotation.Nullable;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.gpsTrail.ListenerRecordingGpsTrail;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.score.event.ScoreChangedEvent;
import com.telenav.osv.utils.Log;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Concrete implementation for {@code CameraControlsPresenter}.
 * @see RecordingContract.CameraControlsPresenter
 */
public class RecordingPresenterImpl implements RecordingContract.CameraControlsPresenter {

    private static final String TAG = RecordingPresenterImpl.class.getSimpleName();

    /**
     * Instance to the recording manager.
     */
    private RecorderManager recorder;

    /**
     * The application preferences instance for reading the value for displaying the recording in background hint.
     */
    private ApplicationPreferences appPrefs;

    /**
     * The view instance from {@code RecordingContract}.
     */
    private RecordingContract.CameraControlsView view;

    /**
     * Instance to the {@code ScoreManager} which handles all the logic for score computation.
     */
    private Score score;

    /**
     * A flag which is {@code true} if the points setting is enable, {@code false} otherwise.
     */
    private boolean isGamificationEnable;

    /**
     * The holder for all the disposable objects in order to dispose them when there are not required.
     */
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * The {@code Disposable} of the location accuracy which is used to unregister from receiving accuracy updates.
     */
    private CompositeDisposable locationAccuracyCompositeDisposable = new CompositeDisposable();

    /**
     * Default constructor for the current class.
     * @param appPrefs the application preference instance.
     * @param recorder the instance to the recording logic.
     * @param score the instance to the {@code Score} implementation.
     * @param view the instance to the view.
     */
    public RecordingPresenterImpl(ApplicationPreferences appPrefs, RecorderManager recorder,
                                  Score score, RecordingPersistence recordingPersistence,
                                  RecordingContract.CameraControlsView view) {
        this.appPrefs = appPrefs;
        this.recorder = recorder;
        this.recorder.setRecordingPersistence(recordingPersistence);
        this.score = score;
        this.view = view;
        initScoreManager();
        observeOnAccuracyUpdates();
        if (isRecording()) {
            initRecording();
            observeOnImageCaptureEvents();
            observeOnScoreUpdates();
            score.onRecordingStateChanged(true, appPrefs.getStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID));
        }
    }

    @Override
    public void startRecording() {
        initRecording();
        recorder.startRecording();
    }

    @Override
    public boolean isRecording() {
        return recorder.isRecording();
    }

    @Nullable
    @Override
    public String getCurrentRecordingSequenceId() {
        return recorder.getCurrentSequenceId();
    }

    @Override
    public void stopRecording() {
        recorder.stopRecording();
    }

    @Override
    public void setScoreValue(long points) {
        score.setScoreValue(points);
    }

    @Override
    public void setRecordingListenerGpsTrail(ListenerRecordingGpsTrail listenerGpsTrail) {
        recorder.setListenerRecordingGpsTrail(listenerGpsTrail);
    }

    @Override
    public void removeRecordingListenerGpsTrail(ListenerRecordingGpsTrail listenerGpsTrail) {
        recorder.removeListenerRecordingGpsTrail(listenerGpsTrail);
    }

    @Override
    public boolean getHintBackgroundPreference() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, false);
    }

    @Override
    public void saveHintBackgroundPreference(boolean displayHintBackgroundRecording) {
        appPrefs.saveBooleanPreference(PreferenceTypes.K_HINT_BACKGROUND, displayHintBackgroundRecording);
    }

    @Override
    public boolean isImperial() {
        return !appPrefs.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
    }

    @Override
    public boolean isGamificationEnable() {
        return isGamificationEnable;
    }

    @Override
    public long getRecordingStartTime() {
        return appPrefs.getLongPreference(PreferenceTypes.K_RECORD_START_TIME);
    }

    @Override
    public void release() {
        Log.d(TAG, "release recording presenter");
        if (score != null) {
            score.release();
        }
        if (locationAccuracyCompositeDisposable != null && !locationAccuracyCompositeDisposable.isDisposed()) {
            locationAccuracyCompositeDisposable.dispose();
        }
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
    }

    /**
     * Initializes the observer for recording events.
     */
    private void initRecording() {
        //register to receive the recording state event on the main thread.
        compositeDisposable.add(recorder.getRecordingStateObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(isRecordingStarted -> {
                    view.onRecordingStateChanged(isRecordingStarted);
                    if (isRecordingStarted) {
                        observeOnImageCaptureEvents();
                        if (isGamificationEnable) {
                            observeOnScoreUpdates();
                        }
                        score.onRecordingStateChanged(true, appPrefs.getStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID));
                    } else {
                        score.onRecordingStateChanged(false, appPrefs.getStringPreference(PreferenceTypes.K_CURRENT_SEQUENCE_ID));
                        compositeDisposable.clear();
                    }
                }));
        compositeDisposable.add(recorder.getRecordingErrorObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> view.onRecordingErrors()));
    }

    /**
     * Registers an observer for the location accuracy updates.
     */
    private void observeOnAccuracyUpdates() {
        locationAccuracyCompositeDisposable.add(recorder.getAccuracyType()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accuracyType -> {
                    Log.d(TAG, String.format("onLocationAccuracyChanged: %s", accuracyType));
                    view.onLocationAccuracyChanged(accuracyType);
                }));
    }

    /**
     * Registers the observer on score updates.
     */
    private void observeOnScoreUpdates() {
        if (score != null) {
            compositeDisposable.add(score.getScoreUpdates()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(event -> {
                        if (event instanceof ScoreChangedEvent) {
                            view.onScoreChanged((ScoreChangedEvent) event);
                        }
                    }));
        }
    }

    /**
     * Subscribes for receiving the image capture events on the main thread.
     */
    private void observeOnImageCaptureEvents() {
        compositeDisposable.add(recorder.getImageCaptureObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        imageSavedEvent -> {
                            view.onSizeChanged(imageSavedEvent.getDiskSize());
                            view.onDistanceChanged((int) imageSavedEvent.getDistance());
                            view.onSavedImagesChanged(imageSavedEvent.getFrameCount());
                            score.onPictureTaken(imageSavedEvent.getImageLocation());
                        },
                        throwable -> Log.d(TAG, String.format("observeOnImageCaptureEvents. Status: error. Message: %s", throwable.getLocalizedMessage()))
                ));
    }

    /**
     * Initialises the score manager taking into account the user type.
     */
    private void initScoreManager() {
        isGamificationEnable = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
        Log.d(TAG,
                "initScoreManager. Status: " + isGamificationEnable);
    }
}
