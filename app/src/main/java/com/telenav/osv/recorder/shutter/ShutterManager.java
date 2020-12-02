package com.telenav.osv.recorder.shutter;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.reactivestreams.Publisher;
import android.location.Location;
import android.util.Pair;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.listener.ShutterListener;
import com.telenav.osv.location.AccuracyQualityChecker;
import com.telenav.osv.location.AccuracyType;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.filter.LocationFilterType;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.metadata.callback.MetadataGpsCallback;
import com.telenav.osv.recorder.metadata.callback.MetadataObdCallback;
import com.telenav.osv.recorder.shutter.shutterlogic.AutoShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.BenchmarkShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.GpsShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.IdleShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.ObdShutterLogic;
import com.telenav.osv.recorder.shutter.shutterlogic.ShutterLogic;
import com.telenav.osv.utils.Log;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * Implementation of the {@link Shutter} interface that holds all the available functionality for the Shutter logic.
 * Created by cameliao on 2/5/18.
 */

public class ShutterManager implements Shutter, ShutterListener, ObdManager.ObdConnectionListener {

    private static final String TAG = ShutterManager.class.getSimpleName();

    /**
     * The maximum delay time between two location updates.
     */
    private static final int TIME_OUT_DELAY = 6;

    /**
     * Delay time in milliseconds used to subscribe for location updates when a time out event was produced.
     */
    private static final int LOCATION_SUBSCRIPTION_DELAY = 100;

    /**
     * Represents the OBD logic used for taking a snapshot.
     * This logic is functional if there is an OBD connected and the speed received through OBD is valid.
     * The OBD logic has top priority.
     */
    private final ShutterLogic obdLogic = new ObdShutterLogic();

    /**
     * Represents the GPS logic for taking a snapshot.
     * This logic is functional when the location accuracy is one of:
     * {@link AccuracyType#ACCURACY_MEDIUM}, {@link AccuracyType#ACCURACY_GOOD}.
     */
    private final ShutterLogic gpsLogic = new GpsShutterLogic();

    /**
     * Represents the auto logic for taking the snapshot.
     * This logic is functional if was received a quality position from the GPS with a low accuracy.
     * In this case the photo are taken every five seconds.
     */
    private final ShutterLogic autoLogic = new AutoShutterLogic();

    /**
     * Represents the default logic for taking a snapshot.
     * In this case no photos are taken.
     */
    private final ShutterLogic idleLogic = new IdleShutterLogic();

    /**
     * Triggers snapshots on a constant frequency, while ignoring sensor data (location, speed).
     * <p>
     * Useful for debugging the whole picture-taking, encoding pipeline, where a having a constant
     * stream of pictures is more useful when compared to having pictures taken at random intervals.
     */
    private BenchmarkShutterLogic benchmarkLogic;

    /**
     * The instance of the OBD quality checker which handles when the {@link #obdLogic} should be timed out.
     */
    private ObdQualityChecker obdQualityChecker;

    /**
     * Represents the current logic used for taking a snapshot.
     */
    private ShutterLogic currentLogic = idleLogic;

    /**
     * The observable object which will notify the client when a photo should be taken
     */
    private PublishSubject<Pair<Float, Location>> takePhotoSubject;

    /**
     * The action which should be done when onComplete from OBD time out is called
     */
    private Action actionObdTimeOut = () -> {
        Log.d(TAG, "onObdSpeedTimeOut");
        if (obdLogic.isFunctional()) {
            obdLogic.setFunctional(false);
            logicPrioritiesChanged();
        }
    };

    /**
     * Holder for all the required observers.
     * The observers should be disposed when are not required.
     */
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * Instance of {@code LocationService} implementation
     * used to register for location updates.
     */
    private LocationService locationService;

    /**
     * Instance of {@code ObdManager} used to register for obd connection updates.
     */
    private ObdManager obdManager;

    /**
     * Instance for obd callback.
     */
    private MetadataObdCallback metadataObdCallback;

    /**
     * Instance for gps callback.
     */
    private MetadataGpsCallback metadataGpsCallback;

    /**
     * Instance of the {@code AccuracyQualityChecker} which defines the current accuracy type.
     */
    private AccuracyQualityChecker accuracyQualityChecker;

    /**
     * Default constructor for the current class.
     */
    public ShutterManager(LocationService locationService, ObdManager obdManager, boolean isBenchmarkLogicEnabled, MetadataObdCallback metadataObdCallback,
                          MetadataGpsCallback metadataGpsCallback, AccuracyQualityChecker accuracyQualityChecker) {
        this.locationService = locationService;
        this.obdManager = obdManager;
        this.benchmarkLogic = new BenchmarkShutterLogic(isBenchmarkLogicEnabled);
        this.metadataObdCallback = metadataObdCallback;
        this.metadataGpsCallback = metadataGpsCallback;
        this.accuracyQualityChecker = accuracyQualityChecker;
    }

    @Override
    public void onRecordingStateChanged(boolean isRecording) {
        Log.d(TAG, "onRecordingStateChanged. Is recording: " + isRecording);
        if (isRecording) {
            observeOnLocationChanges(locationService);
            obdManager.addObdConnectionListener(this);
            autoLogic.setFunctional(true);
        } else {
            autoLogic.setFunctional(false);
            if (!compositeDisposable.isDisposed()) {
                compositeDisposable.clear();
            }
            obdManager.removeObdConnectionListener(this);
        }
        logicPrioritiesChanged();
    }

    @Override
    public Observable<Pair<Float, Location>> getTakeImageObservable() {
        if (takePhotoSubject == null) {
            takePhotoSubject = PublishSubject.create();
        }
        return takePhotoSubject.toSerialized().hide();
    }

    @Override
    public Location getCurrentLocationForManualTakeImage() {
        return currentLogic.getCurrentLocationIfAvailable();
    }

    @Override
    public void requestTakeSnapshot(float distance, Location gpsPhoto) {
        if (takePhotoSubject != null) {
            takePhotoSubject.onNext(new Pair<>(distance, gpsPhoto));
        }
    }

    @Override
    public void logObdData(long timeStamp, int speed) {
        if (metadataObdCallback != null) {
            metadataObdCallback.onObdCallback(timeStamp, speed);
        }
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroy ShutterManager");
        currentLogic.stop();
        currentLogic.setShutterListener(null);
    }

    @Override
    public void onSpeedObtained(SpeedData speedData) {
        if (obdQualityChecker == null) {
            obdQualityChecker = new ObdQualityChecker();
            obdQualityChecker.setActionForTimeOut(actionObdTimeOut);
        }
        if (speedData.getSpeed() != -1) {
            if (!obdLogic.isFunctional()) {
                obdLogic.setFunctional(true);
                logicPrioritiesChanged();
            }
        }
        obdQualityChecker.onSpeedObtained(speedData);
        currentLogic.onSpeedChanged(speedData);
    }

    @Override
    public void onObdConnected() {

    }

    @Override
    public void onObdDisconnected() {

    }

    @Override
    public void onObdConnecting() {

    }

    @Override
    public void onObdInitialised() {

    }

    @Override
    public Observable<Integer> getAccuracyType() {
        return accuracyQualityChecker.getAccuracyType()
                .subscribeOn(Schedulers.computation())
                .share();
    }

    /**
     * Register the observer for location updated.
     * @param locationService the {@code LocationServiceManager} instance.
     */
    private void observeOnLocationChanges(LocationService locationService) {
        Log.d(TAG, "observeOnLocationUpdates");
        compositeDisposable.add(locationService.getLocationUpdates()
                .filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES))
                .timeout(TIME_OUT_DELAY, TimeUnit.SECONDS)
                .retryWhen(throwableFlowable ->
                        throwableFlowable.flatMap((Function<Throwable, Publisher<?>>) throwable -> {
                            if (throwable instanceof TimeoutException) {
                                Log.d(TAG, "observeOnLocationChanges. Status: location timeout.");
                                onGpsAccuracyChangeIfNeccesary(accuracyQualityChecker.onAccuracyChanged(AccuracyType.ACCURACY_BAD));
                                return Flowable.timer(LOCATION_SUBSCRIPTION_DELAY, TimeUnit.MILLISECONDS);
                            }
                            return Flowable.error(throwable);
                        }))
                .doOnComplete(() -> {
                    Log.d(TAG, "observeOnLocationChanges. Status: complete. ");
                    accuracyQualityChecker.onAccuracyReset();
                })
                .doOnCancel(() -> {
                    Log.d(TAG, "observeOnLocationChanges. Status: cancel. ");
                    accuracyQualityChecker.onAccuracyReset();
                })
                .doOnError(throwable -> {
                    Log.d(TAG, "observeOnLocationChanges. Error: " + throwable.getMessage());
                    onGpsAccuracyChangeIfNeccesary(accuracyQualityChecker.onAccuracyChanged(AccuracyType.ACCURACY_BAD));
                })
                .subscribe(this::onLocationChanged));
    }

    /**
     * Update the shutter logic when a new location is received.
     * @param location the new location.
     */
    private void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged. Location: " + location);
        metadataGpsCallback.onGpsLog(location);
        //check the accuracy
        onGpsAccuracyChangeIfNeccesary(accuracyQualityChecker.onAccuracyChanged(location.getAccuracy()));
        //issue a trigger for location change to the current logic
        currentLogic.onLocationChanged(location);
    }

    /**
     * Handles the shutter logic for the given accuracy type.
     * Auto logic  - when the accuracy is bad
     * GPS logic - when the accuracy is medium or good
     * @param type the type of the location accuracy
     */
    private void onGpsAccuracyChangeIfNeccesary(int type) {
        Log.d(TAG, String.format("onLocationAccuracyChanged: %s", type));
        if (type == AccuracyType.ACCURACY_BAD && gpsLogic.isFunctional()) {
            gpsLogic.setFunctional(false);
            logicPrioritiesChanged();
        } else if (!gpsLogic.isFunctional()) {
            gpsLogic.setFunctional(true);
            logicPrioritiesChanged();
        }
    }

    /**
     * Changes the current logic for taking a snapshot if other logic is better than the current one.
     */
    private void logicPrioritiesChanged() {
        ShutterLogic appropriate = null;
        if (benchmarkLogic.betterThan(currentLogic)) {
            appropriate = benchmarkLogic;
        } else if (obdLogic.betterThan(currentLogic)) {
            appropriate = obdLogic;
        } else if (gpsLogic.betterThan(currentLogic)) {
            appropriate = gpsLogic;
        } else if (autoLogic.betterThan(currentLogic)) {
            appropriate = autoLogic;
        } else if (idleLogic.betterThan(currentLogic)) {
            appropriate = idleLogic;
        }
        if (appropriate != null) {
            Log.d(TAG, "logicPrioritiesChanged. Current logic: " + currentLogic + ". New logic: " + appropriate + "Shutter: " + this);
            //persist the current location logic for the use case where it is required by
            Location currentLogicLocation = currentLogic.getCurrentLocationIfAvailable();
            //resets the current location for the current logic
            currentLogic.stop();
            //clears the listeners for the current logic
            currentLogic.setShutterListener(null);
            //exchange the appropriate with the current logic
            currentLogic = appropriate;
            //set the listener and start the current logic
            currentLogic.setShutterListener(this);
            currentLogic.start(currentLogicLocation);
        }
    }
}