package com.telenav.osv.recorder.score;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import android.location.Location;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Segment;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.filter.LocationFilterType;
import com.telenav.osv.network.payrate.model.PayRateData;
import com.telenav.osv.network.payrate.model.PayRateItem;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.score.event.ByodDriverPayRateUpdatedEvent;
import com.telenav.osv.recorder.score.event.ScoreChangedEvent;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Component responsible for points calculation, while recording takes place
 * Created by Kalman on 17/11/2016.
 */
public class ScoreManager implements Score, PositionMatcher.SegmentsListener, ObdManager.ObdConnectionListener {

    /**
     * Default value for the pay rate and score when couldn't be calculated using the segment's coverage.
     */
    public static final int UNKNOWN_VALUE = -1;

    /**
     * Photo increment value for score.
     */
    private static final int SCORE_PHOTO_INCREMENT_VALUE = 1;

    private static final String TAG = ScoreManager.class.getSimpleName();

    /**
     * The maximum value for the coverage.
     */
    private static final int MAX_COVERAGE_VALUE = 10;

    /**
     * The value to double the points if the OBD is connected.
     */
    private static final int OBD_CONNECTED_MULTIPLIER = 2;

    /**
     * The default value if the OBD is disconnected.
     */
    private static final int OBD_DISCONNECTED_MULTIPLIER = 1;

    /**
     * Map which references each pair of coverage and {@code ScoreHistory}. This is used to update persistence with changes which occur to the current recording sequence
     * regarding only score.
     */
    private Map<Integer, ScoreHistory> scoreHistoryMap;

    /**
     * The instance to the {@code PositionMatcher} used for matching the current position with a segment.
     */
    private PositionMatcher positionMatcher;

    /**
     * Total points gained during the current recording.
     */
    private long points;

    /**
     * The sequence id for the current recording.
     */
    private String sequenceId = null;

    /**
     * The pay rate values for each coverage.
     */
    private PayRateData payRateData;

    /**
     * The instance to the {@code LocalDataSource} in order to store the score in the database.
     */
    private ScoreDataSource scoreDataSource;

    /**
     * The instance of {@code LocationService} which is used for receiving location updates.
     */
    private LocationService locationService;

    /**
     * The {@code ObdManager} fused for updates regarding the OBD state.
     */
    private ObdManager obdManager;

    /**
     * The observable for the score changes. This is used to send the score updates to the observers.
     */
    private PublishSubject<OSVEvent> publishSubject = PublishSubject.create();

    /**
     * A flag to determine if the user is BYOD having the payment model 2.0.
     */
    private boolean isUserByod20;

    /**
     * {@code true} if the recording is started, {@code false} otherwise.
     */
    private boolean isRecording;

    /**
     * The holder of all score's subscribers
     */
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * This is used to hold the obd connection state: true when the obd is connected, false otherwise.
     */
    private boolean isObdConnected;

    public ScoreManager(@NonNull ScoreDataSource scoreDataSource, @NonNull PositionMatcher positionMatcher, @NonNull LocationService locationService, @NonNull ObdManager
            obdManager) {
        this.positionMatcher = positionMatcher;
        this.scoreDataSource = scoreDataSource;
        this.positionMatcher.setSegmentsListener(this);
        this.locationService = locationService;
        this.obdManager = obdManager;
        isObdConnected = obdManager.getObdState() == ObdManager.ObdState.OBD_CONNECTED;
        obdManager.addObdConnectionListener(this);
        observeOnLocationUpdates();
    }

    @Override
    public void onSegmentsReceived(SKCoordinate location) {
        if (!isRecording) {
            return;
        }
        Log.d(TAG, "ScoreManager#onSegmentsReceived");
        Segment matchedSegment = positionMatcher.match(location);
        updateGamification(matchedSegment, false);
    }

    @Override
    public void onPictureTaken(Location location) {
        if (!isRecording) {
            return;
        }
        Log.d(TAG, "ScoreManager#onPictureTaken");
        if (sequenceId != null) {
            Segment matchedSegment = positionMatcher.onPictureTaken(location);
            updateGamification(matchedSegment, true);
        }
    }

    @Override
    public void onRecordingStateChanged(boolean isRecording, String sequenceId) {
        this.isRecording = isRecording;
        if (isRecording) {
            points = 0;
            this.sequenceId = sequenceId;
            scoreHistoryMap = new HashMap<>();
        } else {
            this.sequenceId = null;
            scoreHistoryMap = null;
            points = 0;
        }
    }

    @Override
    public void setUserType(boolean isUserByod20) {
        this.isUserByod20 = isUserByod20;
    }

    @Override
    public PayRateData getPayRateData() {
        return payRateData;
    }

    @Override
    public void setPayRateData(PayRateData payRateData) {
        this.payRateData = payRateData;
    }

    @Override
    public Observable<OSVEvent> getScoreUpdates() {
        return publishSubject.toSerialized().hide();
    }

    @Override
    public void setScoreValue(long score) {
        points = score;
    }

    @Override
    public void release() {
        Log.d(TAG, "release score component");
        if (!compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        obdManager.removeObdConnectionListener(this);
    }

    @Override
    public void onSpeedObtained(SpeedData speed) {

    }

    @Override
    public void onObdConnected() {
        isObdConnected = true;
    }

    @Override
    public void onObdDisconnected() {
        isObdConnected = false;
    }

    @Override
    public void onObdConnecting() {

    }

    @Override
    public void onObdInitialised() {

    }

    /**
     * Registers the observer for location updates.
     */
    private void observeOnLocationUpdates() {
        Log.d(TAG, "observeOnLocationUpdates");
        compositeDisposable.add(locationService.getLocationUpdates()
                .filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES))
                .subscribe(location -> {
                    if (!isRecording) {
                        return;
                    }
                    Log.d(TAG, "ScoreManager#onLocationChanged");
                    Segment segment = positionMatcher.onLocationChanged(location);
                    if (segment != null) {
                        updateGamification(segment, false);
                    }
                }));
    }

    /**
     * Updates the score or pay rate for the new segment taking into account the user type.
     * @param matchedSegment the current segment.
     * @param incrementScore {@code true} if the score should be incremented, {@code false} otherwise.
     * The score shouldn't be incremented a picture wasn't taken.
     */
    private void updateGamification(Segment matchedSegment, boolean incrementScore) {
        Log.d(TAG, "ScoreManager#updateGamification. incrementScore: " + incrementScore);

        int coverage = matchedSegment == null ? UNKNOWN_VALUE : matchedSegment.getPolyline().coverage;
        if (matchedSegment != null) {
            Log.d(TAG, "updateGamification: seqId = " + sequenceId + " matched segment " + matchedSegment.getPolyline().getIdentifier() +
                    " coverage" + " " + coverage + " obd : " + isObdConnected);
        }
        if (coverage == UNKNOWN_VALUE) {
            coverage = MAX_COVERAGE_VALUE;
        }
        if (isUserByod20) {
            updatePayRate(coverage);
        } else {
            if (sequenceId == null) {
                return;
            }
            updateScore(coverage, incrementScore);
        }
    }

    /**
     * Updates the pay rate for the current segment coverage.
     * @param coverage the coverage value for the current segment.
     */
    private void updatePayRate(int coverage) {
        if (payRateData != null && payRateData.getPayRates() != null) {
            float payRateForCurrentSegment = UNKNOWN_VALUE;
            for (PayRateItem payRateItem : payRateData.getPayRates()) {
                if ((payRateItem.payRateCoverageInterval.minPass <= coverage && payRateItem.payRateCoverageInterval.maxPass >= coverage) ||
                        (payRateItem.payRateCoverageInterval.maxPass < coverage &&
                                payRateData.getPayRates().indexOf(payRateItem) == payRateData.getPayRates().size() - 1)) {
                    if (isObdConnected) {
                        payRateForCurrentSegment = payRateItem.obdPayRateValue;
                    } else {
                        payRateForCurrentSegment = payRateItem.nonObdPayRateValue;
                    }
                }
            }
            Log.d(TAG, "ScoreManager#updateGamification. byod pay rate for current segment is: " + payRateForCurrentSegment);
            publishSubject.onNext(new ByodDriverPayRateUpdatedEvent(payRateForCurrentSegment, payRateData.getCurrency()));
        }
    }

    /**
     * Updates the points and multiplier for the current segment coverage.
     * @param coverage the coverage value for the current segment
     * @param incrementScore {@code true} if the points should be incremented,
     * {@code false} otherwise.
     */
    private void updateScore(int coverage, boolean incrementScore) {
        int cappedCoverage = Math.min(coverage, MAX_COVERAGE_VALUE);
        int segmentValue = Utils.getValueOnSegment(cappedCoverage);
        Disposable disposable = Completable.create(emitter -> {
            if (incrementScore) {
                if (scoreHistoryMap != null) {
                    ScoreHistory scoreHistory = scoreHistoryMap.get(cappedCoverage);
                    if (scoreHistory == null) {
                        scoreHistory = new ScoreHistory(UUID.randomUUID().toString(), cappedCoverage, 0, 0);
                        scoreHistoryMap.put(scoreHistory.getCoverage(), scoreHistory);
                        boolean insertScore = scoreDataSource.insertScore(scoreHistory, sequenceId);
                        Log.d(TAG, String.format("updateScore. Status: %s. Message: Insert new score history into persistence.", insertScore));
                    }
                    boolean updateScore;
                    if (isObdConnected) {
                        scoreHistory.setObdPhotoCount(scoreHistory.getObdPhotoCount() + SCORE_PHOTO_INCREMENT_VALUE);
                        updateScore = scoreDataSource.updateObdPhotoCount(scoreHistory.getID(), scoreHistory.getObdPhotoCount());
                    } else {
                        scoreHistory.setPhotoCount(scoreHistory.getPhotoCount() + SCORE_PHOTO_INCREMENT_VALUE);
                        updateScore = scoreDataSource.updatePhotoCount(scoreHistory.getID(), scoreHistory.getPhotoCount());
                    }
                    Log.d(TAG, String.format("updateScore. Status: %s. Message: Updating score history photo counts into persistence.", updateScore));
                }
                points = points + ((isObdConnected ? OBD_CONNECTED_MULTIPLIER : OBD_DISCONNECTED_MULTIPLIER) * segmentValue);
                Log.d(TAG, String.format("onPictureTaken: segmentValue %s points %s ", segmentValue, points));
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> publishSubject.onNext(new ScoreChangedEvent(points, (isObdConnected ? OBD_CONNECTED_MULTIPLIER : OBD_DISCONNECTED_MULTIPLIER) * segmentValue))
                );
        compositeDisposable.add(disposable);
    }
}
