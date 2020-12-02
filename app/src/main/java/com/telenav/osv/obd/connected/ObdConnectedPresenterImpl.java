package com.telenav.osv.obd.connected;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.joda.time.Hours;
import org.joda.time.LocalDateTime;
import org.joda.time.Minutes;
import android.os.Bundle;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.obd.Constants;
import com.telenav.osv.obd.connected.ObdConnectedContract.ObdConnectedPresenter;
import com.telenav.osv.obd.connected.ObdConnectedContract.ObdConnectedView;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.RecorderManager;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.score.event.ScoreChangedEvent;
import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Concrete implementation for {@code ObdConnectedPresenter}.
 * @author horatiuf
 * @see ObdConnectedPresenter
 */

public class ObdConnectedPresenterImpl implements ObdConnectedPresenter {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ObdConnectedPresenterImpl.class.getSimpleName();

    /**
     * The timer duration.
     */
    private static final int TIMER_DELAY_DURATION = 1;

    /**
     * The index for value in the speed conversion.
     */
    private static final int SPEED_CONVERSION_VALUE_INDEX = 0;

    /**
     * The index for metric in the speed conversion.
     */
    private static final int SPEED_CONVERSION_METRIC_INDEX = 1;

    /**
     * The view instance from {@code ObdConnectedContract}.
     */
    private ObdConnectedView view;

    /**
     * Timer that will display the duration of the recorder start.
     */
    private ScheduledThreadPoolExecutor recordingDurationTimer;

    /**
     * The duration which have passed since the recording has been active.
     */
    private LocalDateTime recordStartTime;

    /**
     * The application preferences instance for reading the metrics and recording initial time.
     */
    private ApplicationPreferences applicationPreferences;

    /**
     * Flag that denotes if the metrics in the app are set on imperial.
     */
    private boolean isImperial;

    /**
     * Flag that show if the score is enabled or disabled. This is based if the user can either have the score or not available.
     */
    private boolean isScoreEnabled;

    /**
     * Instance to {@code obdManager}.
     */
    private ObdManager obdManager;

    /**
     * Score disposable object which should be dismissed when score updates are no longer required.
     */
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * Default constructor for the current class.
     */
    ObdConnectedPresenterImpl(ObdConnectedView view, ApplicationPreferences applicationPreferences, ObdManager obdManager,
                              Score scoreManager, RecorderManager recorderManager) {
        this.view = view;
        this.applicationPreferences = applicationPreferences;
        this.obdManager = obdManager;
        isImperial = !applicationPreferences.getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        isScoreEnabled = applicationPreferences.getBooleanPreference(PreferenceTypes.K_GAMIFICATION);
        if (isScoreEnabled) {
            observeOnScoreChanges(scoreManager);
        }
        observeOnImageSaved(recorderManager);
        view.setPresenter(this);
    }

    @Override
    public void start() {
        if (applicationPreferences.getLongPreference(PreferenceTypes.K_RECORD_START_TIME) != 0) {
            Log.d(TAG, "Start. Recording detected. Starting scheduler start.");
            initialiseObdConnectedData();
            updateDuration(0);
        } else {
            Log.d(TAG, "Start. Recording not started. Updating view");
            view.updateDuration(0, 0);
        }
        view.updateSpeed(String.valueOf(0), isImperial ? FormatUtils.FORMAT_SPEED_MPH : FormatUtils.FORMAT_SPEED_KM);
        if (obdManager != null) {
            obdManager.addObdConnectionListener(this);
        }
    }

    @Override
    public boolean isScoreEnabled() {
        return isScoreEnabled;
    }

    @Override
    public void dispose() {
        recordingDurationTimer = null;
        recordStartTime = null;
        if (obdManager != null) {
            obdManager.removeObdConnectionListener(this);
        }
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }

    @Override
    public void stopCollecting() {
        if (obdManager != null) {
            applicationPreferences.saveBooleanPreference(PreferenceTypes.K_OBD_MANUAL_STOPPED, true);
            obdManager.stopCollecting();
        }
    }

    @Override
    public boolean isImperial() {
        return isImperial;
    }

    @Override
    public void initDetails(Bundle arguments) {
        if (arguments != null) {
            int pics = arguments.getInt(Constants.ARG_PICS, 0);
            int distance = arguments.getInt(Constants.ARG_DISTANCE, 0);
            int score = arguments.getInt(Constants.ARG_POINTS, 0);
            view.updateImageDetails(pics, distance);
            view.updateScore(score);
        }
    }

    @Override
    public void onSpeedObtained(SpeedData speed) {
        if (speed.getSpeed() != -1) {
            String[] speedConversion = FormatUtils.fromDistanceFromMetersToUnit(isImperial, speed.getSpeed());
            view.updateSpeed(speedConversion[SPEED_CONVERSION_VALUE_INDEX], speedConversion[SPEED_CONVERSION_METRIC_INDEX]);
        }
    }

    @Override
    public void onObdConnected() {}

    @Override
    public void onObdDisconnected() {
        view.goToObdConnect();
    }

    @Override
    public void onObdConnecting() {}

    @Override
    public void onObdInitialised() {}

    /**
     * Set observer for image saved events.
     * @param recorderManager instance used for subscription.
     */
    private void observeOnImageSaved(RecorderManager recorderManager) {
        compositeDisposable.add(recorderManager.getImageCaptureObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(imageSavedEvent -> view.updateImageDetails(imageSavedEvent.getFrameCount(), (int) imageSavedEvent.getDistance())));
    }

    /**
     * SEt observer for score changes events.
     * @param scoreManager instance used for subscription.
     */
    private void observeOnScoreChanges(Score scoreManager) {
        compositeDisposable.add(scoreManager.getScoreUpdates().observeOn(AndroidSchedulers.mainThread())
                .subscribe(scoreEvent ->
                        view.updateScore(((ScoreChangedEvent) scoreEvent).getScore())
                ));
    }

    /**
     * Initialise all related obd connected data.
     */
    private void initialiseObdConnectedData() {
        recordingDurationTimer = new ScheduledThreadPoolExecutor(1);
        recordingDurationTimer.scheduleAtFixedRate(
                () -> {
                    Log.d(TAG, "recording duration timer.");
                    updateDuration(TIMER_DELAY_DURATION);
                },
                TIMER_DELAY_DURATION,
                TIMER_DELAY_DURATION,
                TimeUnit.SECONDS);
    }

    /**
     * Calculates the duration which have passed since the recording have been started
     * @param fixedRate the fixed rate at which the duration is calculated to be added to the duration.
     */
    private void updateDuration(int fixedRate) {
        if (fixedRate != TIMER_DELAY_DURATION) {
            recordStartTime = new LocalDateTime(applicationPreferences.getLongPreference(PreferenceTypes.K_RECORD_START_TIME));
        }
        LocalDateTime localDateTime = LocalDateTime.now();
        int durationHour = Hours.hoursBetween(recordStartTime, localDateTime).getHours();
        localDateTime = localDateTime.minusHours(durationHour);
        int durationMin = Minutes.minutesBetween(recordStartTime, localDateTime).getMinutes();
        Log.d(TAG, String.format("Update duration. Hour: %s. Minutes: %s", durationHour, durationMin));
        view.updateDuration(durationHour, durationMin);
    }
}
