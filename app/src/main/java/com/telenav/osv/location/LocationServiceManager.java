package com.telenav.osv.location;

import android.content.Context;
import android.location.Location;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Implementation of the {@link LocationService} interface that holds all the available functionality of the Location component.
 * Created by cameliao on 2/6/18.
 */
public class LocationServiceManager implements LocationService {

    /**
     * An instance to the implementation of the {@link LocationService} interface.
     */
    private static LocationServiceManager instance;

    /**
     * The logic used for receiving location updates.
     */
    private LocationProvider locationProvider;

    /**
     * Instance of the {@code AccuracyQualityChecker} which defines the current accuracy type.
     */
    private AccuracyQualityChecker accuracyQualityChecker;

    /**
     * Private constructor for the current class to hide the initialisation from external sources.
     */
    private LocationServiceManager(Context context) {
        locationProvider = LocationProvider.get(context);
        accuracyQualityChecker = new AccuracyQualityChecker();
    }

    /**
     * @return a single instance instance of the {@link LocationService} representing {@link #instance}.
     * If the {@link #instance} is not set, a new instance of the {@link LocationService} will be created.
     */
    public static LocationServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationServiceManager(context);
        }
        return instance;
    }

    @Override
    public Flowable<Location> getLocationUpdates() {
        return locationProvider.getLocationUpdates()
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .doOnCancel(() ->
                        accuracyQualityChecker.onAccuracyReset())
                .doOnNext(location ->
                        accuracyQualityChecker.onAccuracyChanged(location.getAccuracy()))
                .doOnError(error ->
                        accuracyQualityChecker.onAccuracyChanged(AccuracyType.ACCURACY_BAD));
    }

    @Override
    public Maybe<Location> getLastKnownLocation() {
        return locationProvider.getLastKnownLocation()
                .doOnSuccess(location -> accuracyQualityChecker.onAccuracyChanged(location.getAccuracy()));
    }

    @Override
    public Observable<Integer> getAccuracyType() {
        return accuracyQualityChecker.getAccuracyType()
                .subscribeOn(Schedulers.computation())
                .share();
    }
}