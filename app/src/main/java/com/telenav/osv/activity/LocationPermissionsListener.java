package com.telenav.osv.activity;

/**
 * Interface used to notify the listener when the location permission is granted.
 */
public interface LocationPermissionsListener {

    /**
     * The method is called when the location permission is granted.
     */
    void onLocationPermissionGranted();

    /**
     * The method is called when the location permission is denied.
     */
    void onLocationPermissionDenied();
}