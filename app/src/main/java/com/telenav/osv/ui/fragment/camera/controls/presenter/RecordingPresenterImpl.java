package com.telenav.osv.ui.fragment.camera.controls.presenter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.reactivestreams.Publisher;
import android.content.Context;
import android.content.SharedPreferences;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.location.AccuracyType;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.filter.LocationFilterType;
import com.telenav.osv.network.payrate.PayRateInteractor;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.score.event.ByodDriverPayRateUpdatedEvent;
import com.telenav.osv.recorder.score.event.ScoreChangedEvent;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;

/**
 * Concrete implementation for {@code CameraControlsPresenter}.
 * @see RecordingContract.CameraControlsPresenter
 */
public class RecordingPresenterImpl implements RecordingContract.CameraControlsPresenter {

    private static final String TAG = RecordingPresenterImpl.class.getSimpleName();

    /**
     * The maximum delay time between two location updates.
     */
    private static final int TIME_OUT_DELAY = 5;

    /**
     * Delay time in milliseconds used to subscribe for location updates when a time out event was produced.
     */
    private static final int LOCATION_SUBSCRIPTION_DELAY = 100;

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
     * Interactor for pay rate requests when the user type is BYOD.
     */
    private PayRateInteractor payRateInteractor;

    /**
     * A flag which is {@code true} if the user is BYOD 2.0, {@code false} otherwise.
     */
    private boolean isByod20Driver;

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
     * @param context the application context for initialising the profile {@code SharedPreferences}
     * @param appPrefs the application preference instance.
     * @param recorder the instance to the recording logic.
     * @param score the instance to the {@code Score} implementation.
     * @param payRateInteractor the interactor for pay arte requests.
     * @param view the instance to the view.
     */
    public RecordingPresenterImpl(Context context, ApplicationPreferences appPrefs, RecorderManager recorder,
                                  Score score, RecordingPersistence recordingPersistence, LocationService locationService, PayRateInteractor payRateInteractor,
                                  RecordingContract.CameraControlsView view) {
        this.appPrefs = appPrefs;
        this.recorder = recorder;
        this.recorder.setRecordingPersistence(recordingPersistence);
        this.score = score;
        this.payRateInteractor = payRateInteractor;
        this.view = view;
        initScoreManager(context);
        observeOnAccuracyUpdates(locationService);
        observeOnLocationTimeOut(locationService);
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

    @Override
    public void stopRecording() {
        recorder.stopRecording();
    }

    @Override
    public void setScoreValue(long points) {
        score.setScoreValue(points);
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
    public boolean isByodDriver() {
        return isByod20Driver;
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
    public void requestPayRateData() {
        if (score != null && score.getPayRateData() == null && isByod20Driver) {
            compositeDisposable.add(payRateInteractor.getDriverPayRateDetails()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(payRateData -> {
                                Log.d(TAG, "Success fetch pay rate data ");
                                score.setPayRateData(payRateData.getPayRateData());
                            }, e -> Log.d(TAG, "Failed to fetch pay rate data from the backend. Do nothing.")
                    ));
        }
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

    @Override
    public boolean isTaggingMode() {
        return appPrefs.getBooleanPreference(PreferenceTypes.K_TAGGING_MODE);
    }

    @Override
    public void setTaggingMode(boolean isTaggingModeEnable) {
        appPrefs.saveBooleanPreference(PreferenceTypes.K_TAGGING_MODE, isTaggingModeEnable);
    }

    @Override
    public boolean isTaggingFeatureEnabled() {
        return appPrefs != null && appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_RECORDING_TAGGING);
    }

    /**
     * Registers the observer for location events.
     * If no location is received for 5 seconds the accuracy type will be set to bad.
     * @param locationService the service for register the observer.
     */
    private void observeOnLocationTimeOut(LocationService locationService) {
        locationAccuracyCompositeDisposable.add(locationService.getLocationUpdates()
                .filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES))
                .timeout(TIME_OUT_DELAY, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .retryWhen(throwableFlowable ->
                        throwableFlowable.flatMap((Function<Throwable, Publisher<?>>) throwable -> {
                            Log.d(TAG, "onLocationTimeout");
                            view.onLocationAccuracyChanged(AccuracyType.ACCURACY_BAD);
                            if (throwable instanceof TimeoutException) {
                                return Flowable.timer(LOCATION_SUBSCRIPTION_DELAY, TimeUnit.MILLISECONDS);
                            }
                            return Flowable.error(throwable);
                        }))
                .doOnError(e -> view.onLocationAccuracyChanged(AccuracyType.ACCURACY_BAD))
                .subscribe());
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
                        if (isGamificationEnable || isByod20Driver) {
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
     * @param locationService the instance of {@code LocationService} used to register for accuracy type updates.
     */
    private void observeOnAccuracyUpdates(LocationService locationService) {
        locationAccuracyCompositeDisposable.add(locationService.getAccuracyType()
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
                        } else if (event instanceof ByodDriverPayRateUpdatedEvent) {
                            view.onPaymentChanged((ByodDriverPayRateUpdatedEvent) event);
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
     * @param context the context used for {@code Profile SharedPreferences} initialisation.
     */
    private void initScoreManager(Context context) {
        SharedPreferences profilePrefs = context.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
        String paymentModel = profilePrefs.getString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, ProfileFragment.PAYMENT_MODEL_VERSION_10);
        boolean isDriver = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, PreferenceTypes.USER_TYPE_UNKNOWN) == PreferenceTypes.USER_TYPE_BYOD;
        boolean isPaymentModel20 = ProfileFragment.PAYMENT_MODEL_VERSION_20.equals(paymentModel);
        isByod20Driver = isPaymentModel20 && isDriver;
        isGamificationEnable = appPrefs.getBooleanPreference(PreferenceTypes.K_GAMIFICATION, true);
        Log.d(TAG, "paymentModel: " + paymentModel + "\n" +
                "isDriver: " + isDriver + "\n" +
                "isPaymentModel20: " + isPaymentModel20 + "\n" +
                "enablePayRateForByod: " + isByod20Driver + "\n" +
                "enabledForNormalUser: " + isGamificationEnable);
        if (score != null) {
            score.setUserType(isByod20Driver);
        }
    }
}
