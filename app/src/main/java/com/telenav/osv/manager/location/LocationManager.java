package com.telenav.osv.manager.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.command.GpsCommand;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * abstract location manager
 * Created by Kalman on 07/09/16.
 */
public abstract class LocationManager implements LocationListener, com.google.android.gms.location.LocationListener {

  public static final int ACCURACY_GOOD = 15;

  public static final int ACCURACY_MEDIUM = 40;

  public static final int ACCURACY_BAD = 41;

  final Context mContext;

  final ApplicationPreferences appPrefs;

  private final LocationQualityChecker mLocationQualityChecker;

  Location mActualLocation;

  private boolean singlePositionRequested;

  LocationManager(Context context, LocationEventListener listener) {
    mContext = context;
    appPrefs = ((OSVApplication) context.getApplicationContext()).getAppPrefs();
    mLocationQualityChecker = new LocationQualityChecker(listener);
  }

  public static LocationManager get(OSVApplication osvApplication, LocationEventListener listener) {
    if (isGooglePlaySevices(osvApplication)) {
      return new GoogleLocationManager(osvApplication, listener);
    }
    return new AndroidLocationManager(osvApplication, listener);
  }

  @SuppressWarnings("deprecation")
  private static boolean isGooglePlaySevices(final OSVApplication app) {
    final int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(app);
    switch (googlePlayServicesCheck) {
      case ConnectionResult.SUCCESS:
      case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
        return true;
    }
    return false;
  }

  public abstract void connect();

  public abstract void disconnect();

  abstract void startLocationUpdates();

  abstract void stopLocationUpdates();

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onGpsCommand(GpsCommand event) {
    if (event.start) {
      if (event.singlePosition) {
        singlePositionRequested = true;
      }
      startLocationUpdates();
    } else {
      stopLocationUpdates();
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location != null) {
      mActualLocation = location;
      boolean shouldCenter = false;
      if (singlePositionRequested) {
        shouldCenter = true;
        singlePositionRequested = false;
        stopLocationUpdates();
      }
      mLocationQualityChecker.onLocationChanged(location, shouldCenter);
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
  }

  @Override
  public void onProviderEnabled(String provider) {
  }

  @Override
  public void onProviderDisabled(String provider) {
  }

  public interface LocationEventListener {

    void onLocationChanged(Location location, boolean shouldCenter);

    void onLocationTimedOut();

    void onGpsAccuracyChanged(int type);
  }
}
