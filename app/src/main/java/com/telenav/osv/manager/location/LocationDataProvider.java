package com.telenav.osv.manager.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;

/**
 * Created by Kalman on 07/09/16.
 */
@SuppressWarnings("MissingPermission")
class LocationDataProvider {

  private static final String TAG = "LocationDataProvider";

  private static final int TOO_OLD_LOCATION_DELTA = 1000 * 60 * 2;

  //config
  private final boolean mUseGPSLocation;

  private final boolean mUseCellLocation;

  private final long mMinTime;

  private final float mMinDistance;

  private Context mContext;

  private LocationManager mLocationMgrCell;

  private LocationManager mLocationMgrGPS;

  private LocationListener mLocationListener;

  private Location mLocation;

  private LocationDataListener mListener;

  LocationDataProvider(Context context, boolean useGPSLocation, boolean useCellLocation, long minTime, float minDistance) {
    this.mContext = context;
    this.mUseGPSLocation = useGPSLocation;
    this.mUseCellLocation = useCellLocation;
    this.mMinTime = minTime;
    this.mMinDistance = minDistance;

    initLocationListener();
    initLocationManager();
  }

  Location getLastKnownLocation() {

    Location lastKnownLocationCell = null;
    Location lastKnownLocationGPS = null;
    Location best = null;

    if (mLocationMgrCell != null) {
      lastKnownLocationCell = mLocationMgrCell.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    if (mLocationMgrGPS != null) {
      lastKnownLocationGPS = mLocationMgrGPS.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    if (lastKnownLocationCell != null && isBetterLocation(lastKnownLocationCell, mLocation)) {
      best = lastKnownLocationCell;
    }

    if (lastKnownLocationGPS != null && isBetterLocation(lastKnownLocationGPS, best)) {
      best = lastKnownLocationGPS;
    }
    if (best == null) {
      ApplicationPreferences appPrefs = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs();
      final double lat = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT);
      final double lon = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON);
      if (lat != 0 && lon != 0) {
        best = new Location("Saved");
        best.setLatitude(lat);
        best.setLatitude(lon);
      }
    }
    return best;
  }

  void startLocationUpdates(LocationDataListener listener) {
    this.mListener = listener;

    Location lastKnownLocationCell = null;
    Location lastKnownLocationGPS = null;

    if (mLocationMgrCell != null) {
      mLocationMgrCell.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, mLocationListener);

      lastKnownLocationCell = mLocationMgrCell.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }

    if (mLocationMgrGPS != null) {
      mLocationMgrGPS.requestLocationUpdates(LocationManager.GPS_PROVIDER, mMinTime, mMinDistance, mLocationListener);

      lastKnownLocationGPS = mLocationMgrGPS.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    if (lastKnownLocationCell != null && isBetterLocation(lastKnownLocationCell, mLocation)) {
      updateLocation(lastKnownLocationCell, LocationType.CELL, false);
    }

    if (lastKnownLocationGPS != null && isBetterLocation(lastKnownLocationGPS, mLocation)) {
      updateLocation(lastKnownLocationGPS, LocationType.GPS, false);
    }
  }

  void stopLocationUpdates() {
    if (mLocationMgrCell != null) {
      mLocationMgrCell.removeUpdates(mLocationListener);
    }

    if (mLocationMgrGPS != null) {
      mLocationMgrGPS.removeUpdates(mLocationListener);
    }
    mListener = null;
  }

  private void updateLocation(Location location, LocationType type, boolean isFresh) {
    mLocation = location;
    if (mListener != null) {
      mListener.onLocationChanged(location);
    }
  }

  private void initLocationManager() {
    if (mUseCellLocation) {
      mLocationMgrCell = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
      if (!mLocationMgrCell.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        mLocationMgrCell = null;
      }
    }

    if (mUseGPSLocation) {
      mLocationMgrGPS = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
      if (!mLocationMgrGPS.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        mLocationMgrGPS = null;
      }
    }
  }

  private void initLocationListener() {
    mLocationListener = new LocationListener() {

      public void onLocationChanged(Location location) {

        if (isBetterLocation(location, mLocation)) {
          updateLocation(location, providerToLocationType(location.getProvider()), true);
        }
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {
        if (mListener != null) {
          mListener.onStatusChanged(provider, status, extras);
        }
      }

      public void onProviderEnabled(String provider) {
        if (mListener != null) {
          mListener.onProviderEnabled(provider);
        }
      }

      public void onProviderDisabled(String provider) {
        if (mListener != null) {
          mListener.onProviderDisabled(provider);
        }
      }
    };
  }

  private LocationType providerToLocationType(String provider) {
    switch (provider) {
      case "gps":
        return LocationType.GPS;
      case "network":
        return LocationType.CELL;
      default:
        Log.w(TAG, "providerToLocationType Unknown Provider: " + provider);
        return LocationType.UNKNOWN;
    }
  }

  private String locationToString(Location l) {
    return "PROVIDER: " + l.getProvider() + " - LAT: " + l.getLatitude() + " - LON: " + l.getLongitude() + " - BEARING: " + l.getBearing() +
        " - ALT: " + l.getAltitude() + " - SPEED: " + l.getSpeed() + " - TIME: " + l.getTime() + " - ACC: " + l.getAccuracy();
  }

  private boolean isBetterLocation(Location location, Location currentBestLocation) {
    if (currentBestLocation == null) {
      // A new location is always better than no location
      return true;
    }

    // Check whether the new location fix is newer or older
    long timeDelta = location.getTime() - currentBestLocation.getTime();
    boolean isSignificantlyNewer = timeDelta > TOO_OLD_LOCATION_DELTA;
    boolean isSignificantlyOlder = timeDelta < -TOO_OLD_LOCATION_DELTA;
    boolean isNewer = timeDelta > 0;

    // If it's been more than two minutes since the current location, use the new location
    // because the user has likely moved
    if (isSignificantlyNewer) {
      return true;
      // If the new location is more than two minutes older, it must be worse
    } else if (isSignificantlyOlder) {
      return false;
    }

    // Check whether the new location fix is more or less accurate
    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
    boolean isLessAccurate = accuracyDelta > 0;
    boolean isMoreAccurate = accuracyDelta < 0;
    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

    // Check if the old and new location are from the same provider
    boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

    // Determine location quality using a combination of timeliness and accuracy
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

  private enum LocationType {
    GPS,
    CELL,
    UNKNOWN
  }

  interface LocationDataListener {

    void onLocationChanged(Location location);

    void onStatusChanged(String provider, int status, Bundle extras);

    void onProviderEnabled(String provider);

    void onProviderDisabled(String provider);
  }
}