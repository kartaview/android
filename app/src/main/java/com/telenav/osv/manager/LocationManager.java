package com.telenav.osv.manager;

import android.app.Activity;
import android.location.Location;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.listener.AccuracyListener;
import com.telenav.osv.listener.LocationEventListener;
import com.telenav.osv.listener.SpeedChangedListener;

/**
 * Created by Kalman on 07/09/16.
 */

public abstract class LocationManager {

    public static final float ACCURACY_GOOD = 15;

    public static final float ACCURACY_MEDIUM = 40;

    public static boolean ACCURATE = false;

    public static LocationManager get(OSVApplication osvApplication) {
        if (isGooglePlaySevices(osvApplication)) {
            return new GoogleLocationManager(osvApplication);
        }
        return new AndroidLocationManager(osvApplication);
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

    public abstract boolean checkGooglePlaySevices(final Activity activity);

    public abstract void setShutterManager(ShutterManager shutterManager);

    public abstract Location getActualLocation();

    public abstract Location getPreviousLocation();

    public abstract void setPreviousLocation(Location previousLocation);

    public abstract void setLocationListener(LocationEventListener listener);

    public abstract void connect();

    public abstract void disconnect();

    public abstract boolean startLocationUpdates();

    public abstract void stopLocationUpdates();

    public abstract void setSpeedChangedListener(SpeedChangedListener speedChangedListener);

    public abstract void setAccuracyListener(AccuracyListener accuracyListener);

    public abstract boolean isLocationEnabled();

    public abstract boolean isGPSEnabled();

    public abstract float getAccuracy();

    public abstract boolean hasPosition();

    public abstract float getBearing();
}
