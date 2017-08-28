package com.telenav.osv.manager.location;

import android.location.Location;
import com.telenav.osv.BuildConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * location quality checker tests
 * Created by Kalman on 15/05/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class LocationQualityCheckerTest {

  @Before
  public void setup() {
    ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
  }

  @Test
  public void basicUseCase() throws Exception {
    Location loc = new Location("test");
    loc.setLatitude(47);
    loc.setLongitude(23);
    loc.setAccuracy(10);
    Location loc2 = new Location(loc);
    loc2.setAccuracy(30);
    Location loc3 = new Location(loc);
    loc3.setAccuracy(60);
    Location loc4 = new Location(loc);
    loc4.setAccuracy(15);
    Location loc5 = new Location(loc);
    loc5.setAccuracy(60);
    final long mLastPosTime = System.currentTimeMillis();
    LocationQualityChecker checker = new LocationQualityChecker(new LocationManager.LocationEventListener() {

      @Override
      public void onLocationChanged(Location location, boolean shouldCenter) {
        Assert.assertTrue(location.getAccuracy() == 10);
      }

      @Override
      public void onLocationTimedOut() {
        Assert.assertTrue(System.currentTimeMillis() - mLastPosTime > 5000);//not called
      }

      @Override
      public void onGpsAccuracyChanged(int type) {
        Assert.assertTrue(type == LocationManager.ACCURACY_GOOD);
      }
    });
    checker.onLocationChanged(loc, false);
    //no fucking timeout, as async not supported by robolectric
  }
}