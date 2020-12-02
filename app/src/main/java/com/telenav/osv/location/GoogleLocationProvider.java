package com.telenav.osv.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.telenav.osv.utils.Log;
import io.reactivex.Maybe;

/**
 * Google location provider, using a {@code FusedLocationProviderClient} which is battery-efficient,
 * managing the underlying location technologies such as {@code GPS} and {@code NETWORK}.
 * This is available only for some devices that came with the Google PlayServices.
 */
class GoogleLocationProvider extends LocationProvider {

    private static final String TAG = "GoogleLocationManager";

    /**
     * The slower interval value used for location updates.
     */
    private static final long LOCATION_UPDATES_INTERVAL_MILLISECONDS = 1000;

    /**
     * The fastest interval value used for location updates.
     */
    private static final long LOCATION_UPDATES_FASTEST_INTERVAL_MILLISECONDS = 500;

    /**
     * Google location client optimised for battery-efficiency.
     */
    private FusedLocationProviderClient fusedLocationProviderClient;

    /**
     * The request settings for location  updates containing the {@code LocationRequest} characteristics.
     */
    private LocationSettingsRequest locationSettingsRequest;

    /**
     * The location request used for specify the custom parameters for location updates.
     */
    private LocationRequest locationRequest;

    /**
     * The settings client which is used for ensure that the device's system settings are properly configures
     * in order to invoke the request for location updates.
     */
    private SettingsClient settingsClient;

    /**
     * Callback used for receiving the location updates.
     */
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if (locationUpdatesProcessor != null) {
                locationUpdatesProcessor.onNext(location);
            }
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
        }
    };

    /**
     * Default constructor for the current class.
     * @param context the application context used to configure the location provider client.
     */
    GoogleLocationProvider(Context context) {
        super();
        Log.d(TAG, "Location provider: Google location provider.");
        settingsClient = LocationServices.getSettingsClient(context);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        createLocationRequests();
    }

    /**
     * Starts a request for location updates.
     */
    void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnCompleteListener(task -> {
                    try {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    } catch (SecurityException exception) {
                        Log.d(TAG, String.format("startLocationUpdates: %s", exception.getMessage()));
                        if (locationUpdatesProcessor != null) {
                            locationUpdatesProcessor.onError(exception);
                        }
                    }
                });
    }

    /**
     * Stops the request for location updates.
     */
    void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationUpdatesProcessor = null;
    }

    @SuppressLint("MissingPermission")
    @Override
    Maybe<Location> getLastKnownLocation() {
        return Maybe.create(emitter ->
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(emitter::onSuccess)
                        .addOnFailureListener(e -> emitter.onComplete()));
    }

    /**
     * Create the location request by setting all the necessary parameters.
     */
    private void createLocationRequests() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATES_INTERVAL_MILLISECONDS);
        locationRequest.setFastestInterval(LOCATION_UPDATES_FASTEST_INTERVAL_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true);
        locationSettingsRequest = builder.build();
    }
}