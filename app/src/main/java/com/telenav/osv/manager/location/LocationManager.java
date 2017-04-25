package com.telenav.osv.manager.location;

import android.location.Location;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.telenav.osv.application.OSVApplication;

/**
 * Created by Kalman on 07/09/16.
 */

public abstract class LocationManager {

    public static boolean ACCURATE = false;

    public static LocationManager get(OSVApplication osvApplication, LocationEventListener listener) {
        if (isGooglePlaySevices(osvApplication)) {
            return new GoogleLocationManager(osvApplication, listener);
        }
        return new AndroidLocationManager(osvApplication, listener);
    }

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

    protected abstract boolean startLocationUpdates();

    protected abstract void stopLocationUpdates();

    public abstract float getAccuracy();

    public abstract boolean hasPosition();

    public interface LocationEventListener {
        void onLocationChanged(Location location);
    }
}
