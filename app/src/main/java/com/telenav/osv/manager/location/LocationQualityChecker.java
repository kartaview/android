package com.telenav.osv.manager.location;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.gps.AccuracyEvent;
import com.telenav.osv.utils.Log;

/**
 * Component handling quality changes in the gps location
 * Created by Kalman on 15/05/2017.
 */
class LocationQualityChecker {

  private static final String TAG = "LocationQualityChecker";

  private final LocationManager.LocationEventListener mListener;

  private int mCurrentAccuracyType = -1;

  private Handler mTimerHandler = new Handler(Looper.getMainLooper());

  private Runnable mTimeoutRunnable;

  LocationQualityChecker(LocationManager.LocationEventListener listener) {
    mListener = listener;
    mTimeoutRunnable = () -> {
      mListener.onLocationTimedOut();
      Log.d(TAG, "mTimeoutRunnable: no location received for 5 seconds");
      onAccuracyChanged(1000);
    };
  }

  private void onAccuracyChanged(float accuracy) {
    int accuracyType = getAccuracyType(accuracy);
    if (accuracyType != mCurrentAccuracyType) {
      Log.d(TAG, "onGpsAccuracyChanged: changed to " + mCurrentAccuracyType);
      mCurrentAccuracyType = accuracyType;
      EventBus.postSticky(new AccuracyEvent(accuracyType));
    }
    mListener.onGpsAccuracyChanged(mCurrentAccuracyType);
  }

  private int getAccuracyType(float accuracy) {
    if (accuracy <= LocationManager.ACCURACY_GOOD) {
      return LocationManager.ACCURACY_GOOD;
    } else if (accuracy <= LocationManager.ACCURACY_MEDIUM) {
      return LocationManager.ACCURACY_MEDIUM;
    } else {
      return LocationManager.ACCURACY_BAD;
    }
  }

  public void onLocationChanged(Location location, boolean shouldCenter) {
    onAccuracyChanged(location.getAccuracy());
    mListener.onLocationChanged(location, shouldCenter);
    //timeout detector
    Log.d(TAG, "onLocationChanged: ACC = " + location.getAccuracy());
    mTimerHandler.removeCallbacks(mTimeoutRunnable);
    mTimerHandler.postDelayed(mTimeoutRunnable, 5000);
  }
}


