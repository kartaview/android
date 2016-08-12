package com.telenav.osv.manager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
import com.telenav.osv.listener.AccuracyListener;
import com.telenav.osv.listener.LocationEventListener;
import com.telenav.osv.listener.SpeedChangedListener;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 10/7/2015.
 */
public class LocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "LocationManager";

    public static final float ACCURACY_GOOD = 15;

    public static final float ACCURACY_MEDIUM = 40;

    public static boolean ACCURATE = false;

    private final Context mContext;

    private final ApplicationPreferences appPrefs;

    private ShutterManager mShutterManager;

    private GoogleApiClient mGoogleApiClient;

    private Location mActualLocation;

    private LocationEventListener mLocationListener;

    private AccuracyListener accuracyListener;

    private float mCurrentAccuracy = 0;

    private float mCurrentBearing = 0.0f;

    private SpeedManager mSpeedManager;


    public LocationManager(Context context) {
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

    public static boolean checkGooglePlaySevices(final Activity activity) {
        final int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        switch (googlePlayServicesCheck) {
            case ConnectionResult.SUCCESS:
                return true;
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCheck, activity, 0);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        activity.finish();
                    }
                });
                dialog.show();
        }
        return false;
    }

    public void setShutterManager(ShutterManager shutterManager) {
        mShutterManager = shutterManager;
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
        setupGoogleApiClient();
    }

    /**
     * initialization of the google api client
     */
    protected void setupGoogleApiClient() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
            ((OSVApplication) mContext.getApplicationContext()).getOBDManager().addConnectionListener(mSpeedManager);
            Location loc = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (loc != null) {
                mActualLocation = loc;
                mSpeedManager.setPreviousLocation(loc);
                if (mLocationListener != null) {
                    mLocationListener.onLocationChanged(mActualLocation);
                }
            }
        } catch (Exception e){
            Log.d(TAG, "onConnected: error " + e);
            setupGoogleApiClient();
        }
    }

    protected LocationRequest createLocationRequest() {
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
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        if (mLocationListener != null) {
                            mLocationListener.onResolutionNeeded(status);
                        }
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
        Log.d(TAG, "onConnectionSuspended: connection suspended");
    }

    @Override
    public void onLocationChanged(Location location) {
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
            } else {
                Log.d(TAG, "onLocationChanged: no accuracy listener.");
            }
            if (mSpeedManager != null) {
                mSpeedManager.onLocationChanged(location);
            }
            mActualLocation = location;
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
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

    public void stopLocationUpdates() {
        if (mActualLocation != null) {
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) mActualLocation.getLatitude());
            appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) mActualLocation.getLongitude());
        }
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        if (mSpeedManager != null) {
            ((OSVApplication) mContext.getApplicationContext()).getOBDManager().removeConnectionListener(mSpeedManager);
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
