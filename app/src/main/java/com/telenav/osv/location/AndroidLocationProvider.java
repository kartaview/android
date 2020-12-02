package com.telenav.osv.location;

import java.util.concurrent.atomic.AtomicBoolean;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.telenav.osv.utils.Log;
import io.reactivex.Maybe;

/**
 * Android location provider, this will use the {@link android.location.LocationManager} from android in order to register for location updates.
 * There are two types of providers which will be listen to in order to obtain the most accurate location.
 * These providers are: {@link android.location.LocationManager#GPS_PROVIDER} and {@link android.location.LocationManager#NETWORK_PROVIDER}.
 * Created by Kalman on 07/09/16.
 */
class AndroidLocationProvider extends LocationProvider implements LocationListener {

    private static final String TAG = AndroidLocationProvider.class.getSimpleName();

    /**
     * Helper class which handles the logic for connecting to both location providers.
     */
    private AndroidLocationProviderHelper androidLocationProviderHelper;

    /**
     * Flag representing if a request for location updates was performed.
     */
    private AtomicBoolean isLocationUpdatesStarted = new AtomicBoolean();

    /**
     * Default constructor for the current class.
     * @param context the application context, used for accessing the {@code LocationManager}
     */
    AndroidLocationProvider(Context context) {
        super();
        Log.d(TAG, "Location provider: Android location provider.");
        androidLocationProviderHelper = new AndroidLocationProviderHelper(context);
    }

    /**
     * Starts listening for continuous location updates.
     */
    void startLocationUpdates() {
        if (androidLocationProviderHelper != null && !isLocationUpdatesStarted.get()) {
            try {
                androidLocationProviderHelper.startLocationUpdates(this);
                isLocationUpdatesStarted.set(true);
            } catch (Exception e) {
                Log.d(TAG, String.format("startLocationUpdates: %s " + e.getMessage()));
            }
        }
    }

    /**
     * Stops the request for location updates.
     */
    void stopLocationUpdates() {
        if (androidLocationProviderHelper != null && isLocationUpdatesStarted.get()) {
            androidLocationProviderHelper.stopLocationUpdates();
            isLocationUpdatesStarted.set(false);
        }
        locationUpdatesProcessor = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (locationUpdatesProcessor != null) {
            locationUpdatesProcessor.onNext(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    Maybe<Location> getLastKnownLocation() {
        return Maybe.create(emitter -> {
            Location location = androidLocationProviderHelper.getLastKnownLocation();
            if (location != null) {
                emitter.onSuccess(location);
            } else {
                emitter.onComplete();
            }
        });
    }
}