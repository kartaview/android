package com.telenav.osv.ui.rules;

import android.support.test.rule.ActivityTestRule;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 03/04/2017.
 */

public class SplashActivityTestRule extends ActivityTestRule<SplashActivity> {

  private static final String TAG = "SplashActivityTestRule";

  public SplashActivityTestRule(Class<SplashActivity> activityClass) {
    super(activityClass);
  }

  public SplashActivityTestRule(Class<SplashActivity> activityClass, boolean initialTouchMode) {
    super(activityClass, initialTouchMode);
  }

  public SplashActivityTestRule(Class<SplashActivity> activityClass, boolean initialTouchMode, boolean launchActivity) {
    super(activityClass, initialTouchMode, launchActivity);
  }

  @Override
  protected void beforeActivityLaunched() {
    super.beforeActivityLaunched();
  }

  @Override
  protected void afterActivityLaunched() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      Log.d(TAG, Log.getStackTraceString(e));
    }
    super.afterActivityLaunched();
  }

  @Override
  protected void afterActivityFinished() {
    super.afterActivityFinished();
  }
}
