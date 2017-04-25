package com.telenav.osv.manager.location;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.LocationPermissionEvent;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 10/7/2015.
 */
class GoogleLocationManager extends LocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "GoogleLocationManager";

    private final Context mContext;

    private final ApplicationPreferences appPrefs;

    private GoogleApiClient mGoogleApiClient;

    private Location mActualLocation;

    private float mCurrentAccuracy = 0;

    private LocationEventListener mListener;

    private boolean shouldStartUpdates = false;

    private boolean mConnected = false;


    GoogleLocationManager(Context context, LocationEventListener listener) {
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
     * initialization of the google api client
     */
    public void connect() {
        if (mGoogleApiClient != null && mConnected) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        Log.d(TAG, "connect: connecting google api ");
    }

    @Override
    public void onConnected(Bundle bundle) {
        mConnected = true;
        try {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (loc != null) {
                mActualLocation = loc;
                mListener.onLocationChanged(mActualLocation);
            }
            if (shouldStartUpdates) {
                shouldStartUpdates = false;
                startLocationUpdates();
            }
            EventBus.register(this);
        } catch (SecurityException e) {
            mConnected = false;
            Log.w(TAG, "onConnected: error " + e);
            connect();
        } catch (Exception e) {
            mConnected = false;
            Log.w(TAG, "onConnected: error " + e);
            connect();
        }
    }

    private LocationRequest createLocationRequest() {
        final LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setSmallestDisplacement(3);
        mLocationRequest.setMaxWaitTime(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
                .setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        EventBus.clear(LocationPermissionEvent.class);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        EventBus.postSticky(new LocationPermissionEvent(status));
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
        builder.build();
        return mLocationRequest;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended: connection suspended");
        mConnected = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (mListener != null) {
                mListener.onLocationChanged(location);
            }
            mActualLocation = location;
        }

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        mConnected = false;
        Log.w(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
//        final int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
//
//        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCheck, mContext, 0);
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialogInterface) {
//
//            }
//        });
//        dialog.show();
    }

    public void disconnect() {
        try {
            Log.d(TAG, "disconnect: disconnecting google api");
            if (mActualLocation != null) {
                appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
                appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
            }
            if (mGoogleApiClient != null && mConnected) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
            mConnected = false;
        } catch (IllegalStateException e) {
            Log.w(TAG, "disconnect: " + Log.getStackTraceString(e));
        }
        EventBus.unregister(this);
    }

    public boolean startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates: successfull: " + (mGoogleApiClient != null && mConnected));
        if (mGoogleApiClient != null && mConnected) {
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
            } catch (SecurityException e) {
                return false;
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
        Log.d(TAG, "stopLocationUpdates: successfull: " + (mGoogleApiClient != null && mConnected));
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        if (mGoogleApiClient != null && mConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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
