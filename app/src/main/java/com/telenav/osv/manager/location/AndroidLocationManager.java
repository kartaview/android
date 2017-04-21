package com.telenav.osv.manager.location;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 07/09/16.
 */
class AndroidLocationManager extends LocationManager implements LocationListener, LocationDataProvider.LocationDataListener {

    private static final String TAG = "AndroidLocationManager";

    private final Context mContext;

    private final ApplicationPreferences appPrefs;

    private Location mActualLocation;

    private float mCurrentAccuracy = 0;

    private LocationEventListener mListener;

    private boolean shouldStartUpdates = false;

    private boolean mConnected = false;

    private LocationDataProvider mLocationProvider;


    AndroidLocationManager(Context context, LocationEventListener listener) {
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

        mListener = listener;
    }

    /**
     * initialization of the location api service
     */
    public void connect() {
        EventBus.register(this);
        mLocationProvider = new LocationDataProvider(mContext, true, true, 1000, 3);
        onConnected();
    }

    public void onConnected() {
        mConnected = true;
        try {
            Location loc = mLocationProvider.getLastKnownLocation();
            if (loc != null) {
                mActualLocation = loc;
                mListener.onLocationChanged(mActualLocation);
            }
            if (shouldStartUpdates) {
                shouldStartUpdates = false;
                startLocationUpdates();
            }
        } catch (Exception e) {
            mConnected = false;
            Log.w(TAG, "onConnected: error " + e);
            connect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        if (location != null) {
            if (mListener != null) {
                mListener.onLocationChanged(location);
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
        mConnected = false;
        EventBus.unregister(this);
    }

    protected boolean startLocationUpdates() {
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

    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates: successfull: " + (mLocationProvider != null && mConnected));
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        if (mLocationProvider != null && mConnected) {
            mLocationProvider.stopLocationUpdates();
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
        return mActualLocation != null;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onGpsCommand(GpsCommand event) {
        if (event.start) {
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }
    }
}
