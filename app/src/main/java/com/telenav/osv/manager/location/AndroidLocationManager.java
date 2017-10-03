package com.telenav.osv.manager.location;

import android.app.Application;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.telenav.osv.data.MapPreferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.utils.Log;
import javax.inject.Inject;

/**
 * internal android location manager
 * Created by Kalman on 07/09/16.
 */
public class AndroidLocationManager extends LocationManager implements LocationListener, LocationDataProvider.LocationDataListener {

  private static final String TAG = "AndroidLocationManager";

  private boolean shouldStartUpdates = false;

  private boolean mConnected = false;

  private LocationDataProvider mLocationProvider;

  @Inject
  public AndroidLocationManager(Application context, MapPreferences prefs, LocationQualityChecker qualityChecker) {
    super(context, prefs, qualityChecker);
  }

  /**
   * initialization of the location api service
   */
  public void connect() {
    EventBus.register(this);
    mLocationProvider = new LocationDataProvider(mContext, appPrefs, true, true, 0, 0);
    onConnected();
  }

  public void disconnect() {
    Log.d(TAG, "disconnect: disconnecting location api");
    if (mActualLocation != null) {
      appPrefs.saveLastLocation(mActualLocation);
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
      appPrefs.saveLastLocation(mActualLocation);
    }
    if (mLocationProvider != null && mConnected) {
      mLocationProvider.stopLocationUpdates();
    }
  }

  @Override
  public void onStatusChanged(String s, int i, Bundle bundle) {
    //no action
  }

  @Override
  public void onProviderEnabled(String s) {
    //no action
  }

  @Override
  public void onProviderDisabled(String s) {
    //no action
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
    } catch (Exception e) {
      mConnected = false;
      Log.w(TAG, "onConnected: error " + e);
      connect();
    }
  }
}
