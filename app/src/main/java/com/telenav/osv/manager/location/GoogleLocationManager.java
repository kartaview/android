package com.telenav.osv.manager.location;

import android.app.Application;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.telenav.osv.data.MapPreferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.LocationPermissionEvent;
import com.telenav.osv.utils.Log;
import javax.inject.Inject;

/**
 * Location manager from google
 * Created by Kalman on 10/7/2015.
 */
public class GoogleLocationManager extends LocationManager
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private static final String TAG = "GoogleLocationManager";

  private GoogleApiClient mGoogleApiClient;

  private boolean shouldStartUpdates = false;

  private boolean mConnected = false;

  @Inject
  public GoogleLocationManager(Application context, MapPreferences prefs, LocationQualityChecker qualityChecker) {
    super(context, prefs, qualityChecker);
  }

  /**
   * initialization of the google api client
   */
  public void connect() {
    if (mGoogleApiClient != null && mConnected) {
      return;
    }
    mGoogleApiClient =
        new GoogleApiClient.Builder(mContext).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API)
            .build();
    mGoogleApiClient.connect();
    Log.d(TAG, "connect: connecting google api ");
  }

  public void disconnect() {
    try {
      Log.d(TAG, "disconnect: disconnecting google api");
      if (mActualLocation != null) {
        appPrefs.saveLastLocation(mActualLocation);
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

  void startLocationUpdates() {
    if (mGoogleApiClient != null && mConnected) {
      try {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
        Log.d(TAG, "startLocationUpdates: successfull: " + (mGoogleApiClient != null && mConnected));
      } catch (SecurityException e) {
        Log.d(TAG, "startLocationUpdates: " + Log.getStackTraceString(e));
      } catch (IllegalStateException e) {
        shouldStartUpdates = true;
        connect();
        Log.d(TAG, "startLocationUpdates: " + e.getLocalizedMessage());
      } catch (Exception e) {
        Log.d(TAG, "startLocationUpdates: " + Log.getStackTraceString(e));
      }
    } else {
      shouldStartUpdates = true;
      connect();
    }
  }

  void stopLocationUpdates() {
    Log.d(TAG, "stopLocationUpdates: successfull: " + (mGoogleApiClient != null && mConnected));
    if (mActualLocation != null) {
      appPrefs.saveLastLocation(mActualLocation);
    }
    if (mGoogleApiClient != null && mConnected) {
      try {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
      } catch (IllegalStateException ignored) {
        Log.d(TAG, Log.getStackTraceString(ignored));
      }
    }
  }

  @Override
  public void onConnected(Bundle bundle) {
    mConnected = true;
    try {
      Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
      if (loc != null) {
        mActualLocation = loc;
        onLocationChanged(mActualLocation);
      }
      if (shouldStartUpdates) {
        shouldStartUpdates = false;
        startLocationUpdates();
      }
      EventBus.register(this);
    } catch (SecurityException e) {
      mConnected = false;
      Log.w(TAG, "onConnected: error " + e);
    } catch (Exception e) {
      mConnected = false;
      Log.w(TAG, "onConnected: error " + e);
      connect();
    }
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.w(TAG, "onConnectionSuspended: connection suspended");
    mConnected = false;
  }

  private LocationRequest createLocationRequest() {
    final LocationRequest mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(1000);
    mLocationRequest.setFastestInterval(500);
    mLocationRequest.setSmallestDisplacement(0);
    mLocationRequest.setMaxWaitTime(0);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    LocationSettingsRequest.Builder builder =
        new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).setAlwaysShow(true);

    PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
    result.setResultCallback(result1 -> {
      final Status status = result1.getStatus();
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
        default:
          // Location settings are not satisfied. However, we have no way to fix the
          // settings so we won't show the dialog.
          break;
      }
    });
    builder.build();
    return mLocationRequest;
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    mConnected = false;
    Log.w(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
  }
}
