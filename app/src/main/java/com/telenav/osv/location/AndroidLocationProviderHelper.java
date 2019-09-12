package com.telenav.osv.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Kalman on 07/09/16.
 */
@SuppressWarnings("MissingPermission")
class AndroidLocationProviderHelper {

    private static final String TAG = AndroidLocationProviderHelper.class.getSimpleName();

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private static final int ACCURACY_THRESHOLD = 200;

    private Context context;

    private LocationManager locationMgrCell;

    private LocationManager locationMgrGps;

    private LocationListener locationListener;

    private Location currentLocation;

    private LocationListener listener;

    AndroidLocationProviderHelper(Context context) {
        this.context = context;

        initLocationListener();
        initLocationManager();
    }

    Location getLastKnownLocation() {

        Location lastKnownLocationCell = null;
        Location lastKnownLocationGPS = null;
        Location best = null;

        if (locationMgrCell != null) {
            lastKnownLocationCell = locationMgrCell.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (locationMgrGps != null) {
            lastKnownLocationGPS = locationMgrGps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (lastKnownLocationCell != null && isBetterLocation(lastKnownLocationCell, currentLocation)) {
            best = lastKnownLocationCell;
        }

        if (lastKnownLocationGPS != null && isBetterLocation(lastKnownLocationGPS, best)) {
            best = lastKnownLocationGPS;
        }
        return best;
    }

    void startLocationUpdates(LocationListener listener) {
        this.listener = listener;

        Location lastKnownLocationCell = null;
        Location lastKnownLocationGPS = null;

        if (locationMgrCell != null) {
            locationMgrCell.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

            lastKnownLocationCell = locationMgrCell.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (locationMgrGps != null) {
            locationMgrGps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            lastKnownLocationGPS = locationMgrGps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (lastKnownLocationCell != null && isBetterLocation(lastKnownLocationCell, currentLocation)) {
            updateLocation(lastKnownLocationCell);
        }

        if (lastKnownLocationGPS != null && isBetterLocation(lastKnownLocationGPS, currentLocation)) {
            updateLocation(lastKnownLocationGPS);
        }
    }

    void stopLocationUpdates() {
        if (locationMgrCell != null) {
            locationMgrCell.removeUpdates(locationListener);
        }

        if (locationMgrGps != null) {
            locationMgrGps.removeUpdates(locationListener);
        }
        listener = null;
    }

    private void updateLocation(Location location) {
        this.currentLocation = location;
        Log.d(TAG, "Location lat: " + location.getLatitude() + " long: " + location.getLongitude());
        if (listener != null) {
            listener.onLocationChanged(location);
        }
    }

    private void initLocationManager() {
        locationMgrCell = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationMgrCell != null && !locationMgrCell.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationMgrCell = null;
        }


        locationMgrGps = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationMgrGps != null && !locationMgrGps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationMgrGps = null;
        }

    }

    private void initLocationListener() {
        locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {

                if (isBetterLocation(location, AndroidLocationProviderHelper.this.currentLocation)) {
                    updateLocation(location);
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (listener != null) {
                    listener.onStatusChanged(provider, status, extras);
                }
            }

            public void onProviderEnabled(String provider) {
                if (listener != null) {
                    listener.onProviderEnabled(provider);
                }
            }

            public void onProviderDisabled(String provider) {
                if (listener != null) {
                    listener.onProviderDisabled(provider);
                }
            }
        };
    }

    /**
     * Determines whether one Location reading is better than the current Location fix.
     * @param location The new Location that you want to evaluate.
     * @param currentBestLocation The current Location fix, to which you want to compare the new one.
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new currentLocation is always better than no currentLocation
            return true;
        }

        // Check whether the new currentLocation fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current currentLocation, use the new currentLocation
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new currentLocation is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new currentLocation fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > ACCURACY_THRESHOLD;

        // Check if the old and new currentLocation are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine currentLocation quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}