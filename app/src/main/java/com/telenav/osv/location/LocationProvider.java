package com.telenav.osv.location;

import android.content.Context;
import android.location.Location;
import com.telenav.osv.utils.Utils;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.processors.PublishProcessor;

/**
 * Abstract class for location provider, containing all the common functionality for both
 * {@code AndroidLocationProvider} and {@code GoogleLocationProvider}.
 * Created by Kalman on 07/09/16.
 */
abstract class LocationProvider {

    /**
     * The {@code PublishProcessor} used to subscribe for location updates.
     */
    PublishProcessor<Location> locationUpdatesProcessor;

    /**
     * The number of active subscribers.
     */
    private int totalSubscribers;

    LocationProvider() {

    }

    /**
     * Method used for create a location provider.
     * The Google location provider has priority over the android location provider.
     * @param context the application context, used to create the provider.
     * @return the instance of the location provider.
     */
    public static LocationProvider get(Context context) {
        return Utils.isGooglePlayServicesAvailable(context) ? new GoogleLocationProvider(context) : new AndroidLocationProvider(context);
    }

    /**
     * Start the request for location updates.
     */
    abstract void startLocationUpdates();

    /**
     * Stop the request for location updates.
     */
    abstract void stopLocationUpdates();

    /**
     * @return a {@code Maybe} in order to subscribe for receiving the last known location.
     * If the location can be determined the {@code onSuccess} callback will be triggered, otherwise the stream will complete with {@code onComplete} callback.
     */
    abstract Maybe<Location> getLastKnownLocation();

    /**
     * @return a {@code Flowable} in order to subscribe for location updates.
     */
    Flowable<Location> getLocationUpdates() {
        if (locationUpdatesProcessor == null) {
            locationUpdatesProcessor = PublishProcessor.create();
            startLocationUpdates();
        }
        return locationUpdatesProcessor.onBackpressureLatest()
                .doOnSubscribe(subscription -> totalSubscribers++)
                .doOnCancel(() -> {
                    totalSubscribers--;
                    if (totalSubscribers == 0) {
                        stopLocationUpdates();
                    }
                });
    }
}