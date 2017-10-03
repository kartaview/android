package com.telenav.osv.manager.location;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.telenav.osv.command.GpsCommand;
import com.telenav.osv.data.MapPreferences;

/**
 * abstract location manager
 * Created by Kalman on 07/09/16.
 */
public abstract class LocationManager implements LocationListener, com.google.android.gms.location.LocationListener {

    public static final int ACCURACY_GOOD = 15;

    public static final int ACCURACY_MEDIUM = 40;

    public static final int ACCURACY_BAD = 41;

    final Context mContext;

    final MapPreferences appPrefs;

    private final LocationQualityChecker mLocationQualityChecker;

    Location mActualLocation;

    private boolean singlePositionRequested;

    LocationManager(Application context, MapPreferences prefs, LocationQualityChecker qualityChecker) {
        mContext = context;
        appPrefs = prefs;
        mLocationQualityChecker = qualityChecker;
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

    public void setListener(LocationEventListener listener) {
        mLocationQualityChecker.setListener(listener);
    }

    public abstract void connect();

    public abstract void disconnect();

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

    public interface LocationEventListener {

        void onLocationChanged(Location location, boolean shouldCenter);

        void onLocationTimedOut();

        void onGpsAccuracyChanged(int type);
    }

    abstract void startLocationUpdates();

    abstract void stopLocationUpdates();
}
