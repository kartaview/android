package com.telenav.osv.recorder.shutter.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.location.AccuracyType;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import androidx.annotation.Nullable;

/**
 * shutter logic for gps based recording
 * Created by Kalman on 17/05/2017.
 */
public class GpsShutterLogic extends ShutterLogic {

    private static final String TAG = "GpsShutterLogic";

    private static final float MPS_TO_KMPH = 3.6f;

    private Location previousTakenPhotoLocation;

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        Log.d(TAG, "onLocationChanged");
        if (isCurrentLocationValid()) {
            if (previousTakenPhotoLocation == null) {
                previousTakenPhotoLocation = location;
            }
            if (!currentCachedLocation.hasSpeed()) {
                Log.d(TAG, "onLocationChanged: mActualLocation has no speed");
                double dist = previousTakenPhotoLocation.distanceTo(currentCachedLocation);
                if (Math.abs(dist) > 0) {
                    float time = (previousTakenPhotoLocation.getTime() - currentCachedLocation.getTime()) / 1000f;
                    location.setSpeed((float) (dist / time));
                    Log.d(TAG, "onLocationChanged: mActualLocation has no speed, calculated speed: " + dist + "m /" + time + "s");
                }
            }
            recalculateSpeedCategory(currentCachedLocation.getSpeed() * MPS_TO_KMPH);
            if (currentCachedLocation.hasAccuracy() && currentCachedLocation.getAccuracy() < AccuracyType.ACCURACY_MEDIUM) {
                double dist = ComputingDistance
                        .distanceBetween(previousTakenPhotoLocation.getLongitude(), previousTakenPhotoLocation.getLatitude(), currentCachedLocation.getLongitude(), currentCachedLocation.getLatitude());
                Log.d(TAG, "onLocationChanged: location has speed: " + dist);

                if (dist >= mSpeedCategory && mShutterListener != null) {
                    mShutterListener.requestTakeSnapshot((float) dist, currentCachedLocation);
                    previousTakenPhotoLocation = currentCachedLocation;
                    Log.d(TAG, "onLocationChanged: image taken ");
                }
            } else {
                Log.d(TAG, "onLocationChanged: accuracy worse than 40 m");
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        previousTakenPhotoLocation = null;
    }

    @Override
    public void onSpeedChanged(SpeedData speedData) {

    }

    @Override
    int getPriority() {
        return PRIORITY_GPS;
    }
}
