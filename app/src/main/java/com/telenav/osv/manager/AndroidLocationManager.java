package com.telenav.osv.manager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.listener.AccuracyListener;
import com.telenav.osv.listener.LocationEventListener;
import com.telenav.osv.listener.SpeedChangedListener;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 07/09/16.
 */
class AndroidLocationManager extends LocationManager implements LocationListener, LocationDataProvider.LocationDataListener {

    private static final String TAG = "AndroidLocationManager";

    private final Context mContext;

    private final ApplicationPreferences appPrefs;

    private Location mActualLocation;

    private LocationEventListener mLocationListener;

    private AccuracyListener accuracyListener;

    private float mCurrentAccuracy = 0;

    private float mCurrentBearing = 0.0f;

    private SpeedManager mSpeedManager;

    private boolean shouldStartUpdates = false;

    private boolean mConnected = false;

    private LocationDataProvider mLocationProvider;


    AndroidLocationManager(Context context) {
        mContext = context;
        appPrefs = ((OSVApplication) context.getApplicationContext()).getAppPrefs();
//        float latitude = appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT, (float) 0);
//        float longitude = appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON, (float) 0);
//        mActualLocation = new Location("");
//        mActualLocation.setLongitude(longitude);
//        mActualLocation.setLatitude(latitude);
//        mPreviousLocation = new Location("");
//        mPreviousLocation.setLongitude(longitude);
//        mPreviousLocation.setLatitude(latitude);

        mSpeedManager = new SpeedManager(mContext);
    }

    public boolean checkGooglePlaySevices(final Activity activity) {
        final int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        switch (googlePlayServicesCheck) {
            case ConnectionResult.SUCCESS:
                return true;
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_MISSING:
                return false;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCheck, activity, 0);
                dialog.show();
        }
        return false;
    }

    public void setShutterManager(ShutterManager shutterManager) {
        mSpeedManager.setShutterManager(shutterManager);
    }

    public Location getActualLocation() {
        return mActualLocation;
    }

    public Location getPreviousLocation() {
        return mSpeedManager.getPreviousLocation();
    }

    public void setPreviousLocation(Location previousLocation) {
        mSpeedManager.setPreviousLocation(previousLocation);
    }

    public void setLocationListener(LocationEventListener listener) {
        mLocationListener = listener;
    }

    /**
     * initialization of the location api service
     */
    public void connect() {
        mLocationProvider = new LocationDataProvider(mContext, true, true, 1000, 3);
        onConnected();
    }

    public void onConnected() {
        mConnected = true;
        try {
            ((OSVApplication) mContext.getApplicationContext()).getOBDManager().addConnectionListener(mSpeedManager);
            Location loc = mLocationProvider.getLastKnownLocation();
            if (loc != null) {
                mActualLocation = loc;
                mSpeedManager.setPreviousLocation(loc);
                if (mLocationListener != null) {
                    mLocationListener.onLocationChanged(mActualLocation);
                }
            }
            if (shouldStartUpdates) {
                shouldStartUpdates = false;
                startLocationUpdates();
            }
        } catch (Exception e) {
            mConnected = false;
            Log.d(TAG, "onConnected: error " + e);
            connect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        if (location != null) {
            if (mActualLocation == null) {
                mActualLocation = location;
            }
            if (mSpeedManager.getPreviousLocation() == null) {
                mSpeedManager.setPreviousLocation(location);
            }
            if (mLocationListener != null) {
                mLocationListener.onLocationChanged(location);
            }
            mCurrentBearing = location.getBearing();
            if (accuracyListener != null) {
                accuracyListener.onAccuracyChanged(location.getAccuracy());
            }
            if (mSpeedManager != null) {
                mSpeedManager.onLocationChanged(location);
            }
            mActualLocation = location;
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

    public void disconnect() {
        Log.d(TAG, "disconnect: disconnecting location api");
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        mLocationProvider.stopLocationUpdates();
        mLocationProvider = null;

        if (mSpeedManager != null) {
            ((OSVApplication) mContext.getApplicationContext()).getOBDManager().removeConnectionListener(mSpeedManager);
        }
        mConnected = false;
    }

    public boolean startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: successfull: " + (mLocationProvider != null && mConnected));
        if (mLocationProvider != null && mConnected) {
            try {
                mLocationProvider.startLocationUpdates(this);
            } catch (Exception e) {
                return false;
            }
            return true;
        } else {
            shouldStartUpdates = true;
            connect();
        }
        return false;
    }

    public void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates: successfull: " + (mLocationProvider != null && mConnected));
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        if (mLocationProvider != null && mConnected) {
            mLocationProvider.stopLocationUpdates();
        }
    }

    public void setSpeedChangedListener(SpeedChangedListener speedChangedListener) {
        this.mSpeedManager.setSpeedChangedListener(speedChangedListener);
    }

    public void setAccuracyListener(AccuracyListener accuracyListener) {
        this.accuracyListener = accuracyListener;
        if (accuracyListener != null) {
            accuracyListener.onAccuracyChanged(mCurrentAccuracy);
        }
    }

    public boolean isLocationEnabled() {
        boolean locationServiceBoolean = false;
        android.location.LocationManager locationManager = (android.location.LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        boolean gpsIsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        boolean networkIsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);

        if (networkIsEnabled && gpsIsEnabled) {
            locationServiceBoolean = true;

        } else if (!networkIsEnabled && gpsIsEnabled) {
            locationServiceBoolean = true;

        } else if (networkIsEnabled) {
            locationServiceBoolean = true;
        }
        return locationServiceBoolean;
    }

    public boolean isGPSEnabled() {
        android.location.LocationManager locationManager = (android.location.LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    public float getAccuracy() {
        return mCurrentAccuracy;
    }

    public boolean hasPosition() {
        return mActualLocation != null && mSpeedManager.getPreviousLocation() != null;
    }

    public float getBearing() {
        return mCurrentBearing;
    }
}
