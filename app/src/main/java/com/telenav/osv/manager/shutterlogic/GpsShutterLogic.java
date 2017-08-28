package com.telenav.osv.manager.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.manager.location.LocationManager;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;

/**
 * shutter logic for gps based recording
 * Created by Kalman on 17/05/2017.
 */
public class GpsShutterLogic extends ShutterLogic {

  private static final String TAG = "GpsShutterLogic";

  private static final float MPS_TO_KMPH = 3.6f;

  @Override
  public void onLocationChanged(Location reference, Location location) {
    if (!location.hasSpeed()) {
      Log.d(TAG, "onLocationChanged: mActualLocation has no speed");
      double dist = reference.distanceTo(location);
      if (Math.abs(dist) > 0) {
        float time = (reference.getTime() - location.getTime()) / 1000f;
        location.setSpeed((float) (dist / time));
        Log.d(TAG, "onLocationChanged: mActualLocation has no speed, calculated speed: " + dist + "m /" + time + "s");
      }
    }
    recalculateSpeedCategory(location.getSpeed() * MPS_TO_KMPH);
    if (location.hasAccuracy() && location.getAccuracy() < LocationManager.ACCURACY_MEDIUM) {
      double dist = ComputingDistance
          .distanceBetween(reference.getLongitude(), reference.getLatitude(), location.getLongitude(), location.getLatitude());
      if (dist >= mSpeedCategory.getDistance()) {
        mShutterListener.takeSnapshot((float) dist);
        Log.d(TAG, "onLocationChanged: image taken");
      }
    } else {
      Log.d(TAG, "onLocationChanged: accuracy worse than 40 m");
    }
  }

  @Override
  public void onSpeedChanged(SpeedData speedData) {

  }

  @Override
  int getPriority() {
    return PRIORITY_GPS;
  }
}
