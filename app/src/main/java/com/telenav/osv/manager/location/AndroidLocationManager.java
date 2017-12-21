package com.telenav.osv.manager.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.utils.Log;

/**
 * internal android location manager
 * Created by Kalman on 07/09/16.
 */
class AndroidLocationManager extends LocationManager implements LocationListener, LocationDataProvider.LocationDataListener {

    private static final String TAG = "AndroidLocationManager";

    private boolean shouldStartUpdates = false;

    private boolean mConnected = false;

    private LocationDataProvider mLocationProvider;

    AndroidLocationManager(Context context, LocationEventListener listener) {
        super(context, listener);
    }

    /**
     * initialization of the location api service
     */
    public void connect() {
        EventBus.register(this);
        mLocationProvider = new LocationDataProvider(mContext, true, true, 0, 0);
        onConnected();
    }

    public void disconnect() {
        Log.d(TAG, "disconnect: disconnecting location api");
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        mLocationProvider.stopLocationUpdates();
        mLocationProvider = null;
        mConnected = false;
        EventBus.unregister(this);
    }

    void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: successfull: " + (mLocationProvider != null && mConnected));
        if (mLocationProvider != null && mConnected) {
            try {
                mLocationProvider.startLocationUpdates(this);
            } catch (Exception e) {
                Log.d(TAG, "startLocationUpdates: " + Log.getStackTraceString(e));
            }
        } else {
            shouldStartUpdates = true;
            connect();
        }
    }

    void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates: successfull: " + (mLocationProvider != null && mConnected));
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        if (mLocationProvider != null && mConnected) {
            mLocationProvider.stopLocationUpdates();
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void onConnected() {
        mConnected = true;
        try {
            Location loc = mLocationProvider.getLastKnownLocation();
            if (loc != null) {
                mActualLocation = loc;
                onLocationChanged(mActualLocation);
            }
            if (shouldStartUpdates) {
                shouldStartUpdates = false;
                startLocationUpdates();
            }
        } catch (SecurityException e) {
            mConnected = false;
            Log.w(TAG, "onConnected: error " + e);
            //            connect();
        } catch (Exception e) {
            mConnected = false;
            Log.w(TAG, "onConnected: error " + e);
            connect();
        }
    }

    //    public boolean isLocationEnabled() {
    //        boolean locationServiceBoolean = false;
    //        android.location.LocationManager locationManager = (android.location.LocationManager) mContext.getSystemService(Context
    // .LOCATION_SERVICE);
    //        boolean gpsIsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    //        boolean networkIsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    //
    //        if (networkIsEnabled && gpsIsEnabled) {
    //            locationServiceBoolean = true;
    //
    //        } else if (!networkIsEnabled && gpsIsEnabled) {
    //            locationServiceBoolean = true;
    //
    //        } else if (networkIsEnabled) {
    //            locationServiceBoolean = true;
    //        }
    //        return locationServiceBoolean;
    //    }

    //    public boolean isGPSEnabled() {
    //        android.location.LocationManager locationManager = (android.location.LocationManager) mContext.getSystemService(Context
    // .LOCATION_SERVICE);
    //        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    //    }
}
